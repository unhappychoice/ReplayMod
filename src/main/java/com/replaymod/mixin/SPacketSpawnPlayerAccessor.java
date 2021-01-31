package com.replaymod.mixin;

import net.minecraft.network.play.server.SSpawnPlayerPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SSpawnPlayerPacket.class)
public interface SPacketSpawnPlayerAccessor {
}
