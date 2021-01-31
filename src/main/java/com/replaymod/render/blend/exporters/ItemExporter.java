package com.replaymod.render.blend.exporters;

import com.replaymod.mixin.ItemRendererAccessor;
import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Random;

public class ItemExporter implements Exporter {
    private final RenderState renderState;

    public ItemExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    public void onRender(Object renderItem, IBakedModel model, ItemStack stack) {
        DObject object = getObjectForItemStack(renderItem, model, stack);

        renderState.pushObject(object);
        renderState.pushModelView();
        renderState.applyLastModelViewTransformToObject();

        object.setVisible(renderState.getFrame());
        object.keyframeLocRotScale(renderState.getFrame());

        renderState.pop();
    }

    private DObject getObjectForItemStack(Object renderItem, IBakedModel model, ItemStack stack) {
        int frame = renderState.getFrame();
        DObject parent = renderState.peekObject();
        DObject object = null;
        for (DObject child : parent.getChildren()) {
            if (child.lastFrame < frame
                    && child instanceof ItemBasedDObject
                    && ((ItemBasedDObject) child).isBasedOn(renderItem, model, stack)) {
                object = child;
                break;
            }
        }
        if (object == null) {
            object = new ItemBasedDObject(renderItem, model, stack);
            object.id.name = stack.getDisplayName().getString();
            object.setParent(parent);
        }
        object.lastFrame = frame;
        return object;
    }

    @SuppressWarnings("unchecked")
    private static DMesh generateMeshForItemStack(Object renderItem, IBakedModel model, ItemStack stack) {
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        builder.setWellBehaved(true);
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        for (Direction face : Direction.values()) {
            renderQuads(renderItem, builder, model.getQuads(null, face, new Random()), stack);
        }
        renderQuads(renderItem, builder, model.getQuads(null, null, new Random()), stack);

        builder.finishDrawing();
        return mesh;
    }

    private static void renderQuads(Object renderItem, BlendMeshBuilder buffer, List<BakedQuad> quads, ItemStack stack) {
        for (BakedQuad quad : quads) {
            int color = stack != null && quad.hasTintIndex()
                    ? ((ItemRendererAccessor) renderItem).getItemColors().getColor(stack, quad.getTintIndex()) | 0xff000000
                    : 0xffffffff;
            // FIXME 1.15
        }

    }

    private static class ItemBasedDObject extends DObject {
        private final Object renderItem;
        private final IBakedModel model;
        private final ItemStack stack;
        private boolean valid;

        public ItemBasedDObject(Object renderItem, IBakedModel model, ItemStack stack) {
            super(generateMeshForItemStack(renderItem, model, stack));
            this.renderItem = renderItem;
            this.model = model;
            this.stack = stack;
        }

        public boolean isBasedOn(Object renderItem, IBakedModel model, ItemStack stack) {
            return this.renderItem == renderItem && this.model == model && this.stack == stack;
        }

        @Override
        public void setVisible(int frame) {
            valid = true;
            super.setVisible(frame);
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }
}
