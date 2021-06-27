package com.replaymod.mixin;

import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ClippingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(WorldRenderer.class)
public abstract class Mixin_ForceChunkLoading implements IForceChunkLoading {
    private ForceChunkLoadingHook replayModRender_hook;

    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender_hook = hook;
    }

    @Shadow
    private Set<ChunkRenderDispatcher.ChunkRender> chunksToUpdate;

    @Shadow
    private ChunkRenderDispatcher renderDispatcher;

    @Shadow
    private boolean displayListEntitiesDirty;

    @Shadow
    protected abstract void setupTerrain(ActiveRenderInfo camera_1, ClippingHelper frustum_1, boolean boolean_1, int int_1, boolean boolean_2);

    @Shadow
    private int frameId;

    private boolean passThrough;

    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)
    private void forceAllChunks(ActiveRenderInfo camera_1, ClippingHelper frustum_1, boolean boolean_1, int int_1, boolean boolean_2, CallbackInfo ci) throws IllegalAccessException {
        if (replayModRender_hook == null) {
            return;
        }
        if (passThrough) {
            return;
        }
        if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
            return;
        }
        ci.cancel();

        passThrough = true;
        try {
            do {
                // Determine which chunks shall be visible
                setupTerrain(camera_1, frustum_1, boolean_1, this.frameId++, boolean_2);

                // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
                // all of them anyway and this way we can take advantage of threading)
                for (ChunkRenderDispatcher.ChunkRender builtChunk : this.chunksToUpdate) {
                    // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
                    if (builtChunk.shouldStayLoaded()) {
                        builtChunk.rebuildChunkLater(this.renderDispatcher);
                    }
                    builtChunk.clearNeedsUpdate();
                }
                this.chunksToUpdate.clear();

                // Upload all chunks
                this.displayListEntitiesDirty |= ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.renderDispatcher).uploadEverythingBlocking();

                // Repeat until no more updates are needed
            } while (this.displayListEntitiesDirty);
        } finally {
            passThrough = false;
        }
    }
}
