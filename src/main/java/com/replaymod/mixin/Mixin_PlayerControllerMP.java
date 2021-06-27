package com.replaymod.mixin;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.util.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.stats.StatisticsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerController.class)
public abstract class Mixin_PlayerControllerMP {

    @Shadow
    private Minecraft mc;

    @Shadow
    private ClientPlayNetHandler connection;

    @Inject(method = "createPlayer(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/stats/StatisticsManager;Lnet/minecraft/client/util/ClientRecipeBook;ZZ)Lnet/minecraft/client/entity/player/ClientPlayerEntity;", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_createReplayCamera(
            ClientWorld worldIn,
            StatisticsManager statisticsManager,
            ClientRecipeBook recipeBookClient,
            boolean lastIsHoldingSneakKey,
            boolean lastSprinting,
            CallbackInfoReturnable<ClientPlayerEntity> ci
    ) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.connection, statisticsManager, recipeBookClient));
            ci.cancel();
        }
    }

    @Inject(method = "isSpectatorMode", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_isSpectator(CallbackInfoReturnable<Boolean> ci) {
        if (this.mc.player instanceof CameraEntity) { // this check should in theory not be required
            ci.setReturnValue(this.mc.player.isSpectator());
        }
    }

}
