package com.replaymod.mixin;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.EntityExporter;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class Mixin_RenderGlobal {

    // FIXME wither skull ._. mojang pls

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void preEntityRender(CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).preEntitiesRender();
        }
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void postEntityRender(CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(EntityExporter.class).postEntitiesRender();
        }
    }

    // FIXME
}
