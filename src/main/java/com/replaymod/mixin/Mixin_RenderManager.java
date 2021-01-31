package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRendererManager.class)
public abstract class Mixin_RenderManager {

    @Inject(
            method = "renderEntityStatic",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V"))
    public void preRender(Entity entity, double x, double y, double z, float yaw, float renderPartialTicks,
                          MatrixStack matrixStack,
                          IRenderTypeBuffer vertexConsumerProvider,
                          int int_1,
                          CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).preRender(entity, x, y, z, yaw, renderPartialTicks);
        }
    }

    @Inject(
            method = "renderEntityStatic",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
                    shift = At.Shift.AFTER))
    public void postRender(Entity entity, double x, double y, double z, float yaw, float renderPartialTicks,
                           MatrixStack matrixStack,
                           IRenderTypeBuffer vertexConsumerProvider,
                           int int_1,
                           CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).postRender(entity, x, y, z, yaw, renderPartialTicks);
        }
    }
}
