package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.gui.element.GuiImage;
import com.replaymod.gui.element.IGuiImage;
import com.replaymod.gui.layout.HorizontalLayout;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.events.ReplayOpenedCallback;
import com.replaymod.replay.gui.overlay.GuiReplayOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

public class FullBrightness extends EventRegistrations implements Extra {
    private ReplayMod core;
    private ReplayModReplay module;
    private IGuiImage indicator;

    private Minecraft mc;
    private boolean active;
    private double originalGamma;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.core = mod;
        this.module = ReplayModReplay.instance;
        this.indicator = new GuiImage().setTexture(ReplayMod.TEXTURE, 90, 20, 19, 16).setSize(19, 16);
        this.mc = mod.getMinecraft();

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.lighting", Keyboard.KEY_Z, new Runnable() {
            @Override
            public void run() {
                active = !active;
                // need to tick once to update lightmap when replay is paused
                mod.getMinecraft().gameRenderer.tick();
                ReplayHandler replayHandler = module.getReplayHandler();
                if (replayHandler != null) {
                    updateIndicator(replayHandler.getOverlay());
                }
            }
        }, true);

        register();
    }

    public Type getType() {
        String str = core.getSettingsRegistry().get(Setting.FULL_BRIGHTNESS);
        for (Type type : Type.values()) {
            if (type.toString().equals(str)) {
                return type;
            }
        }
        return Type.Gamma;
    }

    {
        on(PreRenderCallback.EVENT, this::preRender);
    }

    private void preRender() {
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                originalGamma = mc.gameSettings.gamma;
                mc.gameSettings.gamma = 1000;
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.player != null) {
                    mc.player.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION
                            , Integer.MAX_VALUE));
                }
            }
        }
    }

    {
        on(PostRenderCallback.EVENT, this::postRender);
    }

    private void postRender() {
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                mc.gameSettings.gamma = originalGamma;
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.player != null) {
                    mc.player.removePotionEffect(Effects.NIGHT_VISION
                    );
                }
            }
        }
    }

    {
        on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay()));
    }

    private void updateIndicator(GuiReplayOverlay overlay) {
        if (active) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }

    enum Type {
        Gamma,
        NightVision,
        Both,
        ;

        @Override
        public String toString() {
            return "replaymod.gui.settings.fullbrightness." + name().toLowerCase();
        }
    }
}
