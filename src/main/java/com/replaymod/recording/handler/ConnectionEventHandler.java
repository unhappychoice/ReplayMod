package com.replaymod.recording.handler;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.ModCompat;
import com.replaymod.core.utils.Utils;
import com.replaymod.editor.gui.MarkerProcessor;
import com.replaymod.mixin.NetworkManagerAccessor;
import com.replaymod.recording.ServerInfoExt;
import com.replaymod.recording.Setting;
import com.replaymod.recording.gui.GuiRecordingControls;
import com.replaymod.recording.gui.GuiRecordingOverlay;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.NetworkManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.replaymod.core.versions.MCVer.getMinecraft;

/**
 * Handles connection events and initiates recording if enabled.
 */
public class ConnectionEventHandler {

    private static final String packetHandlerKey = "packet_handler";
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final Minecraft mc = getMinecraft();

    private final Logger logger;
    private final ReplayMod core;

    private RecordingEventHandler recordingEventHandler;
    private PacketListener packetListener;
    private GuiRecordingOverlay guiOverlay;
    private GuiRecordingControls guiControls;

    public ConnectionEventHandler(Logger logger, ReplayMod core) {
        this.logger = logger;
        this.core = core;
    }

    public void onConnectedToServerEvent(NetworkManager networkManager) {
        try {
            boolean local = networkManager.isLocalChannel();
            if (local) {
                if (mc.getIntegratedServer().getWorld(World.OVERWORLD).isDebug()) {
                    logger.info("Debug World recording is not supported.");
                    return;
                }
                if (!core.getSettingsRegistry().get(Setting.RECORD_SINGLEPLAYER)) {
                    logger.info("Singleplayer Recording is disabled");
                    return;
                }
            } else {
                if (!core.getSettingsRegistry().get(Setting.RECORD_SERVER)) {
                    logger.info("Multiplayer Recording is disabled");
                    return;
                }
            }

            String worldName;
            String serverName = null;
            boolean autoStart = core.getSettingsRegistry().get(Setting.AUTO_START_RECORDING);
            if (local) {
                worldName = mc.getIntegratedServer().getServerConfiguration().getWorldName();
                serverName = worldName;
            } else if (mc.getCurrentServerData() != null) {
                ServerData serverInfo = mc.getCurrentServerData();
                worldName = serverInfo.serverIP;
                if (!I18n.format("selectServer.defaultName").equals(serverInfo.serverName)) {
                    serverName = serverInfo.serverName;
                }

                Boolean autoStartServer = ServerInfoExt.from(serverInfo).getAutoRecording();
                if (autoStartServer != null) {
                    autoStart = autoStartServer;
                }
            } else if (mc.isConnectedToRealms()) {
                // we can't access the server name without tapping too deep in the Realms Library
                worldName = "A Realms Server";
            } else {
                logger.info("Recording not started as the world is neither local nor remote (probably a replay).");
                return;
            }

            if (ReplayMod.isMinimalMode()) {
                // Recording controls are not supported in minimal mode, so always auto-start
                autoStart = true;
            }

            String name = sdf.format(Calendar.getInstance().getTime());
            Path outputPath = core.getRecordingFolder().resolve(Utils.replayNameToFileName(name));
            ReplayFile replayFile = core.openReplay(outputPath);

            replayFile.writeModInfo(ModCompat.getInstalledNetworkMods());

            ReplayMetaData metaData = new ReplayMetaData();
            metaData.setSingleplayer(local);
            metaData.setServerName(worldName);
            metaData.setCustomServerName(serverName);
            metaData.setGenerator("ReplayMod v" + ReplayMod.instance.getVersion());
            metaData.setDate(System.currentTimeMillis());
            metaData.setMcVersion(ReplayMod.instance.getMinecraftVersion());
            packetListener = new PacketListener(core, outputPath, replayFile, metaData);
            Channel channel = ((NetworkManagerAccessor) networkManager).getChannel();
            channel.pipeline().addBefore(packetHandlerKey, "replay_recorder", packetListener);

            recordingEventHandler = new RecordingEventHandler(packetListener);
            recordingEventHandler.register();

            guiControls = new GuiRecordingControls(core, packetListener, autoStart);
            guiControls.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry(), guiControls);
            guiOverlay.register();

            if (autoStart) {
                core.printInfoToChat("replaymod.chat.recordingstarted");
            } else {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, 0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            core.printWarningToChat("replaymod.chat.recordingfailed");
        }
    }

    public void reset() {
        if (packetListener != null) {
            guiControls.unregister();
            guiControls = null;
            guiOverlay.unregister();
            guiOverlay = null;
            recordingEventHandler.unregister();
            recordingEventHandler = null;
            packetListener = null;
        }
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }
}
