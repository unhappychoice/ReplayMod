package com.replaymod.gui.versions.callbacks;

import com.replaymod.gui.utils.Event;

public interface PreTickCallback {
    Event<PreTickCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreTickCallback listener : listeners) {
                    listener.preTick();
                }
            }
    );

    void preTick();
}
