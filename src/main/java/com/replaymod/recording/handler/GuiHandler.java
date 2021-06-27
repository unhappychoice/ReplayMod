package com.replaymod.recording.handler;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.gui.GuiReplayButton;
import com.replaymod.gui.container.GuiScreen;
import com.replaymod.gui.container.VanillaGuiScreen;
import com.replaymod.gui.element.GuiButton;
import com.replaymod.gui.element.GuiCheckbox;
import com.replaymod.gui.element.GuiToggleButton;
import com.replaymod.gui.layout.CustomLayout;
import com.replaymod.gui.popup.GuiInfoPopup;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.versions.callbacks.InitScreenCallback;
import com.replaymod.mixin.AddServerScreenAccessor;
import com.replaymod.recording.ServerInfoExt;
import com.replaymod.recording.Setting;
import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;

public class GuiHandler extends EventRegistrations {

    private final ReplayMod mod;

    public GuiHandler(ReplayMod mod) {
        this.mod = mod;
    }

    {
        on(InitScreenCallback.EVENT, (screen, buttons) -> onGuiInit(screen));
    }

    private void onGuiInit(net.minecraft.client.gui.screen.Screen gui) {
        if (gui instanceof WorldSelectionScreen || gui instanceof MultiplayerScreen) {
            boolean sp = gui instanceof WorldSelectionScreen;
            SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
            Setting<Boolean> setting = sp ? Setting.RECORD_SINGLEPLAYER : Setting.RECORD_SERVER;

            GuiCheckbox recordingCheckbox = new GuiCheckbox()
                    .setI18nLabel("replaymod.gui.settings.record" + (sp ? "singleplayer" : "server"))
                    .setChecked(settingsRegistry.get(setting));
            recordingCheckbox.onClick(() -> {
                settingsRegistry.set(setting, recordingCheckbox.isChecked());
                settingsRegistry.save();
            });

            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(gui);
            vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    //size(recordingCheckbox, 200, 20);
                    pos(recordingCheckbox, width - width(recordingCheckbox) - 5, 5);
                }
            }).addElements(null, recordingCheckbox);
        }

        if (gui instanceof AddServerScreen) {
            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(gui);
            GuiButton replayButton = new GuiReplayButton().onClick(() -> {
                ServerData serverInfo = ((AddServerScreenAccessor) gui).getServer();
                ServerInfoExt serverInfoExt = ServerInfoExt.from(serverInfo);
                Boolean state = serverInfoExt.getAutoRecording();
                GuiToggleButton<String> autoRecording = new GuiToggleButton<String>()
                        .setI18nLabel("replaymod.gui.settings.autostartrecording")
                        .setValues(
                                I18n.format("replaymod.gui.settings.default"),
                                I18n.format("options.off"),
                                I18n.format("options.on")
                        )
                        .setSelected(state == null ? 0 : state ? 2 : 1);
                autoRecording.onClick(() -> {
                    int selected = autoRecording.getSelected();
                    serverInfoExt.setAutoRecording(selected == 0 ? null : selected == 2);
                });
                GuiInfoPopup.open(vanillaGui, autoRecording);
            });
            vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    size(replayButton, 20, 20);
                    pos(replayButton, width - width(replayButton) - 5, 5);
                }
            }).addElements(null, replayButton);
        }
    }
}
