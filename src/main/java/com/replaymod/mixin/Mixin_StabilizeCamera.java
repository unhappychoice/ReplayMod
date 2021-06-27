package com.replaymod.mixin;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.replaymod.core.versions.MCVer.getMinecraft;

@Mixin(value = ActiveRenderInfo.class)
public abstract class Mixin_StabilizeCamera {
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }

    private float orgYaw;
    private float orgPitch;
    private float orgPrevYaw;
    private float orgPrevPitch;
    private float orgRoll;

    // Only relevant on 1.13+ (previously MC always used the non-head yaw) and only for LivingEntity view entities.
    private float orgHeadYaw;
    private float orgPrevHeadYaw;

    @Inject(method = "update", at = @At("HEAD"))
    private void replayModRender_beforeSetupCameraTransform(
            IBlockReader blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            orgYaw = entity.rotationYaw;
            orgPitch = entity.rotationPitch;
            orgPrevYaw = entity.prevRotationYaw;
            orgPrevPitch = entity.prevRotationPitch;
            orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
            if (entity instanceof LivingEntity) {
                orgHeadYaw = ((LivingEntity) entity).rotationYawHead;
                orgPrevHeadYaw = ((LivingEntity) entity).prevRotationYawHead;
            }
        }
        if (getHandler() != null) {
            RenderSettings settings = getHandler().getSettings();
            if (settings.isStabilizeYaw()) {
                entity.prevRotationYaw = entity.rotationYaw = 0;
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).prevRotationYawHead = ((LivingEntity) entity).rotationYawHead = 0;
                }
            }
            if (settings.isStabilizePitch()) {
                entity.prevRotationPitch = entity.rotationPitch = 0;
            }
            if (settings.isStabilizeRoll() && entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = 0;
            }
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void replayModRender_afterSetupCameraTransform(
            IBlockReader blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (getHandler() != null) {
            entity.rotationYaw = orgYaw;
            entity.rotationPitch = orgPitch;
            entity.prevRotationYaw = orgPrevYaw;
            entity.prevRotationPitch = orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = orgRoll;
            }
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).rotationYawHead = orgHeadYaw;
                ((LivingEntity) entity).prevRotationYawHead = orgPrevHeadYaw;
            }
        }
    }
}
