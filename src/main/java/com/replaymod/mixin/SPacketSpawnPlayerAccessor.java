package com.replaymod.mixin;

import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SSpawnPlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SSpawnPlayerPacket.class)
public interface SPacketSpawnPlayerAccessor {
    //#if MC<11500
    //$$ @Accessor("dataTracker")
    //$$ DataTracker getDataManager();
    //$$ @Accessor("dataTracker")
    //$$ void setDataManager(DataTracker value);
    //#endif
}
