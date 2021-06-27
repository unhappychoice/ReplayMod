package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class Mixin_Omnidirectional_SkipHand implements EntityRendererHandler.IEntityRenderer {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void replayModRender_renderSpectatorHand(
            MatrixStack matrixStack,
            ActiveRenderInfo camera,
            float partialTicks,
            CallbackInfo ci
    ) {
        EntityRendererHandler handler = replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            // No spectator hands during 360° view, we wouldn't even know where to put it
            ci.cancel();
        }
    }
}
