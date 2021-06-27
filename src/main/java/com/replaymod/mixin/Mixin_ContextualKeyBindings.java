package com.replaymod.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * We have bunch of keybindings which only have an effect while in a replay but heavily conflict with vanilla ones
 * otherwise. To work around this, we prevent our keybindings (or conflicting ones) from making it into the keysByCode
 * map, depending on the current context.
 */
@Mixin(KeyBinding.class)
public class Mixin_ContextualKeyBindings {
    @Shadow
    @Final
    private static Map<String, KeyBinding> KEYBIND_ARRAY;

    @Unique
    private static Collection<KeyBinding> keyBindings() {
        return Mixin_ContextualKeyBindings.KEYBIND_ARRAY.values();
    }

    @Unique
    private static final List<KeyBinding> temporarilyRemoved = new ArrayList<>();

    @Inject(method = "resetKeyBindingArrayAndHash", at = @At("HEAD"))
    private static void preContextualKeyBindings(CallbackInfo ci) {
        ReplayMod mod = ReplayMod.instance;
        if (mod == null) {
            return;
        }
        Set<KeyBinding> onlyInReplay = mod.getKeyBindingRegistry().getOnlyInReplay();
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            // In replay, remove any conflicting key bindings, so that ours are guaranteed in
            keyBindings().removeIf(keyBinding -> {
                for (KeyBinding exclusiveBinding : onlyInReplay) {
                    if (keyBinding.conflicts(exclusiveBinding) && keyBinding != exclusiveBinding) {
                        temporarilyRemoved.add(keyBinding);
                        return true;
                    }
                }
                return false;
            });
        } else {
            // Not in a replay, remove all replay-exclusive keybindings
            keyBindings().removeIf(keyBinding -> {
                if (onlyInReplay.contains(keyBinding)) {
                    temporarilyRemoved.add(keyBinding);
                    return true;
                }
                return false;
            });
        }
    }

    @Inject(method = "resetKeyBindingArrayAndHash", at = @At("RETURN"))
    private static void postContextualKeyBindings(CallbackInfo ci) {
        for (KeyBinding keyBinding : temporarilyRemoved) {
            Mixin_ContextualKeyBindings.KEYBIND_ARRAY.put(keyBinding.getKeyDescription(), keyBinding);
        }
        temporarilyRemoved.clear();
    }
}
