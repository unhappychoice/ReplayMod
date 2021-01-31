package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRendererManager.class)
public abstract class MixinRenderManager {
    @Shadow
    private Quaternion cameraOrientation;

    @Inject(method = "renderEntityStatic", at = @At("HEAD"))
    private void replayModRender_reorientForCubicRendering(Entity entity, double dx, double dy, double dz, float iDoNotKnow, float partialTicks,
                                                           MatrixStack matrixStack,
                                                           IRenderTypeBuffer vertexConsumerProvider,
                                                           int int_1,
                                                           CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            double yaw = -Math.atan2(dx, dz);
            this.cameraOrientation = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
            this.cameraOrientation.multiply(Vector3f.YP.rotationDegrees((float) -yaw));
            this.cameraOrientation.multiply(Vector3f.XP.rotationDegrees((float) pitch));
        }
    }
}
