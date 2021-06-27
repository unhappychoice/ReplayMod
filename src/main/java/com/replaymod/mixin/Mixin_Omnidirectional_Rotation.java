package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.getMinecraft;

@Mixin(value = net.minecraft.client.renderer.GameRenderer.class)
public abstract class Mixin_Omnidirectional_Rotation {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void replayModRender_setupCubicFrameRotation(
            float partialTicks,
            long frameStartNano,
            MatrixStack matrixStack,
            CallbackInfo ci
    ) {
        if (getHandler() != null && getHandler().data instanceof CubicOpenGlFrameCapturer.Data) {
            CubicOpenGlFrameCapturer.Data data = (CubicOpenGlFrameCapturer.Data) getHandler().data;
            float angle = 0;
            float x = 0;
            float y = 0;
            switch (data) {
                case FRONT:
                    angle = 0;
                    y = 1;
                    break;
                case RIGHT:
                    angle = 90;
                    y = 1;
                    break;
                case BACK:
                    angle = 180;
                    y = 1;
                    break;
                case LEFT:
                    angle = -90;
                    y = 1;
                    break;
                case TOP:
                    angle = -90;
                    x = 1;
                    break;
                case BOTTOM:
                    angle = 90;
                    x = 1;
                    break;
            }
            matrixStack.rotate(new Vector3f(x, y, 0).rotationDegrees(angle));
        }
    }
}
