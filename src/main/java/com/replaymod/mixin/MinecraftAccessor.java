package com.replaymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

//#if MC>=11400
import java.util.concurrent.CompletableFuture;
//#endif

//#if MC<11400
//$$ import java.util.concurrent.FutureTask;
//#endif

//#if MC<11400
//$$ import net.minecraft.client.resources.IResourcePack;
//$$ import java.util.List;
//#endif

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    Timer getTimer();
    @Accessor
    void setTimer(Timer value);

    //#if MC>=11400
    @Accessor("futureRefreshResources")
    CompletableFuture<Void> getResourceReloadFuture();
    @Accessor("futureRefreshResources")
    void setResourceReloadFuture(CompletableFuture<Void> value);
    //#endif

    //#if MC>=11400
    @Accessor("queueChunkTracking")
    Queue<Runnable> getRenderTaskQueue();
    //#else
    //$$ @Accessor
    //$$ Queue<FutureTask<?>> getScheduledTasks();
    //#endif

    //#if FABRIC>=1
    //$$ @Accessor("crashReport")
    //$$ CrashReport getCrashReporter();
    //#else
    @Accessor
    CrashReport getCrashReporter();
    //#endif

    //#if MC<11400
    //$$ @Accessor
    //$$ List<IResourcePack> getDefaultResourcePacks();
    //#endif
}
