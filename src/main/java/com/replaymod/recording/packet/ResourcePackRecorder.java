package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.replaymod.gui.utils.Consumer;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.resources.DownloadingPackFinder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CResourcePackStatusPacket;
import net.minecraft.network.play.client.CResourcePackStatusPacket.Action;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.replaymod.core.versions.MCVer.*;

/**
 * Records resource packs and handles incoming resource pack packets during recording.
 */
public class ResourcePackRecorder {
    private static final Logger logger = LogManager.getLogger();
    private static final Minecraft mc = getMinecraft();

    private final ReplayFile replayFile;

    private int nextRequestId;

    public ResourcePackRecorder(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    public void recordResourcePack(File file, int requestId) {
        try {
            // Read in resource pack file
            byte[] bytes = Files.toByteArray(file);
            // Check whether it is already known
            String hash = Hashing.sha1().hashBytes(bytes).toString();
            boolean doWrite = false; // Whether we are the first and have to write it
            synchronized (replayFile) { // Need to read, modify and write the resource pack index atomically
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index == null) {
                    index = new HashMap<>();
                }
                if (!index.containsValue(hash)) {
                    // Hash is unknown, we have to write the resource pack ourselves
                    doWrite = true;
                }
                // Save this request
                index.put(requestId, hash);
                replayFile.writeResourcePackIndex(index);
            }
            if (doWrite) {
                try (OutputStream out = replayFile.writeResourcePack(hash)) {
                    out.write(bytes);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    public CResourcePackStatusPacket makeStatusPacket(String hash, Action action) {
        return new CResourcePackStatusPacket(action);
    }


    public synchronized SSendResourcePackPacket handleResourcePack(SSendResourcePackPacket packet) {
        final int requestId = nextRequestId++;
        final ClientPlayNetHandler netHandler = mc.getConnection();
        final NetworkManager netManager = netHandler.getNetworkManager();
        final String url = packet.getURL();
        final String hash = packet.getHash();

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.gameDir, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.sendPacket(makeStatusPacket(hash, Action.ACCEPTED));
                addCallback(setServerResourcePack(levelDir), result -> {
                    recordResourcePack(levelDir, requestId);
                    netManager.sendPacket(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED));
                }, throwable -> {
                    netManager.sendPacket(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
                });
            } else {
                netManager.sendPacket(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
            }
        } else {
            final ServerData serverData = mc.getCurrentServerData();
            if (serverData != null && serverData.getResourceMode() == ServerData.ServerResourceMode.ENABLED) {
                netManager.sendPacket(makeStatusPacket(hash, Action.ACCEPTED));
                downloadResourcePackFuture(requestId, url, hash);
            } else if (serverData != null && serverData.getResourceMode() != ServerData.ServerResourceMode.PROMPT) {
                netManager.sendPacket(makeStatusPacket(hash, Action.DECLINED));
            } else {
                // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
                mc.execute(() -> mc.displayGuiScreen(new ConfirmScreen(result -> {
                    if (serverData != null) {
                        serverData.setResourceMode(result ? ServerData.ServerResourceMode.ENABLED : ServerData.ServerResourceMode.DISABLED);
                    }
                    if (result) {
                        netManager.sendPacket(makeStatusPacket(hash, Action.ACCEPTED));
                        downloadResourcePackFuture(requestId, url, hash);
                    } else {
                        netManager.sendPacket(makeStatusPacket(hash, Action.DECLINED));
                    }

                    ServerList.saveSingleServer(serverData);
                    mc.displayGuiScreen(null);
                }
                        , new TranslationTextComponent("multiplayer.texturePrompt.line1"), new TranslationTextComponent("multiplayer.texturePrompt.line2"))));
            }
        }

        return new SSendResourcePackPacket("replay://" + requestId, "");
    }

    private void downloadResourcePackFuture(int requestId, String url, final String hash) {
        addCallback(downloadResourcePack(requestId, url, hash),
                result -> mc.getConnection().sendPacket(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED)),
                throwable -> mc.getConnection().sendPacket(makeStatusPacket(hash, Action.FAILED_DOWNLOAD)));
    }

    private CompletableFuture<?>
    downloadResourcePack(final int requestId, String url, String hash) {
        DownloadingPackFinder packFinder = mc.getPackFinder();
        ((IDownloadingPackFinder) packFinder).setRequestCallback(file -> recordResourcePack(file, requestId));
        return packFinder.downloadResourcePack(url, hash);
    }

    public interface IDownloadingPackFinder {
        void setRequestCallback(Consumer<File> callback);
    }

}
