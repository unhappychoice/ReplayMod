package com.replaymod.replay.events;

import com.replaymod.gui.utils.Event;
import com.replaymod.replay.ReplayHandler;

import java.io.IOException;

public interface ReplayOpenedCallback {
    Event<ReplayOpenedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayOpenedCallback listener : listeners) {
                    listener.replayOpened(replayHandler);
                }
            });

    void replayOpened(ReplayHandler replayHandler) throws IOException;
}
