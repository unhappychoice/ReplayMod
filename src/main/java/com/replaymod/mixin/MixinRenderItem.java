package com.replaymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.exporters.ItemExporter;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinRenderItem {
    @Inject(method = "renderModel", at = @At("HEAD"))
    private void onRenderModel(IBakedModel model, ItemStack stack, int int_1, int int_2, MatrixStack matrixStack_1, IVertexBuilder vertexConsumer_1, CallbackInfo ci) {
        BlendState blendState = BlendState.getState();
        if (blendState != null) {
            blendState.get(ItemExporter.class).onRender(this, model, stack);
        }
    }
}
