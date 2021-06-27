package com.replaymod.mixin;

import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
    // TODO probably https://github.com/ReplayMod/remap/issues/10
    @Accessor("isGamePaused")
    boolean isGamePaused();
}
