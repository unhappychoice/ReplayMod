package com.replaymod.gui.versions.callbacks;

import com.replaymod.gui.utils.Event;
import com.mojang.blaze3d.matrix.MatrixStack;

public interface PostRenderScreenCallback {
    Event<PostRenderScreenCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for (PostRenderScreenCallback listener : listeners) {
                    listener.postRenderScreen(stack, partialTicks);
                }
            }
    );

    void postRenderScreen(MatrixStack stack, float partialTicks);
}
