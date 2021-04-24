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
package com.replaymod.gui.container;

import com.replaymod.gui.element.GuiElement;
import com.replaymod.gui.layout.LayoutData;
import com.replaymod.gui.layout.Layout;

import java.util.ArrayList;
import java.util.Map;

public class GuiPanel extends AbstractGuiContainer<GuiPanel> {

    public GuiPanel() {
    }

    public GuiPanel(GuiContainer container) {
        super(container);
    }

    GuiPanel(Layout layout, int width , int height, Map<com.replaymod.gui.element.GuiElement, com.replaymod.gui.layout.LayoutData> withElements) {
        setLayout(layout);
        if (width != 0 || height != 0) {
            setSize(width, height);
        }
        for (Map.Entry<com.replaymod.gui.element.GuiElement, com.replaymod.gui.layout.LayoutData> e : withElements.entrySet()) {
            addElements(e.getValue(), e.getKey());
        }
    }

    public static GuiPanelBuilder builder() {
        return new GuiPanelBuilder();
    }

    @Override
    protected GuiPanel getThis() {
        return this;
    }

    public static class GuiPanelBuilder {
        private Layout layout;
        private int width;
        private int height;
        private ArrayList<com.replaymod.gui.element.GuiElement> withElements$key;
        private ArrayList<com.replaymod.gui.layout.LayoutData> withElements$value;

        GuiPanelBuilder() {
        }

        public GuiPanelBuilder layout(Layout layout) {
            this.layout = layout;
            return this;
        }

        public GuiPanelBuilder width(int width) {
            this.width = width;
            return this;
        }

        public GuiPanelBuilder height(int height) {
            this.height = height;
            return this;
        }

        public GuiPanelBuilder with(com.replaymod.gui.element.GuiElement withKey, com.replaymod.gui.layout.LayoutData withValue) {
            if (this.withElements$key == null) {
                this.withElements$key = new ArrayList<com.replaymod.gui.element.GuiElement>();
                this.withElements$value = new ArrayList<com.replaymod.gui.layout.LayoutData>();
            }
            this.withElements$key.add(withKey);
            this.withElements$value.add(withValue);
            return this;
        }

        public GuiPanelBuilder withElements(Map<? extends com.replaymod.gui.element.GuiElement, ? extends com.replaymod.gui.layout.LayoutData> withElements) {
            if (this.withElements$key == null) {
                this.withElements$key = new ArrayList<com.replaymod.gui.element.GuiElement>();
                this.withElements$value = new ArrayList<com.replaymod.gui.layout.LayoutData>();
            }
            for (final Map.Entry<? extends com.replaymod.gui.element.GuiElement, ? extends com.replaymod.gui.layout.LayoutData> $lombokEntry : withElements.entrySet()) {
                this.withElements$key.add($lombokEntry.getKey());
                this.withElements$value.add($lombokEntry.getValue());
            }
            return this;
        }

        public GuiPanelBuilder clearWithElements() {
            if (this.withElements$key != null) {
                this.withElements$key.clear();
                this.withElements$value.clear();
            }
            return this;
        }

        public GuiPanel build() {
            Map<com.replaymod.gui.element.GuiElement, com.replaymod.gui.layout.LayoutData> withElements;
            switch (this.withElements$key == null ? 0 : this.withElements$key.size()) {
                case 0:
                    withElements = java.util.Collections.emptyMap();
                    break;
                case 1:
                    withElements = java.util.Collections.singletonMap(this.withElements$key.get(0), this.withElements$value.get(0));
                    break;
                default:
                    withElements = new java.util.LinkedHashMap<GuiElement, com.replaymod.gui.layout.LayoutData>(this.withElements$key.size() < 1073741824 ? 1 + this.withElements$key.size() + (this.withElements$key.size() - 3) / 3 : Integer.MAX_VALUE);
                    for (int $i = 0; $i < this.withElements$key.size(); $i++)
                        withElements.put(this.withElements$key.get($i), (LayoutData) this.withElements$value.get($i));
                    withElements = java.util.Collections.unmodifiableMap(withElements);
            }

            return new GuiPanel(layout, width, height, withElements);
        }

        public String toString() {
            return "GuiPanel.GuiPanelBuilder(layout=" + this.layout + ", width=" + this.width + ", height=" + this.height + ", withElements$key=" + this.withElements$key + ", withElements$value=" + this.withElements$value + ")";
        }
    }
}
