package com.replaymod.mixin;

import com.replaymod.extras.ReplayModExtras;
import com.replaymod.extras.playeroverview.PlayerOverview;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This mixin prevents players that are hidden in the PlayerOverview from being rendered.
 * <p>
 * Cancelling the RenderPlayerEvent.Pre is insufficient because it affects neither the shadows nor the fire texture.
 * See: https://github.com/MinecraftForge/MinecraftForge/issues/2987
 * <p>
 * The previous solution was to overwrite the RenderPlayer instances which has been dropped in favor of this one
 * because it is less compatible with other mods whereas this one should be fine as long as no other mod completely
 * overwrites the shouldRender method.
 * One example of the previous solution breaking is when used with VanillaEnhancements because it replaces the
 * RenderManager with a new custom one which in turn will reset our registered RenderPlayer instances because
 * it does so after we have already registered with the old RenderManager.
 * <p>
 * For 1.7.10, that method doesn't exist, so we use a combination of the event and inject into
 */
@Mixin(value = EntityRenderer.class, priority = 1200)
public abstract class MixinRender {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void replayModExtras_isPlayerHidden(Entity entity, @Coerce Object camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> ci) {
        ReplayModExtras.instance.get(PlayerOverview.class).ifPresent(playerOverview -> {
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                if (playerOverview.isHidden(player.getUniqueID())) {
                    ci.setReturnValue(false);
                }
            }
        });
    }
}
