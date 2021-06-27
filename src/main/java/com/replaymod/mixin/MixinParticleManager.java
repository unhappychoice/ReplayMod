package com.replaymod.mixin;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ParticlesExporter;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleManager.class)
public abstract class MixinParticleManager {
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lcom/mojang/blaze3d/vertex/IVertexBuilder;Lnet/minecraft/client/renderer/ActiveRenderInfo;F)V"))
    private void buildOrientedGeometry(Particle particle, IVertexBuilder vertexConsumer, ActiveRenderInfo camera, float partialTicks) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            buildGeometry(particle, vertexConsumer, camera, partialTicks);
        } else {
            Quaternion rotation = camera.getRotation();
            Quaternion org = rotation.copy();
            try {
                Vector3d from = new Vector3d(0, 0, 1);
                Vector3d to = MCVer.getPosition(particle, partialTicks).subtract(camera.getProjectedView()).normalize();
                Vector3d axis = from.crossProduct(to);
                rotation.set((float) axis.x, (float) axis.y, (float) axis.z, (float) (1 + from.dotProduct(to)));
                rotation.normalize();

                buildGeometry(particle, vertexConsumer, camera, partialTicks);
            } finally {
                rotation.set(org.getW(), org.getX(), org.getY(), org.getZ());
            }
        }
    }

    private void buildGeometry(Particle particle, IVertexBuilder vertexConsumer, ActiveRenderInfo camera, float partialTicks) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ParticlesExporter.class).onRender(particle, partialTicks);
        }
        particle.renderParticle(vertexConsumer, camera, partialTicks);
    }
}
