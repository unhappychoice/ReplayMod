package com.replaymod.core.utils;

import com.replaymod.mixin.TimerAccessor;
import net.minecraft.util.Timer;

public class WrappedTimer extends Timer {
    public static final float DEFAULT_MS_PER_TICK = 1000 / 20;

    protected final Timer wrapped;

    public WrappedTimer(Timer wrapped) {
        super(0, 0);
        this.wrapped = wrapped;
        copy(wrapped, this);
    }

    @Override
    public int
    getPartialTicks(
            long sysClock
    ) {
        copy(this, wrapped);
        try {
            return
                    wrapped.getPartialTicks(
                            sysClock
                    );
        } finally {
            copy(wrapped, this);
        }
    }

    protected void copy(Timer from, Timer to) {
        TimerAccessor fromA = (TimerAccessor) from;
        TimerAccessor toA = (TimerAccessor) to;

        to.renderPartialTicks = from.renderPartialTicks;
        toA.setLastSyncSysClock(fromA.getLastSyncSysClock());
        to.elapsedPartialTicks = from.elapsedPartialTicks;
        toA.setTickLength(fromA.getTickLength());
    }
}
