package com.replaymod.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.culling.ClippingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClippingHelper.class)
public abstract class Mixin_Omnidirectional_DisableFrustumCulling {
    @Inject(method = "isBoxInFrustumRaw", at = @At("HEAD"), cancellable = true)
    public void intersects(CallbackInfoReturnable<Boolean> ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            // Note the following used to be true but for simplicity non-ODS omnidirectional is the same now too.
            // Normally the camera is always facing the direction of the omnidirectional image face that is currently
            // getting rendered. With ODS however, the camera is always facing forwards and the turning happens in the
            // vertex shader (non-trivial due to stereo). As such, all chunks need to be rendered all the time for ODS.
            ci.setReturnValue(true);
        }
    }
}
