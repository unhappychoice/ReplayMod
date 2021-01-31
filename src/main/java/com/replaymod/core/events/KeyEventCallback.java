package com.replaymod.core.events;

import com.replaymod.gui.utils.Event;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = Event.create((listeners) ->
            (key, scanCode, action, modifiers) -> {
                for (KeyEventCallback listener : listeners) {
                    if (listener.onKeyEvent(key, scanCode, action, modifiers)) {
                        return true;
                    }
                }
                return false;
            }
    );

    int ACTION_RELEASE = org.lwjgl.glfw.GLFW.GLFW_RELEASE;
    int ACTION_PRESS = org.lwjgl.glfw.GLFW.GLFW_PRESS;

    boolean onKeyEvent(int key, int scanCode, int action, int modifiers);
}
