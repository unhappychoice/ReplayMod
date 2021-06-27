package com.replaymod.mixin;

import com.replaymod.extras.advancedscreenshots.AdvancedScreenshots;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.KeyboardListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardListener.class)
public abstract class Mixin_KeyboardListener {
    @Inject(
            method = "onKeyEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;Ljava/util/function/Consumer;)V"
            ),
            cancellable = true
    )
    private void takeScreenshot(CallbackInfo ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            AdvancedScreenshots.take();
            ci.cancel();
        }
    }
}
