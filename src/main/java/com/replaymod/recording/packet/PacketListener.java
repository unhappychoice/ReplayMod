package com.replaymod.recording.packet;

import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.editor.gui.MarkerProcessor;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiSavingReplay;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import com.replaymod.gui.container.VanillaGuiScreen;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.network.play.server.SDisconnectPacket;
import net.minecraft.network.play.server.SCollectItemPacket;
import net.minecraft.network.play.server.SSpawnPlayerPacket;
import net.minecraft.entity.Entity;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.crash.CrashReport;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11400
import net.minecraft.network.login.server.SLoginSuccessPacket;
//#else
//$$ import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
//#endif

//#if FABRIC<1
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
//#endif

//#if MC>=10904
//#else
//$$ import java.util.List;
//#endif

//#if MC>=10800
//#if MC<10904
//$$ import net.minecraft.network.play.server.S46PacketSetCompressionLevel;
//#endif
import net.minecraft.network.login.server.SEnableCompressionPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.network.PacketDirection;
//#endif

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replaystudio.util.Utils.writeInt;

public class PacketListener extends ChannelInboundHandlerAdapter {

    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final ReplayMod core;
    private final Path outputPath;
    private final ReplayFile replayFile;

    private final ResourcePackRecorder resourcePackRecorder;

    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private final ReplayOutputStream packetOutputStream;

    private ReplayMetaData metaData;

    private ChannelHandlerContext context = null;

    private final long startTime;
    private long lastSentPacket;
    private long timePassedWhilePaused;
    private volatile boolean serverWasPaused;
    private ProtocolType connectionState = ProtocolType.LOGIN;
    private boolean loginPhase = true;

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
        this.packetOutputStream = replayFile.writePacketData();
        this.startTime = metaData.getDate();

        saveMetaData();
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
            if(packet instanceof SSpawnPlayerPacket) {
                //#if MC>=10800
                UUID uuid = ((SSpawnPlayerPacket) packet).getUniqueId();
                //#else
                //$$ UUID uuid = ((S0CPacketSpawnPlayer) packet).func_148948_e().getId();
                //#endif
                Set<String> uuids = new HashSet<>(Arrays.asList(metaData.getPlayers()));
                uuids.add(uuid.toString());
                metaData.setPlayers(uuids.toArray(new String[uuids.size()]));
                saveMetaData();
            }

            //#if MC>=10800
            if (packet instanceof SEnableCompressionPacket) {
                return; // Replay data is never compressed on the packet level
            }
            //#if MC<10904
            //$$ if (packet instanceof S46PacketSetCompressionLevel) {
            //$$     return; // Replay data is never compressed on the packet level
            //$$ }
            //#endif
            //#endif

            long now = System.currentTimeMillis();
            if (serverWasPaused) {
                timePassedWhilePaused = now - startTime - lastSentPacket;
                serverWasPaused = false;
            }
            int timestamp = (int) (now - startTime - timePassedWhilePaused);
            lastSentPacket = timestamp;
            PacketData packetData = getPacketData(timestamp, packet);
            saveService.submit(() -> {
                try {
                    if (ReplayMod.isMinimalMode()) {
                        // Minimal mode, ReplayStudio might not know our packet ids, so we cannot use it
                        com.github.steveice10.netty.buffer.ByteBuf packetIdBuf = PooledByteBufAllocator.DEFAULT.buffer();
                        com.github.steveice10.netty.buffer.ByteBuf packetBuf = packetData.getPacket().getBuf();
                        try {
                            new ByteBufNetOutput(packetIdBuf).writeVarInt(packetData.getPacket().getId());

                            int packetIdLen = packetIdBuf.readableBytes();
                            int packetBufLen = packetBuf.readableBytes();
                            writeInt(packetOutputStream, (int) packetData.getTime());
                            writeInt(packetOutputStream, packetIdLen + packetBufLen);
                            packetIdBuf.readBytes(packetOutputStream, packetIdLen);
                            packetBuf.getBytes(packetBuf.readerIndex(), packetOutputStream, packetBufLen);
                        } finally {
                            packetIdBuf.release();
                            packetBuf.release();
                        }
                    } else {
                        packetOutputStream.write(packetData);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            //#if MC>=11400
            if (packet instanceof SLoginSuccessPacket) {
                moveToPlayState();
            }
            //#endif
        } catch(Exception e) {
            logger.error("Writing packet:", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metaData.setDuration((int) lastSentPacket);
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
                packetOutputStream.close();
            } catch (IOException e) {
                logger.error("Failed to close packet output stream:", e);
            }

            List<Pair<Path, ReplayMetaData>> outputPaths;
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
                        return;
                    }

                    replayFile.save();
                    replayFile.close();

                    if (core.getSettingsRegistry().get(Setting.AUTO_POST_PROCESS) && !ReplayMod.isMinimalMode()) {
                        outputPaths = MarkerProcessor.apply(outputPath, guiSavingReplay.getProgressBar()::setProgress);
                    } else {
                        outputPaths = Collections.singletonList(new Pair<>(outputPath, metaData));
                    }
                } catch (Exception e) {
                    logger.error("Saving replay file:", e);
                    CrashReport crashReport = CrashReport.makeCrashReport(e, "Saving replay file");
                    core.runLater(() -> Utils.error(logger, VanillaGuiScreen.wrap(mc.currentScreen), crashReport, guiSavingReplay::close));
                    return;
                }
            }

            core.runLater(() -> guiSavingReplay.presentRenameDialog(outputPaths));
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(ctx == null) {
            if(context == null) {
                return;
            } else {
                ctx = context;
            }
        }
        this.context = ctx;

        if (msg instanceof IPacket) {
            try {
                IPacket packet = (IPacket) msg;

                //#if MC>=10904
                if(packet instanceof SCollectItemPacket) {
                    if(mc.player != null ||
                            ((SCollectItemPacket) packet).getCollectedItemEntityID() == mc.player.getEntityId()) {
                //#else
                //$$ if(packet instanceof S0DPacketCollectItem) {
                //$$     if(mc.thePlayer != null || ((S0DPacketCollectItem) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                //#endif
                        super.channelRead(ctx, msg);
                        return;
                    }
                }

                //#if MC>=10800
                if (packet instanceof SSendResourcePackPacket) {
                    save(resourcePackRecorder.handleResourcePack((SSendResourcePackPacket) packet));
                    return;
                }
                //#else
                //$$ if (packet instanceof S3FPacketCustomPayload) {
                //$$     S3FPacketCustomPayload p = (S3FPacketCustomPayload) packet;
                //$$     if ("MC|RPack".equals(p.func_149169_c())) {
                //$$         save(resourcePackRecorder.handleResourcePack(p));
                //$$         return;
                //$$     }
                //$$ }
                //#endif

                //#if MC<11400
                //$$ if (packet instanceof FMLProxyPacket) {
                //$$     // This packet requires special handling
                    //#if MC>=10800
                    //$$ ((FMLProxyPacket) packet).toS3FPackets().forEach(this::save);
                    //#else
                    //$$ save(((FMLProxyPacket) packet).toS3FPacket());
                    //#endif
                //$$     super.channelRead(ctx, msg);
                //$$     return;
                //$$ }
                //#endif

                //#if FABRIC<1
                if (packet instanceof SCustomPayloadLoginPacket) {
                    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                    packet.writePacketData(buffer);
                    packet = new SCustomPayloadLoginPacket();
                    packet.readPacketData(buffer);
                }
                //#endif

                //#if MC>=10800
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
                //#endif

                save(packet);

                if (packet instanceof SCustomPayloadPlayPacket) {
                    SCustomPayloadPlayPacket p = (SCustomPayloadPlayPacket) packet;
                    if (Restrictions.PLUGIN_CHANNEL.equals(p.getChannelName())) {
                        packet = new SDisconnectPacket(new StringTextComponent("Please update to view this replay."));
                        save(packet);
                    }
                }
            } catch(Exception e) {
                logger.error("Handling packet for recording:", e);
            }

        }

        super.channelRead(ctx, msg);
    }

    //#if MC>=10904
    private <T> void DataManager_set(EntityDataManager dataManager, EntityDataManager.DataEntry<T> entry) {
        dataManager.register(entry.getKey(), entry.getValue());
    }
    //#endif

    @SuppressWarnings("unchecked")
    private PacketData getPacketData(int timestamp, IPacket packet) throws Exception {
        //#if MC<11500
        //$$ if (packet instanceof MobSpawnS2CPacket) {
        //$$     MobSpawnS2CPacket p = (MobSpawnS2CPacket) packet;
        //$$     SPacketSpawnMobAccessor pa = (SPacketSpawnMobAccessor) p;
        //$$     if (pa.getDataManager() == null) {
        //$$         pa.setDataManager(new DataTracker(null));
        //$$         if (p.getTrackedValues() != null) {
        //$$             Set<Integer> seen = new HashSet<>();
                    //#if MC>=10904
                    //$$ for (DataTracker.Entry<?> entry : Lists.reverse(p.getTrackedValues())) {
                    //$$     if (!seen.add(entry.getData().getId())) continue;
                    //$$     DataManager_set(pa.getDataManager(), entry);
                    //$$ }
                    //#else
                    //$$ for(DataWatcher.WatchableObject wo : Lists.reverse((List<DataWatcher.WatchableObject>) p.func_149027_c())) {
                    //$$     if (!seen.add(wo.getDataValueId())) continue;
                    //$$     pa.getDataManager().addObject(wo.getDataValueId(), wo.getObject());
                    //$$ }
                    //#endif
        //$$         }
        //$$     }
        //$$ }
        //$$
        //$$ if (packet instanceof PlayerSpawnS2CPacket) {
        //$$     PlayerSpawnS2CPacket p = (PlayerSpawnS2CPacket) packet;
        //$$     SPacketSpawnPlayerAccessor pa = (SPacketSpawnPlayerAccessor) p;
        //$$     if (pa.getDataManager() == null) {
        //$$         pa.setDataManager(new DataTracker(null));
        //$$         if (p.getTrackedValues() != null) {
        //$$             Set<Integer> seen = new HashSet<>();
                    //#if MC>=10904
                    //$$ for (DataTracker.Entry<?> entry : Lists.reverse(p.getTrackedValues())) {
                    //$$     if (!seen.add(entry.getData().getId())) continue;
                    //$$     DataManager_set(pa.getDataManager(), entry);
                    //$$ }
                    //#else
                    //$$ for(DataWatcher.WatchableObject wo : Lists.reverse((List<DataWatcher.WatchableObject>) p.func_148944_c())) {
                    //$$     if (!seen.add(wo.getDataValueId())) continue;
                    //$$     pa.getDataManager().addObject(wo.getDataValueId(), wo.getObject());
                    //$$ }
                    //#endif
        //$$         }
        //$$     }
        //$$ }
        //#endif

        Integer packetId = getPacketId(packet);
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            packet.writePacketData(new PacketBuffer(byteBuf));
            return new PacketData(timestamp, new com.replaymod.replaystudio.protocol.Packet(
                    MCVer.getPacketTypeRegistry(loginPhase),
                    packetId,
                    com.github.steveice10.netty.buffer.Unpooled.wrappedBuffer(
                            byteBuf.array(),
                            byteBuf.arrayOffset(),
                            byteBuf.readableBytes()
                    )
            ));
        } finally {
            byteBuf.release();

            //#if MC>=10800
            if (packet instanceof SCustomPayloadPlayPacket) {
                ((SCustomPayloadPlayPacket) packet).getBufferData().release();
            }
            //#endif
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
        return lastSentPacket;
    }

    public void setServerWasPaused() {
        this.serverWasPaused = true;
    }

    private Integer getPacketId(IPacket packet) throws IOException {
        //#if MC>=10800
        Integer packetId = connectionState.getPacketId(PacketDirection.CLIENTBOUND, packet);
        //#else
        //$$ Integer packetId = (Integer) connectionState.func_150755_b().inverse().get(packet.getClass());
        //#endif

        if (packetId == null) {
            /*
               Retrying as the state is PLAY
               TODO: Investigate packet order and refactor to be more consistent
            */
            packetId = ProtocolType.PLAY.getPacketId(PacketDirection.CLIENTBOUND, packet);

            if (packetId == null) {
                throw new IOException("Unknown packet type:" + packet.getClass());
            }

            moveToPlayState();
        }

        return packetId;
    }

    private void moveToPlayState() {
        connectionState = ProtocolType.PLAY;
        loginPhase = false;
    }
}
