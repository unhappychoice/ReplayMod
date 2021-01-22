package com.replaymod.core.events;

import com.replaymod.gui.utils.Event;
import com.mojang.blaze3d.matrix.MatrixStack;

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
