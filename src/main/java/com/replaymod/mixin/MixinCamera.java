package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinCamera {
    @Shadow
    @Final
    private Minecraft mc;

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getPitch()F"
            )
    )
    private void applyRoll(float float_1, long long_1, MatrixStack matrixStack, CallbackInfo ci) {
        Entity entity = this.mc.getRenderViewEntity() == null ? this.mc.player : this.mc.getRenderViewEntity();
        if (entity instanceof CameraEntity) {
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(((CameraEntity) entity).roll));
        }
    }
}
