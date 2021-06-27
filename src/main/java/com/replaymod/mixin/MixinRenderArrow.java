package com.replaymod.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.TippedArrowRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TippedArrowRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer {
    protected MixinRenderArrow(EntityRendererManager renderManager) {
        super(renderManager);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldRender(Entity entity,
                                ClippingHelper camera,
                                double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return ReplayModReplay.instance.getReplayHandler() != null || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
