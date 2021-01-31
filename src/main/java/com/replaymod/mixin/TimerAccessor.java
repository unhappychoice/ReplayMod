package com.replaymod.mixin;

import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timer.class)
public interface TimerAccessor {
    @Accessor
    long getLastSyncSysClock();

    @Accessor
    void setLastSyncSysClock(long value);

    @Accessor
    float getTickLength();

    @Accessor
    void setTickLength(float value);
}
