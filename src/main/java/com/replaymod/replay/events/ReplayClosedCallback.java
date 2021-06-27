package com.replaymod.replay.events;

import com.replaymod.gui.utils.Event;
import com.replaymod.replay.ReplayHandler;

public interface ReplayClosedCallback {
    Event<ReplayClosedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosedCallback listener : listeners) {
                    listener.replayClosed(replayHandler);
                }
            });

    void replayClosed(ReplayHandler replayHandler);
}
