package com.replaymod.gui.versions.callbacks;

import com.replaymod.gui.utils.Event;
import net.minecraft.client.util.math.MatrixStack;

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
