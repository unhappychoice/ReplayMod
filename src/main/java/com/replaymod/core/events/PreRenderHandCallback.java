package com.replaymod.core.events;

import com.replaymod.gui.utils.Event;

public interface PreRenderHandCallback {
    Event<PreRenderHandCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreRenderHandCallback listener : listeners) {
                    if (listener.preRenderHand()) {
                        return true;
                    }
                }
                return false;
            }
    );

    boolean preRenderHand();
}
