package com.replaymod.core.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.gui.utils.Event;

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            (MatrixStack matrixStack) -> {
                for (PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld(matrixStack);
                }
            }
    );

    void postRenderWorld(MatrixStack matrixStack);
}
