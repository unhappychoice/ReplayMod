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
package com.replaymod.gui.element;

import com.replaymod.gui.GuiRenderer;
import com.replaymod.gui.RenderInfo;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.versions.MCVer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractGuiLabel<T extends AbstractGuiLabel<T>> extends AbstractGuiElement<T> implements IGuiLabel<T> {
    private String text = "";

    private ReadableColor color = ReadableColor.WHITE, disabledColor = ReadableColor.GREY;

    public AbstractGuiLabel() {
    }

    public AbstractGuiLabel(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        FontRenderer fontRenderer = MCVer.getFontRenderer();
        List<String> lines = fontRenderer.getCharacterManager().func_238362_b_(new StringTextComponent(text), size.getWidth(), Style.EMPTY).stream()
                .map(it -> it.getComponent(Optional::of)).filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
        int y = 0;
        for (String line : lines) {
            renderer.drawString(0, y, isEnabled() ? color : disabledColor, line);
            y += fontRenderer.FONT_HEIGHT;
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        FontRenderer fontRenderer = MCVer.getFontRenderer();
        return new Dimension(fontRenderer.getStringWidth(text), fontRenderer.FONT_HEIGHT);
    }

    @Override
    public ReadableDimension getMaxSize() {
        return getMinSize();
    }

    @Override
    public T setText(String text) {
        this.text = text;
        return getThis();
    }

    @Override
    public T setI18nText(String text, Object... args) {
        return setText(I18n.format(text, args));
    }

    @Override
    public T setColor(ReadableColor color) {
        this.color = color;
        return getThis();
    }

    @Override
    public T setDisabledColor(ReadableColor disabledColor) {
        this.disabledColor = disabledColor;
        return getThis();
    }

    public String getText() {
        return this.text;
    }

    public ReadableColor getColor() {
        return this.color;
    }

    public ReadableColor getDisabledColor() {
        return this.disabledColor;
    }
}
