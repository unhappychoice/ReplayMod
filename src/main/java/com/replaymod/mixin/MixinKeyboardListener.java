package com.replaymod.mixin;

import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
import net.minecraft.client.KeyboardListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardListener.class)
public class MixinKeyboardListener {
    private static final String ON_KEY_PRESSED = "Lnet/minecraft/client/settings/KeyBinding;onTick(Lnet/minecraft/client/util/InputMappings$Input;)V";

    @Inject(method = "onKeyEvent", at = @At(value = "INVOKE", target = ON_KEY_PRESSED), cancellable = true)
    private void beforeKeyBindingTick(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (KeyEventCallback.EVENT.invoker().onKeyEvent(key, scanCode, action, modifiers)) {
            ci.cancel();
        }
    }

    @Inject(method = "onKeyEvent", at = @At(value = "INVOKE", target = ON_KEY_PRESSED, shift = At.Shift.AFTER))
    private void afterKeyBindingTick(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }
}
