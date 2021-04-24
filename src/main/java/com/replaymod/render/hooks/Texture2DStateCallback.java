package com.replaymod.render.hooks;

import com.replaymod.gui.utils.Event;

public interface Texture2DStateCallback {
    Event<Texture2DStateCallback> EVENT = Event.create((listeners) ->
            (slot, enabled) -> {
                for (Texture2DStateCallback listener : listeners) {
                    listener.texture2DStateChanged(slot, enabled);
                }
            }
    );

    void texture2DStateChanged(int slot, boolean enabled);
}
