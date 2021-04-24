package com.replaymod.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface Mixin_EntityLivingBaseAccessor {
    //#if MC>=11400
    @Accessor("serverX")
    double getInterpTargetX();
    @Accessor("serverY")
    double getInterpTargetY();
    @Accessor("serverZ")
    double getInterpTargetZ();
    @Accessor("serverYaw")
    double getInterpTargetYaw();
    @Accessor("serverPitch")
    double getInterpTargetPitch();
    //#endif

    //#if MC>=10904
    @Accessor("itemUseTimeLeft")
    int getActiveItemStackUseCount();
    @Accessor("itemUseTimeLeft")
    void setActiveItemStackUseCount(int value);
    //#endif
}
