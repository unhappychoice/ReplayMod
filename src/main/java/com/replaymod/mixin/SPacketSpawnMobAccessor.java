package com.replaymod.mixin;

import net.minecraft.network.play.server.SSpawnMobPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SSpawnMobPacket.class)
public interface SPacketSpawnMobAccessor {
}
