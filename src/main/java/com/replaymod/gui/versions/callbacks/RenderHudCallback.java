package com.replaymod.gui.versions.callbacks;

import com.replaymod.gui.utils.Event;
import net.minecraft.client.util.math.MatrixStack;

public interface RenderHudCallback {
    Event<RenderHudCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for (RenderHudCallback listener : listeners) {
                    listener.renderHud(stack, partialTicks);
                }
            }
    );

    void renderHud(MatrixStack stack, float partialTicks);
}
