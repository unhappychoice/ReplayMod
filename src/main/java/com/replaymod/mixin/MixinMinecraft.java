package com.replaymod.mixin;

import com.replaymod.core.MinecraftMethodAccessor;
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.util.concurrent.RecursiveEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
        extends RecursiveEventLoop<Runnable>
        implements MinecraftMethodAccessor {

    public MixinMinecraft(String string_1) {
        super(string_1);
    }

    @Shadow
    protected abstract void processKeyBinds();

    public void replayModProcessKeyBinds() {
        processKeyBinds();
    }

    public void replayModExecuteTaskQueue() {
        drainTasks();
    }

    @Inject(method = "runGameLoop",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V"))
    private void preRender(boolean unused, CallbackInfo ci) {
        PreRenderCallback.EVENT.invoker().preRender();
    }

    @Inject(method = "runGameLoop",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V",
                    shift = At.Shift.AFTER))
    private void postRender(boolean unused, CallbackInfo ci) {
        PostRenderCallback.EVENT.invoker().postRender();
    }
}
