/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.gui.popup;

import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.container.GuiPanel;
import com.replaymod.gui.element.GuiButton;
import com.replaymod.gui.element.GuiElement;
import com.replaymod.gui.element.GuiLabel;
import com.replaymod.gui.function.Typeable;
import com.replaymod.gui.layout.VerticalLayout;
import com.replaymod.gui.utils.Colors;
import com.replaymod.gui.versions.MCVer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;


public class GuiInfoPopup extends AbstractGuiPopup<GuiInfoPopup> implements Typeable {
    public static GuiInfoPopup open(com.replaymod.gui.container.GuiContainer container, String... info) {
        com.replaymod.gui.element.GuiElement[] labels = new com.replaymod.gui.element.GuiElement[info.length];
        for (int i = 0; i < info.length; i++) {
            labels[i] = new GuiLabel().setI18nText(info[i]).setColor(com.replaymod.gui.utils.Colors.BLACK);
        }
        return open(container, labels);
    }

    public static GuiInfoPopup open(com.replaymod.gui.container.GuiContainer container, GuiElement... info) {
        GuiInfoPopup popup = new GuiInfoPopup(container).setBackgroundColor(Colors.DARK_TRANSPARENT);
        popup.getInfo().addElements(new com.replaymod.gui.layout.VerticalLayout.Data(0.5), info);
        popup.open();
        return popup;
    }

    private Runnable onClosed = () -> {
    };

    private final com.replaymod.gui.element.GuiButton closeButton = new com.replaymod.gui.element.GuiButton().setSize(150, 20).onClick(() -> {
        close();
        onClosed.run();
    }).setI18nLabel("gui.back");

    private final com.replaymod.gui.container.GuiPanel info = new com.replaymod.gui.container.GuiPanel().setMinSize(new Dimension(320, 50))
            .setLayout(new com.replaymod.gui.layout.VerticalLayout(com.replaymod.gui.layout.VerticalLayout.Alignment.TOP).setSpacing(2));

    {
        popup.setLayout(new com.replaymod.gui.layout.VerticalLayout().setSpacing(10))
                .addElements(new VerticalLayout.Data(0.5), info, closeButton);
    }

    private int layer;

    public GuiInfoPopup(GuiContainer container) {
        super(container);
    }

    public GuiInfoPopup setCloseLabel(String label) {
        closeButton.setLabel(label);
        return this;
    }

    public GuiInfoPopup setCloseI18nLabel(String label, Object... args) {
        closeButton.setI18nLabel(label, args);
        return this;
    }

    public GuiInfoPopup onClosed(Runnable onClosed) {
        this.onClosed = onClosed;
        return this;
    }

    @Override
    protected GuiInfoPopup getThis() {
        return this;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == MCVer.Keyboard.KEY_ESCAPE) {
            closeButton.onClick();
            return true;
        }
        return false;
    }

    public GuiButton getCloseButton() {
        return this.closeButton;
    }

    public GuiPanel getInfo() {
        return this.info;
    }

    public int getLayer() {
        return this.layer;
    }

    public GuiInfoPopup setLayer(int layer) {
        this.layer = layer;
        return this;
    }
}
