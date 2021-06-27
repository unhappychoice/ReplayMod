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
import com.replaymod.gui.function.Scrollable;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.utils.MouseUtils;
import com.replaymod.gui.versions.MCVer;
import com.replaymod.gui.versions.callbacks.PreTickCallback;
import com.replaymod.gui.versions.callbacks.RenderHudCallback;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.text.StringTextComponent;


public abstract class AbstractGuiOverlay<T extends AbstractGuiOverlay<T>> extends AbstractGuiContainer<T> {

    private final UserInputGuiScreen userInputGuiScreen = new UserInputGuiScreen();
    private final EventHandler eventHandler = new EventHandler();
    private boolean visible;
    private Dimension screenSize;
    private boolean mouseVisible;
    private boolean closeable = true;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (visible) {
                invokeAll(com.replaymod.gui.function.Loadable.class, com.replaymod.gui.function.Loadable::load);
                eventHandler.register();
            } else {
                invokeAll(com.replaymod.gui.function.Closeable.class, com.replaymod.gui.function.Closeable::close);
                eventHandler.unregister();
            }
            updateUserInputGui();
        }
        this.visible = visible;
    }

    public boolean isMouseVisible() {
        return mouseVisible;
    }

    public void setMouseVisible(boolean mouseVisible) {
        this.mouseVisible = mouseVisible;
        updateUserInputGui();
    }

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
    }

    /**
     * @see #setAllowUserInput(boolean)
     */
    public boolean isAllowUserInput() {
        return userInputGuiScreen.passEvents;
    }

    /**
     * Enable/Disable user input for this overlay while the mouse is visible.
     * User input are things like moving the player, attacking/interacting, key bindings but not input into the
     * GUI elements such as text fields.
     * Default for overlays is {@code true} whereas for normal GUI screens it is {@code false}.
     *
     * @param allowUserInput {@code true} to allow user input, {@code false} to disallow it
     * @see net.minecraft.client.gui.screen.Screen#passEvents
     */
    public void setAllowUserInput(boolean allowUserInput) {
        userInputGuiScreen.passEvents = allowUserInput;
    }

    private void updateUserInputGui() {
        Minecraft mc = getMinecraft();
        if (visible) {
            if (mouseVisible) {
                if (mc.currentScreen == null) {
                    mc.displayGuiScreen(userInputGuiScreen);
                }
            } else {
                if (mc.currentScreen == userInputGuiScreen) {
                    mc.displayGuiScreen(null);
                }
            }
        }
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            size = screenSize;
        }
        super.layout(size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
            final com.replaymod.gui.element.GuiElement tooltip = forEach(com.replaymod.gui.element.GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
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

    private class EventHandler extends EventRegistrations {
        private EventHandler() {
        }

        {
            on(RenderHudCallback.EVENT, this::renderOverlay);
        }

        private void renderOverlay(MatrixStack stack, float partialTicks) {
            updateUserInputGui();
            updateRenderer();
            int layers = getMaxLayer();
            int mouseX = -1, mouseY = -1;
            if (mouseVisible) {
                Point mouse = MouseUtils.getMousePos();
                mouseX = mouse.getX();
                mouseY = mouse.getY();
            }
            RenderInfo renderInfo = new RenderInfo(partialTicks, mouseX, mouseY, 0);
            for (int layer = 0; layer <= layers; layer++) {
                layout(screenSize, renderInfo.layer(layer));
            }
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            for (int layer = 0; layer <= layers; layer++) {
                draw(renderer, screenSize, renderInfo.layer(layer));
            }
        }

        {
            on(PreTickCallback.EVENT, () -> invokeAll(com.replaymod.gui.function.Tickable.class, com.replaymod.gui.function.Tickable::tick));
        }

        private void updateRenderer() {
            Minecraft mc = getMinecraft();
            MainWindow
                    res = MCVer.newScaledResolution(mc);
            if (screenSize == null
                    || screenSize.getWidth() != res.getScaledWidth()
                    || screenSize.getHeight() != res.getScaledHeight()) {
                screenSize = new Dimension(res.getScaledWidth(), res.getScaledHeight());
            }
        }
    }

    protected class UserInputGuiScreen extends net.minecraft.client.gui.screen.Screen {

        UserInputGuiScreen() {
            super(new StringTextComponent(""));
        }

        {
            this.passEvents = true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            Point mousePos = MouseUtils.getMousePos();
            boolean controlDown = hasControlDown();
            boolean shiftDown = hasShiftDown();
            if (!invokeHandlers(com.replaymod.gui.function.Typeable.class, e -> e.typeKey(mousePos, keyCode, '\0', controlDown, shiftDown))) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char keyChar, int modifiers) {
            Point mousePos = MouseUtils.getMousePos();
            boolean controlDown = hasControlDown();
            boolean shiftDown = hasShiftDown();
            if (!invokeHandlers(com.replaymod.gui.function.Typeable.class, e -> e.typeKey(mousePos, 0, keyChar, controlDown, shiftDown))) {
                return super.charTyped(keyChar, modifiers);
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
            return invokeHandlers(Scrollable.class, e -> e.scroll(mouse, wheel));
        }

        @Override
        public void closeScreen() {
            if (closeable) {
                super.closeScreen();
            }
        }

        @Override
        public void onClose() {
            if (closeable) {
                mouseVisible = false;
            }
        }

        public AbstractGuiOverlay<T> getOverlay() {
            return AbstractGuiOverlay.this;
        }
    }
}
