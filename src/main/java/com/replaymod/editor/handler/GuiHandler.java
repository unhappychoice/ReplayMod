package com.replaymod.editor.handler;

import com.replaymod.core.utils.Utils;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.editor.gui.GuiEditReplay;
import com.replaymod.gui.container.AbstractGuiScreen;
import com.replaymod.gui.container.GuiScreen;
import com.replaymod.gui.element.GuiButton;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.versions.callbacks.InitScreenCallback;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.crash.CrashReport;

import java.io.IOException;

public class GuiHandler extends EventRegistrations {
    {
        on(InitScreenCallback.EVENT, (vanillaGuiScreen, buttonList) -> injectIntoReplayViewer(vanillaGuiScreen));
    }

    public void injectIntoReplayViewer(net.minecraft.client.gui.screen.Screen vanillaGuiScreen) {
        AbstractGuiScreen guiScreen = GuiScreen.from(vanillaGuiScreen);
        if (!(guiScreen instanceof GuiReplayViewer)) {
            return;
        }
        final GuiReplayViewer replayViewer = (GuiReplayViewer) guiScreen;
        // Inject Edit button
        if (!replayViewer.editorButton.getChildren().isEmpty()) return;
        replayViewer.replaySpecificButtons.add(new GuiButton(replayViewer.editorButton).onClick(() -> {
            if (Utils.ifMinimalModeDoPopup(replayViewer, () -> {
            })) return;
            try {
                new GuiEditReplay(replayViewer, replayViewer.list.getSelected().get(0).file.toPath()) {
                    @Override
                    protected void close() {
                        super.close();
                        replayViewer.list.load();
                    }
                }.open();
            } catch (IOException e) {
                Utils.error(ReplayModEditor.LOGGER, replayViewer, CrashReport.makeCrashReport(e, "Opening replay editor"), () -> {
                });
            }
        }).setSize(73, 20).setI18nLabel("replaymod.gui.edit").setDisabled());
    }
}
