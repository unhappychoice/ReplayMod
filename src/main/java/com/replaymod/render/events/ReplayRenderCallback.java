package com.replaymod.render.events;

import com.replaymod.gui.utils.Event;
import com.replaymod.render.rendering.VideoRenderer;

public interface ReplayRenderCallback {
    interface Pre {
        Event<Pre> EVENT = Event.create((listeners) ->
                (renderer) -> {
                    for (Pre listener : listeners) {
                        listener.beforeRendering(renderer);
                    }
                });

        void beforeRendering(VideoRenderer renderer);
    }

    interface Post {
        Event<Post> EVENT = Event.create((listeners) ->
                (renderer) -> {
                    for (Post listener : listeners) {
                        listener.afterRendering(renderer);
                    }
                });

        void afterRendering(VideoRenderer renderer);
    }
}
