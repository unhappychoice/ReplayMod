package com.replaymod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface Mixin_EntityLivingBaseAccessor {
    @Accessor
    double getInterpTargetX();

    @Accessor
    double getInterpTargetY();

    @Accessor
    double getInterpTargetZ();

    @Accessor
    double getInterpTargetYaw();

    @Accessor
    double getInterpTargetPitch();

    @Accessor
    int getActiveItemStackUseCount();

    @Accessor
    void setActiveItemStackUseCount(int value);
}
