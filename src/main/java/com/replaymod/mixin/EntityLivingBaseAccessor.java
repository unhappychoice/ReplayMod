package com.replaymod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

//#if MC>=10904
import net.minecraft.network.datasync.DataParameter;
//#endif

@Mixin(LivingEntity.class)
public interface EntityLivingBaseAccessor {
    //#if MC>=10904
    @Accessor("LIVING_FLAGS")
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static DataParameter<Byte> getLivingFlags() { return null; }
    //#endif
}
