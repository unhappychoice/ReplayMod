package com.replaymod.extras.youtube;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;
import com.replaymod.gui.container.AbstractGuiScreen;
import com.replaymod.gui.container.GuiScreen;
import com.replaymod.gui.element.GuiButton;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.versions.callbacks.InitScreenCallback;
import com.replaymod.render.gui.GuiRenderingDone;
import net.minecraft.client.gui.screen.Screen;

public class YoutubeUpload extends EventRegistrations implements Extra {
    @Override
    public void register(ReplayMod mod) {
        register();
    }

    {
        on(InitScreenCallback.EVENT, ((screen, buttons) -> onGuiOpen(screen)));
    }

    private void onGuiOpen(Screen vanillaGui) {
        AbstractGuiScreen<?> abstractScreen = GuiScreen.from(vanillaGui);
        if (abstractScreen instanceof GuiRenderingDone) {
            GuiRenderingDone gui = (GuiRenderingDone) abstractScreen;
            // Check if there already is a youtube button
            if (gui.actionsPanel.getChildren().stream().anyMatch(it -> it instanceof YoutubeButton)) {
                return; // Button already added
            }
            // Add the Upload to YouTube button to actions panel
            gui.actionsPanel.addElements(null,
                    new YoutubeButton().onClick(() ->
                            new GuiYoutubeUpload(gui, gui.videoFile, gui.videoFrames, gui.settings).display()
                    ).setSize(200, 20).setI18nLabel("replaymod.gui.youtubeupload"));
        }
    }

    private static class YoutubeButton extends GuiButton {
    }
}
