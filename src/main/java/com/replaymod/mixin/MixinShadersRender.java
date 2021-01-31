package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.core.events.PreRenderHandCallback;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "shadersmod/client/ShadersRender", // Pre Optifine 1.12.2 E1
        "net/optifine/shaders/ShadersRender" // Post Optifine 1.12.2 E1
}, remap = false)
public abstract class MixinShadersRender {

    @Inject(method = {"renderHand0", "renderHand1"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void replayModCompat_disableRenderHand0(
            GameRenderer er,
            MatrixStack stack,
            ActiveRenderInfo camera,
            float partialTicks,
            CallbackInfo ci) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }

}
