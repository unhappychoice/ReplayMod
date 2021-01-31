package com.replaymod.mixin;

import com.replaymod.replay.events.RenderHotbarCallback;
import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;
import net.minecraft.client.gui.IngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IngameGui.class)
public class MixinInGameHud {
    @Inject(method = "isTargetNamedMenuProvider", at = @At("HEAD"), cancellable = true)
    private void shouldRenderSpectatorCrosshair(CallbackInfoReturnable<Boolean> ci) {
        Boolean state = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair();
        if (state != null) {
            ci.setReturnValue(state);
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void shouldRenderHotbar(CallbackInfo ci) {
        Boolean state = RenderHotbarCallback.EVENT.invoker().shouldRenderHotbar();
        if (state == Boolean.FALSE) {
            ci.cancel();
        }
    }
}
