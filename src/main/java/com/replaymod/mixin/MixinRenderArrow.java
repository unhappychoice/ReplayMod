//#if MC>=10800
package com.replaymod.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.TippedArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

//#if MC>=11500
import net.minecraft.client.renderer.culling.ClippingHelper;
//#else
//$$ import net.minecraft.client.render.VisibleRegion;
//#endif

@Mixin(TippedArrowRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer {
    protected MixinRenderArrow(EntityRendererManager renderManager) {
        super(renderManager);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldRender(Entity entity,
                                //#if MC>=11500
                                ClippingHelper camera,
                                //#else
                                //$$ VisibleRegion camera,
                                //#endif
                                double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return ReplayModReplay.instance.getReplayHandler() != null || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
//#endif
