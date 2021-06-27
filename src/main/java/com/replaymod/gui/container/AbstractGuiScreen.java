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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.replaymod.gui.GuiRenderer;
import com.replaymod.gui.MinecraftGuiRenderer;
import com.replaymod.gui.OffsetGuiRenderer;
import com.replaymod.gui.RenderInfo;
import com.replaymod.gui.element.GuiElement;
import com.replaymod.gui.element.GuiLabel;
import com.replaymod.gui.function.Loadable;
import com.replaymod.gui.utils.MouseUtils;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;


public abstract class AbstractGuiScreen<T extends AbstractGuiScreen<T>> extends AbstractGuiContainer<T> {

    private final MinecraftGuiScreen wrapped = new MinecraftGuiScreen();

    private Dimension screenSize;

    private Background background = Background.DEFAULT;

    private boolean enabledRepeatedKeyEvents = true;

    private com.replaymod.gui.element.GuiLabel title;

    protected boolean suppressVanillaKeys;

    public net.minecraft.client.gui.screen.Screen toMinecraft() {
        return wrapped;
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            size = screenSize;
        }
        if (renderInfo.layer == 0) {
            if (title != null) {
                title.layout(title.getMinSize(), renderInfo);
            }
        }
        super.layout(size, renderInfo);
        if (renderInfo.layer == getMaxLayer()) {
            final com.replaymod.gui.element.GuiElement tooltip = forEach(com.replaymod.gui.element.GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        if (renderInfo.layer == 0) {
            switch (background) {
                case NONE:
                    break;
                case DEFAULT:
                    wrapped.renderBackground(renderer.getMatrixStack());
                    break;
                case TRANSPARENT:
                    int top = 0xc0_10_10_10, bottom = 0xd0_10_10_10;
                    renderer.drawRect(0, 0, size.getWidth(), size.getHeight(), top, top, bottom, bottom);
                    break;
                case DIRT:
                    wrapped.renderDirtBackground(0);
                    break;
            }
            if (title != null) {
                ReadableDimension titleSize = title.getMinSize();
                int x = screenSize.getWidth() / 2 - titleSize.getWidth() / 2;
                OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, new Point(x, 10), new Dimension(0, 0));
                title.draw(eRenderer, titleSize, renderInfo);
            }
        }
        super.draw(renderer, size, renderInfo);
        if (renderInfo.layer == getMaxLayer()) {
            final com.replaymod.gui.element.GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                final ReadableDimension tooltipSize = tooltip.getMinSize();
                int x, y;
                if (renderInfo.mouseX + 8 + tooltipSize.getWidth() < screenSize.getWidth()) {
                    x = renderInfo.mouseX + 8;
                } else {
                    x = screenSize.getWidth() - tooltipSize.getWidth() - 1;
                }
                if (renderInfo.mouseY + 8 + tooltipSize.getHeight() < screenSize.getHeight()) {
                    y = renderInfo.mouseY + 8;
                } else {
                    y = screenSize.getHeight() - tooltipSize.getHeight() - 1;
                }
                final ReadablePoint position = new Point(x, y);
                try {
                    OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, position, tooltipSize);
                    tooltip.draw(eRenderer, tooltipSize, renderInfo);
                } catch (Exception ex) {
                    CrashReport crashReport = CrashReport.makeCrashReport(ex, "Rendering Gui Tooltip");
                    renderInfo.addTo(crashReport);
                    CrashReportCategory category = crashReport.makeCategory("Gui container details");
                    com.replaymod.gui.versions.MCVer.addDetail(category, "Container", this::toString);
                    com.replaymod.gui.versions.MCVer.addDetail(category, "Width", () -> "" + size.getWidth());
                    com.replaymod.gui.versions.MCVer.addDetail(category, "Height", () -> "" + size.getHeight());
                    category = crashReport.makeCategory("Tooltip details");
                    com.replaymod.gui.versions.MCVer.addDetail(category, "Element", tooltip::toString);
                    com.replaymod.gui.versions.MCVer.addDetail(category, "Position", position::toString);
                    com.replaymod.gui.versions.MCVer.addDetail(category, "Size", tooltipSize::toString);
                    throw new ReportedException(crashReport);
                }
            }
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        return screenSize;
    }

    @Override
    public ReadableDimension getMaxSize() {
        return screenSize;
    }

    public void setEnabledRepeatedKeyEvents(boolean enableRepeatKeyEvents) {
        this.enabledRepeatedKeyEvents = enableRepeatKeyEvents;
        if (wrapped.active) {
            com.replaymod.gui.versions.MCVer.Keyboard.enableRepeatEvents(enableRepeatKeyEvents);
        }
    }

    public void display() {
        getMinecraft().displayGuiScreen(toMinecraft());
    }

    public Background getBackground() {
        return this.background;
    }

    public boolean isEnabledRepeatedKeyEvents() {
        return this.enabledRepeatedKeyEvents;
    }

    public com.replaymod.gui.element.GuiLabel getTitle() {
        return this.title;
    }

    public void setBackground(Background background) {
        this.background = background;
    }

    public void setTitle(GuiLabel title) {
        this.title = title;
    }

    protected class MinecraftGuiScreen extends net.minecraft.client.gui.screen.Screen {
        private boolean active;

        protected MinecraftGuiScreen() {
            super(null);
        }

        @Override
        public String getNarrationMessage() {
            return title == null ? "" : title.getString();
        }

        @Override
        public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
            // The Forge loading screen apparently leaves one of the textures of the GlStateManager in an
            // incorrect state which can cause the whole screen to just remain white. This is a workaround.

            int layers = getMaxLayer();
            RenderInfo renderInfo = new RenderInfo(partialTicks, mouseX, mouseY, 0);
            for (int layer = 0; layer <= layers; layer++) {
                layout(screenSize, renderInfo.layer(layer));
            }
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            for (int layer = 0; layer <= layers; layer++) {
                draw(renderer, screenSize, renderInfo.layer(layer));
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            Point mouse = MouseUtils.getMousePos();
            boolean ctrlDown = hasControlDown();
            boolean shiftDown = hasShiftDown();
            if (!invokeHandlers(com.replaymod.gui.function.Typeable.class, e -> e.typeKey(mouse, keyCode, '\0', ctrlDown, shiftDown))) {
                if (suppressVanillaKeys) {
                    return false;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char keyChar, int scanCode) {
            Point mouse = MouseUtils.getMousePos();
            boolean ctrlDown = hasControlDown();
            boolean shiftDown = hasShiftDown();
            if (!invokeHandlers(com.replaymod.gui.function.Typeable.class, e -> e.typeKey(mouse, 0, keyChar, ctrlDown, shiftDown))) {
                if (suppressVanillaKeys) {
                    return false;
                }
                return super.charTyped(keyChar, scanCode);
            }
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseXD, double mouseYD, int mouseButton) {
            int mouseX = (int) Math.round(mouseXD), mouseY = (int) Math.round(mouseYD);
            return
                    invokeHandlers(com.replaymod.gui.function.Clickable.class, e -> e.mouseClick(new Point(mouseX, mouseY), mouseButton));
        }

        @Override
        public boolean mouseReleased(double mouseXD, double mouseYD, int mouseButton) {
            int mouseX = (int) Math.round(mouseXD), mouseY = (int) Math.round(mouseYD);
            return
                    invokeHandlers(com.replaymod.gui.function.Draggable.class, e -> e.mouseRelease(new Point(mouseX, mouseY), mouseButton));
        }

        @Override
        public boolean mouseDragged(double mouseXD, double mouseYD, int mouseButton, double deltaX, double deltaY) {
            int mouseX = (int) Math.round(mouseXD), mouseY = (int) Math.round(mouseYD);
            long timeSinceLastClick = 0;
            return
                    invokeHandlers(com.replaymod.gui.function.Draggable.class, e -> e.mouseDrag(new Point(mouseX, mouseY), mouseButton, timeSinceLastClick));
        }

        @Override
        public void tick() {
            invokeAll(com.replaymod.gui.function.Tickable.class, com.replaymod.gui.function.Tickable::tick);
        }

        @Override
        public boolean mouseScrolled(
                double mouseX,
                double mouseY,
                double dWheel
        ) {
            Point mouse = new Point((int) mouseX, (int) mouseY);
            int wheel = (int) (dWheel * 120);
            return invokeHandlers(com.replaymod.gui.function.Scrollable.class, e -> e.scroll(mouse, wheel));
        }

        @Override
        public void onClose() {
            invokeAll(com.replaymod.gui.function.Closeable.class, com.replaymod.gui.function.Closeable::close);
            active = false;
            if (enabledRepeatedKeyEvents) {
                com.replaymod.gui.versions.MCVer.Keyboard.enableRepeatEvents(false);
            }
        }

        @Override
        public void init() {
            active = false;
            if (enabledRepeatedKeyEvents) {
                com.replaymod.gui.versions.MCVer.Keyboard.enableRepeatEvents(true);
            }
            screenSize = new Dimension(width, height);
            invokeAll(com.replaymod.gui.function.Loadable.class, Loadable::load);
        }

        public T getWrapper() {
            return AbstractGuiScreen.this.getThis();
        }
    }

    public enum Background {
        NONE, DEFAULT, TRANSPARENT, DIRT;
    }
}
