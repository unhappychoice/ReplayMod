//#if MC>=10800
package com.replaymod.mixin;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11500
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.ChunkRender;
//#else
//$$ import net.minecraft.client.render.chunk.ChunkRenderer;
//#endif

@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {
    @Redirect(
            method = "updateChunkPositions",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=10904
                    //#if MC>=11500
                    target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$ChunkRender;setPosition(III)V"
                    //#else
                    //$$ target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;setOrigin(III)V"
                    //#endif
                    //#else
                    //$$ target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;setPosition(Lnet/minecraft/util/BlockPos;)V"
                    //#endif
            )
    )
    private void replayModReplay_updatePositionAndMarkForUpdate(
            //#if MC>=11500
            ChunkRender renderChunk,
            //#else
            //$$ ChunkRenderer renderChunk,
            //#endif
            //#if MC>=10904
            int x, int y, int z
            //#else
            //$$ BlockPos pos
            //#endif
    ) {
        //#if MC>=10904
        BlockPos pos = new BlockPos(x, y, z);
        //#endif
        if (!pos.equals(renderChunk.getPosition())) {
            //#if MC>=10904
            renderChunk.setPosition(x, y, z);
            renderChunk.setNeedsUpdate(false);
            //#else
            //$$ renderChunk.setPosition(pos);
            //$$ renderChunk.setNeedsUpdate(true);
            //#endif
        }
    }
}
//#endif
