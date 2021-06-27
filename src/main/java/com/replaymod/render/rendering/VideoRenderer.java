package com.replaymod.render.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.replaymod.core.MinecraftMethodAccessor;
import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.core.versions.MCVer;
import com.replaymod.mixin.MainWindowAccessor;
import com.replaymod.mixin.MinecraftAccessor;
import com.replaymod.mixin.TimerAccessor;
import com.replaymod.pathing.player.AbstractTimelinePlayer;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.render.*;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.events.ReplayRenderCallback;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.gui.GuiRenderingDone;
import com.replaymod.render.gui.GuiVideoRenderer;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.metadata.MetadataInjector;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Timer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.google.common.collect.Iterables.getLast;
import static com.mojang.blaze3d.platform.GlStateManager.*;
import static com.replaymod.core.versions.MCVer.resizeMainWindow;
import static com.replaymod.render.ReplayModRender.LOGGER;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer implements RenderInfo {
    private static final ResourceLocation SOUND_RENDER_SUCCESS = new ResourceLocation("replaymod", "render_success");
    private final Minecraft mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private final Pipeline renderingPipeline;
    private final FFmpegWriter ffmpegWriter;
    private final CameraPathExporter cameraPathExporter;

    private int fps;
    private boolean mouseWasGrabbed;
    private boolean debugInfoWasShown;
    private Map<SoundCategory, Float> originalSoundLevels;

    private TimelinePlayer timelinePlayer;
    private Future<Void> timelinePlayerFuture;
    private ForceChunkLoadingHook forceChunkLoadingHook;

    private int framesDone;
    private int totalFrames;

    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;
    private volatile Throwable failureCause;

    private Framebuffer guiFramebuffer;
    private int displayWidth, displayHeight;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
            BlendState.setState(new BlendState(settings.getOutputFile()));

            this.renderingPipeline = Pipelines.newBlendPipeline(this);
            this.ffmpegWriter = null;
        } else {
            FrameConsumer<BitmapFrame> frameConsumer;
            if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.EXR) {
                frameConsumer = new EXRWriter(settings.getOutputFile().toPath());
            } else if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.PNG) {
                frameConsumer = new PNGWriter(settings.getOutputFile().toPath());
            } else {
                frameConsumer = new FFmpegWriter(this);
            }
            ffmpegWriter = frameConsumer instanceof FFmpegWriter ? (FFmpegWriter) frameConsumer : null;
            FrameConsumer<BitmapFrame> previewingFrameConsumer = new FrameConsumer<BitmapFrame>() {
                @Override
                public void consume(Map<Channel, BitmapFrame> channels) {
                    BitmapFrame bgra = channels.get(Channel.BRGA);
                    if (bgra != null) {
                        gui.updatePreview(bgra.getByteBuffer(), bgra.getSize());
                    }
                    frameConsumer.consume(channels);
                }

                @Override
                public void close() throws IOException {
                    frameConsumer.close();
                }
            };
            this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this, previewingFrameConsumer);
        }

        if (settings.isCameraPathExport()) {
            this.cameraPathExporter = new CameraPathExporter(settings);
        } else {
            this.cameraPathExporter = null;
        }
    }

    /**
     * Render this video.
     *
     * @return {@code true} if rendering was successful, {@code false} if the user aborted rendering (or the window was closed)
     */
    public boolean renderVideo() throws Throwable {
        ReplayRenderCallback.Pre.EVENT.invoker().beforeRendering(this);

        setup();

        // Because this might take some time to prepare we'll render the GUI at least once to not confuse the user
        drawGui();

        Timer timer = ((MinecraftAccessor) mc).getTimer();

        // Play up to one second before starting to render
        // This is necessary in order to ensure that all entities have at least two position packets
        // and their first position in the recording is correct.
        // Note that it is impossible to also get the interpolation between their latest position
        // and the one in the recording correct as there's no reliable way to tell when the server ticks
        // or when we should be done with the interpolation of the entity
        Optional<Integer> optionalVideoStartTime = timeline.getValue(TimestampProperty.PROPERTY, 0);
        if (optionalVideoStartTime.isPresent()) {
            int videoStart = optionalVideoStartTime.get();

            if (videoStart > 1000) {
                int replayTime = videoStart - 1000;
                timer.renderPartialTicks = 0;
                ((TimerAccessor) timer).setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK);
                while (replayTime < videoStart) {
                    replayTime += 50;
                    replayHandler.getReplaySender().sendPacketsTill(replayTime);
                    tick();
                }
            }
        }


        renderingPipeline.run();

        if (((MinecraftAccessor) mc).getCrashReporter() != null) {
            throw new ReportedException(((MinecraftAccessor) mc).getCrashReporter());
        }

        if (settings.isInjectSphericalMetadata()) {
            MetadataInjector.injectMetadata(settings.getRenderMethod(), settings.getOutputFile(),
                    settings.getTargetVideoWidth(), settings.getTargetVideoHeight(),
                    settings.getSphericalFovX(), settings.getSphericalFovY());
        }

        finish();

        ReplayRenderCallback.Post.EVENT.invoker().afterRendering(this);

        if (failureCause != null) {
            throw failureCause;
        }

        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        // because the jGui lib uses Minecraft's displayWidth and displayHeight values, update these temporarily
        MainWindowAccessor acc = (MainWindowAccessor) (Object) mc.getMainWindow();
        int displayWidthBefore = acc.getFramebufferWidth();
        int displayHeightBefore = acc.getFramebufferHeight();
        acc.setFramebufferWidth(displayWidth);
        acc.setFramebufferHeight(displayHeight);

        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            while (drawGui() && paused) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Updating the timer will cause the timeline player to update the game state
        Timer timer = ((MinecraftAccessor) mc).getTimer();
        int elapsedTicks =
                timer.getPartialTicks(
                        MCVer.milliTime()
                );

        executeTaskQueue();


        while (elapsedTicks-- > 0) {
            tick();
        }

        // change Minecraft's display size back
        acc.setFramebufferWidth(displayWidthBefore);
        acc.setFramebufferHeight(displayHeightBefore);

        if (cameraPathExporter != null) {
            cameraPathExporter.recordFrame(timer.renderPartialTicks);
        }

        framesDone++;
        return timer.renderPartialTicks;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        timelinePlayer = new TimelinePlayer(replayHandler);
        timelinePlayerFuture = timelinePlayer.start(timeline);

        // FBOs are always used in 1.14+
        if (mc.gameSettings.showDebugInfo) {
            debugInfoWasShown = true;
            mc.gameSettings.showDebugInfo = false;
        }
        if (mc.mouseHelper.isMouseGrabbed()) {
            mouseWasGrabbed = true;
        }
        mc.mouseHelper.ungrabMouse();

        // Mute all sounds except GUI sounds (buttons, etc.)
        originalSoundLevels = new EnumMap<>(SoundCategory.class);
        for (SoundCategory category : SoundCategory.values()) {
            if (category != SoundCategory.MASTER) {
                originalSoundLevels.put(category, mc.gameSettings.getSoundLevel(category));
                mc.gameSettings.setSoundLevel(category, 0);
            }
        }

        fps = settings.getFramesPerSecond();

        long duration = 0;
        for (Path path : timeline.getPaths()) {
            if (!path.isActive()) continue;

            // Prepare path interpolations
            path.updateAll();
            // Find end time
            Collection<Keyframe> keyframes = path.getKeyframes();
            if (keyframes.size() > 0) {
                duration = Math.max(duration, getLast(keyframes).getTime());
            }
        }

        totalFrames = (int) (duration * fps / 1000);

        if (cameraPathExporter != null) {
            cameraPathExporter.setup(totalFrames);
        }

        updateDisplaySize();

        gui.toMinecraft().init(mc, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());

        forceChunkLoadingHook = new ForceChunkLoadingHook(mc.worldRenderer);

        // Set up our own framebuffer to render the GUI to
        guiFramebuffer = new Framebuffer(displayWidth, displayHeight, true
                , false
        );
    }

    private void finish() {
        if (!timelinePlayerFuture.isDone()) {
            timelinePlayerFuture.cancel(false);
        }
        // Tear down of the timeline player might only happen the next tick after it was cancelled
        timelinePlayer.onTick();

        // FBOs are always used in 1.14+
        mc.gameSettings.showDebugInfo = debugInfoWasShown;
        if (mouseWasGrabbed) {
            mc.mouseHelper.grabMouse();
        }
        for (Map.Entry<SoundCategory, Float> entry : originalSoundLevels.entrySet()) {
            mc.gameSettings.setSoundLevel(entry.getKey(), entry.getValue());
        }
        mc.displayGuiScreen(null);
        forceChunkLoadingHook.uninstall();

        if (!hasFailed() && cameraPathExporter != null) {
            try {
                cameraPathExporter.finish();
            } catch (IOException e) {
                setFailure(e);
            }
        }

        mc.getSoundHandler().play(SimpleSound.master(new SoundEvent(SOUND_RENDER_SUCCESS), 1));

        try {
            if (!hasFailed() && ffmpegWriter != null) {
                new GuiRenderingDone(ReplayModRender.instance, ffmpegWriter.getVideoFile(), totalFrames, settings).display();
            }
        } catch (FFmpegWriter.FFmpegStartupException e) {
            setFailure(e);
        }

        // Finally, resize the Minecraft framebuffer to the actual width/height of the window
        resizeMainWindow(mc, displayWidth, displayHeight);
    }

    private void executeTaskQueue() {
        while (true) {
            while (mc.loadingGui != null) {
                drawGui();
                ((MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
            }

            CompletableFuture<Void> resourceReloadFuture = ((MinecraftAccessor) mc).getResourceReloadFuture();
            if (resourceReloadFuture != null) {
                ((MinecraftAccessor) mc).setResourceReloadFuture(null);
                mc.reloadResources().thenRun(() -> resourceReloadFuture.complete(null));
                continue;
            }
            break;
        }
        ((MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();


        mc.currentScreen = gui.toMinecraft();
    }

    private void tick() {
        mc.runTick();
    }

    public boolean drawGui() {
        MainWindow window = mc.getMainWindow();
        do {
            if (GLFW.glfwWindowShouldClose(window.getHandle()) || ((MinecraftAccessor) mc).getCrashReporter() != null) {
                return false;
            }

            // Resize the GUI framebuffer if the display size changed
            if (displaySizeChanged()) {
                updateDisplaySize();
                guiFramebuffer.resize(displayWidth, displayHeight
                        , false
                );
            }

            pushMatrix();
            clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                    , false
            );
            enableTexture();
            guiFramebuffer.bindFramebuffer(true);

            RenderSystem.clear(256, Minecraft.IS_RUNNING_ON_MAC);
            RenderSystem.matrixMode(GL11.GL_PROJECTION);
            RenderSystem.loadIdentity();
            RenderSystem.ortho(0, window.getFramebufferWidth() / window.getGuiScaleFactor(), window.getFramebufferHeight() / window.getGuiScaleFactor(), 0, 1000, 3000);
            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.loadIdentity();
            RenderSystem.translatef(0, 0, -2000);

            gui.toMinecraft().init(mc, window.getScaledWidth(), window.getScaledHeight());

            // Events are polled on 1.13+ in mainWindow.update which is called later

            int mouseX = (int) mc.mouseHelper.getMouseX() * window.getScaledWidth() / displayWidth;
            int mouseY = (int) mc.mouseHelper.getMouseY() * window.getScaledHeight() / displayHeight;

            if (mc.loadingGui != null) {
                Screen orgScreen = mc.currentScreen;
                try {
                    mc.currentScreen = gui.toMinecraft();
                    mc.loadingGui.render(
                            new MatrixStack(),
                            mouseX, mouseY, 0);
                } finally {
                    mc.currentScreen = orgScreen;
                }
            } else {
                gui.toMinecraft().tick();
                gui.toMinecraft().render(
                        new MatrixStack(),
                        mouseX, mouseY, 0);
            }

            guiFramebuffer.unbindFramebuffer();
            popMatrix();
            pushMatrix();
            guiFramebuffer.framebufferRender(displayWidth, displayHeight);
            popMatrix();

            window.flipFrame();
            if (mc.mouseHelper.isMouseGrabbed()) {
                mc.mouseHelper.ungrabMouse();
            }

            return !hasFailed() && !cancelled;
        } while (true);
    }

    private boolean displaySizeChanged() {
        int realWidth = mc.getMainWindow().getWidth();
        int realHeight = mc.getMainWindow().getHeight();
        if (realWidth == 0 || realHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return false;
        }
        return displayWidth != realWidth || displayHeight != realHeight;
    }

    private void updateDisplaySize() {
        displayWidth = mc.getMainWindow().getWidth();
        displayHeight = mc.getMainWindow().getHeight();
    }

    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getVideoTime() {
        return framesDone * 1000 / fps;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void cancel() {
        if (ffmpegWriter != null) {
            ffmpegWriter.abort();
        }
        this.cancelled = true;
        renderingPipeline.cancel();
    }

    public boolean hasFailed() {
        return failureCause != null;
    }

    public synchronized void setFailure(Throwable cause) {
        if (this.failureCause != null) {
            LOGGER.error("Further failure during failed rendering: ", cause);
        } else {
            LOGGER.error("Failure during rendering: ", cause);
            this.failureCause = cause;
            cancel();
        }
    }

    private class TimelinePlayer extends AbstractTimelinePlayer {
        public TimelinePlayer(ReplayHandler replayHandler) {
            super(replayHandler);
        }

        @Override
        public long getTimePassed() {
            return getVideoTime();
        }
    }

    public static String[] checkCompat() {
        return null;
    }
}
