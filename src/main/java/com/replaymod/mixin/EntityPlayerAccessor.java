package com.replaymod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface EntityPlayerAccessor extends Mixin_EntityLivingBaseAccessor {
    @Accessor
    ItemStack getItemStackMainHand();

    @Accessor
    void setItemStackMainHand(ItemStack value);
}
