package com.replaymod.mixin;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.ChunkRender;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {
    @Redirect(
            method = "updateChunkPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$ChunkRender;setPosition(III)V"
            )
    )
    private void replayModReplay_updatePositionAndMarkForUpdate(
            ChunkRender renderChunk,
            int x, int y, int z
    ) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!pos.equals(renderChunk.getPosition())) {
            renderChunk.setPosition(x, y, z);
            renderChunk.setNeedsUpdate(false);
        }
    }
}
