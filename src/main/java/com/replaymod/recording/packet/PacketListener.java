package com.replaymod.recording.packet;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.editor.gui.MarkerProcessor;
import com.replaymod.gui.container.VanillaGuiScreen;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiSavingReplay;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
import net.minecraft.network.login.server.SEnableCompressionPacket;
import net.minecraft.network.play.server.*;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.replaymod.core.versions.MCVer.getMinecraft;

public class PacketListener extends ChannelInboundHandlerAdapter {

    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final ReplayMod core;
    private final Path outputPath;
    private final ReplayFile replayFile;
    private final ReplayMetaData metaData;

    private final PacketRecorder packetRecorder;
    private final ResourcePackRecorder resourcePackRecorder;

    private final ExecutorService saveService = Executors.newSingleThreadExecutor();

    private ChannelHandlerContext context = null;

    /**
     * Used to keep track of the last metadata save job submitted to the save service and
     * as such prevents unnecessary writes.
     */
    private final AtomicInteger lastSaveMetaDataId = new AtomicInteger();

    public PacketListener(ReplayMod core, Path outputPath, ReplayFile replayFile, ReplayMetaData metaData) throws IOException {
        this.core = core;
        this.outputPath = outputPath;
        this.replayFile = replayFile;
        this.metaData = metaData;
        this.resourcePackRecorder = new ResourcePackRecorder(replayFile);
        this.packetRecorder = new PacketRecorder(replayFile, metaData);

        saveMetaData();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metaData.setDuration((int) packetRecorder.getLastSentPacket());
        saveMetaData();

        core.runLater(() -> {
            ConnectionEventHandler connectionEventHandler = ReplayModRecording.instance.getConnectionEventHandler();
            if (connectionEventHandler.getPacketListener() == this) {
                connectionEventHandler.reset();
            }
        });

        GuiSavingReplay guiSavingReplay = new GuiSavingReplay(core);
        new Thread(() -> {
            core.runLater(guiSavingReplay::open);

            saveService.shutdown();
            try {
                saveService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Waiting for save service termination:", e);
            }
            try {
                packetRecorder.close();
            } catch (IOException e) {
                logger.error("Failed to close packet output stream:", e);
            }

            List<Pair<Path, ReplayMetaData>> outputPaths = saveReplayFile(guiSavingReplay);
            if (outputPaths != null) {
                core.runLater(() -> guiSavingReplay.presentRenameDialog(outputPaths));
            }
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ctx == null) {
            if (context == null) {
                return;
            } else {
                ctx = context;
            }
        }
        this.context = ctx;

        if (msg instanceof IPacket) {
            try {
                IPacket packet = (IPacket) msg;

                if (packet instanceof SCollectItemPacket) {
                    if (mc.player != null ||
                            ((SCollectItemPacket) packet).getCollectedItemEntityID() == mc.player.getEntityId()) {
                        super.channelRead(ctx, msg);
                        return;
                    }
                }

                if (packet instanceof SSendResourcePackPacket) {
                    save(resourcePackRecorder.handleResourcePack((SSendResourcePackPacket) packet));
                    return;
                }

                if (packet instanceof SCustomPayloadLoginPacket) {
                    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                    packet.writePacketData(buffer);
                    packet = new SCustomPayloadLoginPacket();
                    packet.readPacketData(buffer);
                }

                if (packet instanceof SCustomPayloadPlayPacket) {
                    // Forge may read from this ByteBuf and/or release it during handling
                    // We want to save the full thing however, so we create a copy and save that one instead of the
                    // original one
                    // Note: This isn't an issue with vanilla MC because our saving code runs on the main thread
                    //       shortly before the vanilla handling code does. Forge however does some stuff on the netty
                    //       threads which leads to this race condition
                    packet = new SCustomPayloadPlayPacket(
                            ((SCustomPayloadPlayPacket) packet).getChannelName(),
                            new PacketBuffer(((SCustomPayloadPlayPacket) packet).getBufferData().slice().retain())
                    );
                }

                save(packet);

                if (packet instanceof SCustomPayloadPlayPacket) {
                    SCustomPayloadPlayPacket p = (SCustomPayloadPlayPacket) packet;
                    if (Restrictions.PLUGIN_CHANNEL.equals(p.getChannelName())) {
                        packet = new SDisconnectPacket(new StringTextComponent("Please update to view this replay."));
                        save(packet);
                    }
                }
            } catch (Exception e) {
                logger.error("Handling packet for recording:", e);
            }
        }

        super.channelRead(ctx, msg);
    }

    public void save(IPacket packet) {
        // If we're not on the main thread (i.e. we're on the netty thread), then we need to schedule the saving
        // to happen on the main thread so we can guarantee correct ordering of inbound and inject packets.
        // Otherwise, injected packets may end up further down the packet stream than they were supposed to and other
        // inbound packets which may rely on the injected packet would behave incorrectly when played back.
        if (!mc.isOnExecutionThread()) {
            mc.enqueue(() -> save(packet));
            return;
        }

        try {
            if (packet instanceof SSpawnPlayerPacket) {
                UUID uuid = ((SSpawnPlayerPacket) packet).getUniqueId();
                Set<String> uuids = new HashSet<>(Arrays.asList(metaData.getPlayers()));
                uuids.add(uuid.toString());
                metaData.setPlayers(uuids.toArray(new String[uuids.size()]));
                saveMetaData();
            }

            if (packet instanceof SEnableCompressionPacket) {
                return; // Replay data is never compressed on the packet level
            }

            packetRecorder.saveIntoReplayFile(packet);
        } catch (Exception e) {
            logger.error("Writing packet:", e);
        }
    }

    public void addMarker(String name) {
        addMarker(name, (int) getCurrentDuration());
    }

    public void addMarker(String name, int timestamp) {
        Entity view = mc.getRenderViewEntity();

        Marker marker = new Marker();
        marker.setName(name);
        marker.setTime(timestamp);
        if (view != null) {
            marker.setX(view.getPosX());
            marker.setY(view.getPosY());
            marker.setZ(view.getPosZ());
            marker.setYaw(view.rotationYaw);
            marker.setPitch(view.rotationPitch);
        }
        // Roll is always 0
        saveService.submit(() -> {
            synchronized (replayFile) {
                try {
                    Set<Marker> markers = replayFile.getMarkers().or(HashSet::new);
                    markers.add(marker);
                    replayFile.writeMarkers(markers);
                } catch (IOException e) {
                    logger.error("Writing markers:", e);
                }
            }
        });
    }

    public long getCurrentDuration() {
        return packetRecorder.getLastSentPacket();
    }

    public void setServerWasPaused() {
        packetRecorder.setServerWasPaused(true);
    }

    private void saveMetaData() {
        int id = lastSaveMetaDataId.incrementAndGet();
        saveService.submit(() -> {
            if (lastSaveMetaDataId.get() != id) {
                return; // Another job has been scheduled, it will do the hard work.
            }
            try {
                synchronized (replayFile) {
                    if (ReplayMod.isMinimalMode()) {
                        metaData.setFileFormat("MCPR");
                        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
                        metaData.setProtocolVersion(MCVer.getProtocolVersion());
                        metaData.setGenerator("ReplayMod in Minimal Mode");

                        try (OutputStream out = replayFile.write("metaData.json")) {
                            String json = (new Gson()).toJson(metaData);
                            out.write(json.getBytes());
                        }
                    } else {
                        replayFile.writeMetaData(MCVer.getPacketTypeRegistry(true), metaData);
                    }
                }
            } catch (IOException e) {
                logger.error("Writing metadata:", e);
            }
        });
    }

    private List<Pair<Path, ReplayMetaData>> saveReplayFile(GuiSavingReplay guiSavingReplay) {
        synchronized (replayFile) {
            try {
                if (!MarkerProcessor.producesAnyOutput(replayFile)) {
                    // Immediately close the saving popup, the user doesn't care about it
                    core.runLater(guiSavingReplay::close);

                    // If we crash right here, on the next start we'll prompt the user for recovery
                    // but we don't really want that, so drop a marker file to skip recovery for this replay.
                    Files.createFile(outputPath.resolveSibling(outputPath.getFileName() + ".no_recover"));

                    // We still have the replay, so we just save it (at least for a few weeks) in case they change their mind
                    String replayName = FilenameUtils.getBaseName(outputPath.getFileName().toString());
                    Path rawFolder = ReplayMod.instance.getRawReplayFolder();
                    Path rawPath = rawFolder.resolve(outputPath.getFileName());
                    for (int i = 1; Files.exists(rawPath); i++) {
                        rawPath = rawPath.resolveSibling(replayName + "." + i + ".mcpr");
                    }
                    Files.createDirectories(rawPath.getParent());
                    replayFile.saveTo(rawPath.toFile());
                    replayFile.close();
                    return null;
                }

                replayFile.save();
                replayFile.close();

                if (core.getSettingsRegistry().get(Setting.AUTO_POST_PROCESS) && !ReplayMod.isMinimalMode()) {
                    return MarkerProcessor.apply(outputPath, guiSavingReplay.getProgressBar()::setProgress);
                } else {
                    return Collections.singletonList(new Pair<>(outputPath, metaData));
                }
            } catch (Exception e) {
                logger.error("Saving replay file:", e);
                CrashReport crashReport = CrashReport.makeCrashReport(e, "Saving replay file");
                core.runLater(() -> Utils.error(logger, VanillaGuiScreen.wrap(mc.currentScreen), crashReport, guiSavingReplay::close));
                return null;
            }
        }
    }
}
