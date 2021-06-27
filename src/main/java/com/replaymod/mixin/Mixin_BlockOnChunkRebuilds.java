package com.replaymod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkRenderDispatcher.class)
public abstract class Mixin_BlockOnChunkRebuilds implements ForceChunkLoadingHook.IBlockOnChunkRebuilds {
    @Shadow
    @Final
    private Queue<RegionRenderCacheBuilder> freeBuilders;

    @Shadow
    public abstract boolean runChunkUploads();

    @Shadow
    @Final
    private DelegatedTaskExecutor<Runnable> delegatedTaskExecutor;

    @Shadow
    protected abstract void runTask();

    @Shadow
    @Final
    private Queue<Runnable> uploadTasks;
    private final Lock waitingForWorkLock = new ReentrantLock();
    private final Condition newWork = waitingForWorkLock.newCondition();
    private volatile boolean allDone;

    private int totalBufferCount;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rememberTotalThreads(CallbackInfo ci) {
        this.totalBufferCount = this.freeBuilders.size();
    }

    @Inject(method = "runTask", at = @At("RETURN"))
    private void notifyMainThreadIfEverythingIsDone(CallbackInfo ci) {
        if (this.freeBuilders.size() == this.totalBufferCount) {
            // Looks like we're done, better notify the main thread in case the previous task didn't generate an upload
            this.waitingForWorkLock.lock();
            try {
                this.allDone = true;
                this.newWork.signalAll();
            } finally {
                this.waitingForWorkLock.unlock();
            }
        } else {
            this.allDone = false;
        }
    }

    @Inject(method = "uploadChunkLayer", at = @At("RETURN"))
    private void notifyMainThreadOfNewUpload(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        this.waitingForWorkLock.lock();
        try {
            this.newWork.signal();
        } finally {
            this.waitingForWorkLock.unlock();
        }
    }

    private boolean waitForMainThreadWork() {
        boolean allDone = this.delegatedTaskExecutor.<Boolean>func_213141_a(reply -> () -> {
            runTask();
            reply.enqueue(this.freeBuilders.size() == this.totalBufferCount);
        }).join();

        if (allDone) {
            return true;
        } else {
            this.waitingForWorkLock.lock();
            try {
                while (true) {
                    // Now, what is this call doing here you might be wondering. Well, from a quick look over everything
                    // it does not look like it would be required but have a **very** close look at [scheduleUpload]:
                    // It is not actually guaranteed to run the upload on the main thread, it just looks like it (and
                    // was probably supposed to do that) but in actuality, because it adds the first, empty future to
                    // the upload queue before attaching the thenCompose callback, that future can actually be completed
                    // by the main thread before thenCompose is called, resulting in thenCompose immediately calling
                    // its callback on the same (non-main) thread.
                    // Looking at how the upload behaves executed on a non-main thread, it eventually just enqueues
                    // itself in the RenderSystem's queue and returns a future for that.
                    // So, even though our [notifyMainThreadOfNewUpload] gets executed after all that, we would simply
                    // dead-lock ourselves here (since the upload queue is already empty), if we did never do this call
                    // to run the upload scheduled via this particular path of code execution.
                    RenderSystem.replayQueue();

                    if (this.allDone) {
                        return true;
                    } else if (!this.uploadTasks.isEmpty()) {
                        return false;
                    } else {
                        this.newWork.awaitUninterruptibly();
                    }
                }
            } finally {
                this.waitingForWorkLock.unlock();
            }
        }
    }

    @Override
    public boolean uploadEverythingBlocking() {
        boolean anything = false;

        boolean allChunksBuilt;
        do {
            allChunksBuilt = waitForMainThreadWork();
            while (runChunkUploads()) {
                anything = true;
            }
        } while (!allChunksBuilt);

        return anything;
    }
}
