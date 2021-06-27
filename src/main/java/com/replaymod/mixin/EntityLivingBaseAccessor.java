package com.replaymod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;

@Mixin(LivingEntity.class)
public interface EntityLivingBaseAccessor {
    @Accessor("LIVING_FLAGS")
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static DataParameter<Byte> getLivingFlags() {
        return null;
    }
}
