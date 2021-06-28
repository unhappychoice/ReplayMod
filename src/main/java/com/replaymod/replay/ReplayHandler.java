package com.replaymod.replay;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.gui.container.AbstractGuiScreen;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.container.GuiScreen;
import com.replaymod.gui.element.GuiLabel;
import com.replaymod.gui.element.advanced.GuiProgressBar;
import com.replaymod.gui.layout.HorizontalLayout;
import com.replaymod.gui.popup.AbstractGuiPopup;
import com.replaymod.mixin.MinecraftAccessor;
import com.replaymod.mixin.Mixin_EntityLivingBaseAccessor;
import com.replaymod.mixin.TimerAccessor;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replay.camera.SpectatorCameraController;
import com.replaymod.replay.events.ReplayClosedCallback;
import com.replaymod.replay.events.ReplayClosingCallback;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import com.replaymod.replay.sender.FullReplaySender;
import com.replaymod.replay.sender.QuickReplaySender;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.util.Location;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DownloadTerrainScreen;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraftforge.fml.network.NetworkHooks;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static com.replaymod.core.versions.MCVer.getMinecraft;
import static com.replaymod.replay.ReplayModReplay.LOGGER;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class ReplayHandler {

    private static Minecraft mc = getMinecraft();

    /**
     * The file currently being played.
     */
    private final ReplayFile replayFile;

    /**
     * Decodes and sends packets into channel.
     */
    private final FullReplaySender fullReplaySender;
    private final QuickReplaySender quickReplaySender;
    private boolean quickMode = false;

    /**
     * Currently active replay restrictions.
     */
    private Restrictions restrictions = new Restrictions();

    /**
     * Whether camera movements by user input and/or server packets should be suppressed.
     */
    private boolean suppressCameraMovements;

    private Set<Marker> markers;

    private final GuiReplayOverlay overlay;

    private EmbeddedChannel channel;

    private int replayDuration;

    /**
     * The position at which the camera should be located after the next jump.
     */
    private Location targetCameraPosition;

    private UUID spectating;

    public ReplayHandler(ReplayFile replayFile, boolean asyncMode) throws IOException {
        Preconditions.checkState(mc.isOnExecutionThread(), "Must be called from Minecraft thread.");
        this.replayFile = replayFile;

        replayDuration = replayFile.getMetaData().getDuration();

        markers = replayFile.getMarkers().or(Collections.emptySet());

        fullReplaySender = new FullReplaySender(this, replayFile, false);
        quickReplaySender = new QuickReplaySender(ReplayModReplay.instance, replayFile);

        setup();

        overlay = new GuiReplayOverlay(this);
        overlay.setVisible(true);

        ReplayOpenedCallback.EVENT.invoker().replayOpened(this);

        fullReplaySender.setAsyncMode(asyncMode);
    }

    public void restartedReplay() {
        Preconditions.checkState(mc.isOnExecutionThread(), "Must be called from Minecraft thread.");

        channel.close();

        mc.mouseHelper.ungrabMouse();

        // Force re-creation of camera entity by unloading the previous world
        mc.unloadWorld();

        restrictions = new Restrictions();

        setup();
    }

    public void endReplay() throws IOException {
        Preconditions.checkState(mc.isOnExecutionThread(), "Must be called from Minecraft thread.");

        ReplayClosingCallback.EVENT.invoker().replayClosing(this);

        fullReplaySender.terminateReplay();
        if (quickMode) {
            quickReplaySender.unregister();
        }

        replayFile.save();
        replayFile.close();

        channel.close().awaitUninterruptibly();

        if (mc.player instanceof CameraEntity) {
            mc.player.remove();
        }

        if (mc.world != null) {
            mc.unloadWorld();
        }

        TimerAccessor timer = (TimerAccessor) ((MinecraftAccessor) mc).getTimer();
        timer.setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK);
        overlay.setVisible(false);

        ReplayModReplay.instance.forcefullyStopReplay();

        mc.displayGuiScreen(null);

        ReplayClosedCallback.EVENT.invoker().replayClosed(this);
    }

    private void setup() {
        Preconditions.checkState(mc.isOnExecutionThread(), "Must be called from Minecraft thread.");

        mc.ingameGUI.getChatGUI().clearChatMessages(false);

        NetworkManager networkManager = new NetworkManager(PacketDirection.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };

        networkManager.setNetHandler(new ClientLoginNetHandler(
                networkManager,
                mc,
                null
                , it -> {
        }
        ));

        channel = new EmbeddedChannel();
        channel.pipeline().addLast("ReplayModReplay_quickReplaySender", quickReplaySender);
        channel.pipeline().addLast("ReplayModReplay_replaySender", fullReplaySender);
        channel.pipeline().addLast("packet_handler", networkManager);
        channel.pipeline().fireChannelActive();

        NetworkHooks.registerClientLoginChannel(networkManager);
    }

    public ReplayFile getReplayFile() {
        return replayFile;
    }

    public Restrictions getRestrictions() {
        return restrictions;
    }

    public ReplaySender getReplaySender() {
        return quickMode ? quickReplaySender : fullReplaySender;
    }

    public GuiReplayOverlay getOverlay() {
        return overlay;
    }

    public void ensureQuickModeInitialized(Runnable andThen) {
        if (Utils.ifMinimalModeDoPopup(overlay, () -> {
        })) return;
        ListenableFuture<Void> future = quickReplaySender.getInitializationPromise();
        if (future == null) {
            InitializingQuickModePopup popup = new InitializingQuickModePopup(overlay);
            future = quickReplaySender.initialize(progress -> popup.progressBar.setProgress(progress.floatValue()));
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    popup.close();
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    String message = "Failed to initialize quick mode. It will not be available.";
                    Utils.error(LOGGER, overlay, CrashReport.makeCrashReport(t, message), popup::close);
                }
            });
        }
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                andThen.run();
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                // Exception already printed in callback added above
            }
        });
    }

    private class InitializingQuickModePopup extends AbstractGuiPopup<InitializingQuickModePopup> {
        private final GuiProgressBar progressBar = new GuiProgressBar(popup).setSize(300, 20)
                .setI18nLabel("replaymod.gui.loadquickmode");

        public InitializingQuickModePopup(GuiContainer container) {
            super(container);
            open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected InitializingQuickModePopup getThis() {
            return this;
        }
    }

    public void setQuickMode(boolean quickMode) {
        if (ReplayMod.isMinimalMode()) {
            throw new UnsupportedOperationException("Quick Mode not supported in minimal mode.");
        }
        if (quickMode == this.quickMode) return;
        if (quickMode && fullReplaySender.isAsyncMode()) {
            // If this method is called via runLater, then it cannot switch to sync mode by itself as there might be
            // some rogue packets in the task queue after it. Instead the caller must switch to sync mode first and
            // use runLater until all packets have been processed (when using setAsyncModeAndWait, one runLater should
            // be sufficient).
            throw new IllegalStateException("Cannot switch to quick mode while in async mode.");
        }
        this.quickMode = quickMode;

        CameraEntity cam = getCameraEntity();
        if (cam != null) {
            targetCameraPosition = new Location(cam.getPosX(), cam.getPosY(), cam.getPosZ(), cam.rotationYaw, cam.rotationPitch);
        } else {
            targetCameraPosition = null;
        }

        if (quickMode) {
            quickReplaySender.register();
            quickReplaySender.restart();
            quickReplaySender.sendPacketsTill(fullReplaySender.currentTimeStamp());
        } else {
            quickReplaySender.unregister();
            fullReplaySender.sendPacketsTill(0);
            fullReplaySender.sendPacketsTill(quickReplaySender.currentTimeStamp());
        }

        moveCameraToTargetPosition();
    }

    public boolean isQuickMode() {
        return quickMode;
    }

    public int getReplayDuration() {
        return replayDuration;
    }

    /**
     * Return whether camera movement by user inputs and/or server packets should be suppressed.
     *
     * @return {@code true} if these kinds of movement should be suppressed
     */
    public boolean shouldSuppressCameraMovements() {
        return suppressCameraMovements;
    }

    /**
     * Set whether camera movement by user inputs and/or server packets should be suppressed.
     *
     * @param suppressCameraMovements {@code true} to suppress these kinds of movement, {@code false} to allow them
     */
    public void setSuppressCameraMovements(boolean suppressCameraMovements) {
        this.suppressCameraMovements = suppressCameraMovements;
    }

    /**
     * Spectate the specified entity.
     * When the entity is {@code null} or the camera entity, the camera becomes the view entity.
     *
     * @param e The entity to spectate
     */
    public void spectateEntity(Entity e) {
        CameraEntity cameraEntity = getCameraEntity();
        if (cameraEntity == null) {
            return; // Cannot spectate if we have no camera
        }
        if (e == null || e == cameraEntity) {
            spectating = null;
            e = cameraEntity;
        } else if (e instanceof PlayerEntity) {
            spectating = e.getUniqueID();
        }

        if (e == cameraEntity) {
            cameraEntity.setCameraController(ReplayModReplay.instance.createCameraController(cameraEntity));
        } else {
            cameraEntity.setCameraController(new SpectatorCameraController(cameraEntity));
        }

        if (mc.getRenderViewEntity() != e) {
            mc.setRenderViewEntity(e);
            cameraEntity.setCameraPosRot(e);
        }
    }

    /**
     * Set the camera as the view entity.
     * This is equivalent to {@code spectateEntity(null)}.
     */
    public void spectateCamera() {
        spectateEntity(null);
    }

    /**
     * Returns whether the current view entity is the camera entity.
     *
     * @return {@code true} if the camera is the view entity, {@code false} otherwise
     */
    public boolean isCameraView() {
        return mc.player instanceof CameraEntity && mc.player == mc.getRenderViewEntity();
    }

    /**
     * Returns the camera entity.
     *
     * @return The camera entity or {@code null} if it does not yet exist
     */
    public CameraEntity getCameraEntity() {
        return mc.player instanceof CameraEntity ? (CameraEntity) mc.player : null;
    }

    public UUID getSpectatedUUID() {
        return spectating;
    }

    public void moveCameraToTargetPosition() {
        CameraEntity cam = getCameraEntity();
        if (cam != null && targetCameraPosition != null) {
            cam.setCameraPosRot(targetCameraPosition);
        }
    }

    public void doJump(int targetTime, boolean retainCameraPosition) {
        if (getReplaySender() == quickReplaySender) {
            // Always round to full tick
            targetTime = targetTime + targetTime % 50;

            if (targetTime >= 50) {
                // Jump to time of previous tick first
                quickReplaySender.sendPacketsTill(targetTime - 50);
            }

            // Update all entity positions (especially prev/lastTick values)
            for (Entity entity : mc.world.getAllEntities()) {
                skipTeleportInterpolation(entity);
                entity.lastTickPosX = entity.prevPosX = entity.getPosX();
                entity.lastTickPosY = entity.prevPosY = entity.getPosY();
                entity.lastTickPosZ = entity.prevPosZ = entity.getPosZ();
                entity.prevRotationYaw = entity.rotationYaw;
                entity.prevRotationPitch = entity.rotationPitch;
            }

            // Run previous tick
            mc.runTick();

            // Jump to target tick
            quickReplaySender.sendPacketsTill(targetTime);

            // Immediately apply player teleport interpolation
            for (Entity entity : mc.world.getAllEntities()) {
                skipTeleportInterpolation(entity);
            }
            return;
        }
        FullReplaySender replaySender = fullReplaySender;

        if (replaySender.isHurrying()) {
            return; // When hurrying, no Timeline jumping etc. is possible
        }

        if (targetTime < replaySender.currentTimeStamp()) {
            mc.displayGuiScreen(null);
        }

        if (retainCameraPosition) {
            CameraEntity cam = getCameraEntity();
            if (cam != null) {
                targetCameraPosition = new Location(cam.getPosX(), cam.getPosY(), cam.getPosZ(),
                        cam.rotationYaw, cam.rotationPitch);
            } else {
                targetCameraPosition = null;
            }
        }

        long diff = targetTime - (replaySender.isHurrying() ? replaySender.getDesiredTimestamp() : replaySender.currentTimeStamp());
        if (diff != 0) {
            if (diff > 0 && diff < 5000) { // Small difference and no time travel
                replaySender.jumpToTime(targetTime);
            } else { // We either have to restart the replay or send a significant amount of packets
                // Render our please-wait-screen
                GuiScreen guiScreen = new GuiScreen();
                guiScreen.setBackground(AbstractGuiScreen.Background.DIRT);
                guiScreen.addElements(new HorizontalLayout.Data(0.5),
                        new GuiLabel().setI18nText("replaymod.gui.pleasewait"));

                // Make sure that the replaysender changes into sync mode
                replaySender.setSyncModeAndWait();

                // Perform the rendering using OpenGL
                pushMatrix();
                clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                        , true
                );
                enableTexture();
                mc.getFramebuffer().bindFramebuffer(true);
                MainWindow window = mc.getMainWindow();
                RenderSystem.clear(256, Minecraft.IS_RUNNING_ON_MAC);
                RenderSystem.matrixMode(GL11.GL_PROJECTION);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(0, window.getFramebufferWidth() / window.getGuiScaleFactor(), window.getFramebufferHeight() / window.getGuiScaleFactor(), 0, 1000, 3000);
                RenderSystem.matrixMode(GL11.GL_MODELVIEW);
                RenderSystem.loadIdentity();
                RenderSystem.translatef(0, 0, -2000);

                guiScreen.toMinecraft().init(mc, window.getScaledWidth(), window.getScaledHeight());
                guiScreen.toMinecraft().render(new MatrixStack(), 0, 0, 0);
                guiScreen.toMinecraft().onClose();

                mc.getFramebuffer().unbindFramebuffer();
                popMatrix();
                pushMatrix();
                mc.getFramebuffer().framebufferRender(mc.getMainWindow().getFramebufferWidth(), mc.getMainWindow().getFramebufferHeight());
                popMatrix();

                mc.getMainWindow().flipFrame();

                // Send the packets
                do {
                    replaySender.sendPacketsTill(targetTime);
                    targetTime += 500;
                } while (mc.player == null || mc.currentScreen instanceof DownloadTerrainScreen);
                replaySender.setAsyncMode(true);
                replaySender.setReplaySpeed(0);


                mc.getConnection().getNetworkManager()
                        .tick();
                for (Entity entity : mc.world.getAllEntities()) {
                    skipTeleportInterpolation(entity);
                    entity.lastTickPosX = entity.prevPosX = entity.getPosX();
                    entity.lastTickPosY = entity.prevPosY = entity.getPosY();
                    entity.lastTickPosZ = entity.prevPosZ = entity.getPosZ();
                    entity.prevRotationYaw = entity.rotationYaw;
                    entity.prevRotationPitch = entity.rotationPitch;
                }
                mc.runTick();

                //finally, updating the camera's position (which is not done by the sync jumping)
                moveCameraToTargetPosition();

                // No need to remove our please-wait-screen. It'll vanish with the next
                // render pass as it's never been a real GuiScreen in the first place.
            }
        }
    }

    private void skipTeleportInterpolation(Entity entity) {
        if (entity instanceof LivingEntity && !(entity instanceof CameraEntity)) {
            LivingEntity e = (LivingEntity) entity;
            Mixin_EntityLivingBaseAccessor ea = (Mixin_EntityLivingBaseAccessor) e;
            e.setPosition(ea.getInterpTargetX(), ea.getInterpTargetY(), ea.getInterpTargetZ());
            e.rotationYaw = (float) ea.getInterpTargetYaw();
            e.rotationPitch = (float) ea.getInterpTargetPitch();
        }
    }
}
