package com.replaymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;


@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Timer getTimer();

    @Accessor
    void setTimer(Timer value);

    @Accessor("futureRefreshResources")
    CompletableFuture<Void> getResourceReloadFuture();

    @Accessor("futureRefreshResources")
    void setResourceReloadFuture(CompletableFuture<Void> value);

    @Accessor("queueChunkTracking")
    Queue<Runnable> getRenderTaskQueue();

    @Accessor
    CrashReport getCrashReporter();

}
