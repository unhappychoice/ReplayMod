package com.replaymod.core.events;

import com.replaymod.gui.utils.Event;

public interface PreRenderCallback {
    Event<PreRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreRenderCallback listener : listeners) {
                    listener.preRender();
                }
            }
    );

    void preRender();
}
