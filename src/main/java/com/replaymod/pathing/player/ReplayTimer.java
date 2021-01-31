package com.replaymod.pathing.player;

import com.replaymod.core.utils.WrappedTimer;
import com.replaymod.gui.utils.Event;
import net.minecraft.util.Timer;

/**
 * Wrapper around the current timer that prevents the timer from advancing by itself.
 */
public class ReplayTimer extends WrappedTimer {
    private final Timer state = new Timer(0, 0);

    public int ticksThisFrame;

    public ReplayTimer(Timer wrapped) {
        super(wrapped);
    }

    @Override
    // This should be handled by Remap but it isn't (was handled before a9724e3).
    public int
    getPartialTicks(
            long sysClock
    ) {
        copy(this, state); // Save our current state
        try {
            ticksThisFrame =
                    wrapped.getPartialTicks(
                            sysClock
                    ); // Update current state
        } finally {
            copy(state, this); // Restore our old state
            UpdatedCallback.EVENT.invoker().onUpdate();
        }
        return ticksThisFrame;
    }

    public Timer getWrapped() {
        return wrapped;
    }

    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = Event.create((listeners) ->
                () -> {
                    for (UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                }
        );

        void onUpdate();
    }
}
