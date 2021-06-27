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
import com.replaymod.gui.function.Clickable;
import com.replaymod.gui.versions.MCVer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public abstract class AbstractGuiButton<T extends AbstractGuiButton<T>> extends AbstractGuiClickable<T> implements Clickable, IGuiButton<T> {
    protected static final ResourceLocation BUTTON_SOUND = new ResourceLocation("gui.button.press");
    protected static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation("textures/gui/widgets.png");

    private SoundEvent sound = SoundEvents.UI_BUTTON_CLICK;

    private int labelColor = 0xe0e0e0;
    private String label;

    private ResourceLocation texture;
    private ReadableDimension textureSize;
    private ReadablePoint spriteUV;
    private ReadableDimension spriteSize;

    public AbstractGuiButton() {
    }

    public AbstractGuiButton(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        color4f(1, 1, 1, 1);

        byte texture = 1;
        int color = labelColor;
        if (!isEnabled()) {
            texture = 0;
            color = 0xa0a0a0;
        } else if (isMouseHovering(new Point(renderInfo.mouseX, renderInfo.mouseY))) {
            texture = 2;
            color = 0xffffa0;
        }

        enableBlend();
        blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int textureY = 46 + texture * 20;
        int halfWidth = size.getWidth() / 2;
        int secondHalfWidth = size.getWidth() - halfWidth;
        int halfHeight = size.getHeight() / 2;
        int secondHalfHeight = size.getHeight() - halfHeight;

        renderer.bindTexture(WIDGETS_TEXTURE);
        renderer.drawTexturedRect(0, 0, 0, textureY, halfWidth, halfHeight);
        renderer.drawTexturedRect(0, halfHeight, 0, textureY + 20 - secondHalfHeight, halfWidth, secondHalfHeight);
        renderer.drawTexturedRect(halfWidth, 0, 200 - secondHalfWidth, textureY, secondHalfWidth, halfHeight);
        renderer.drawTexturedRect(halfWidth, halfHeight, 200 - secondHalfWidth, textureY + 20 - secondHalfHeight, secondHalfWidth, secondHalfHeight);

        if (this.texture != null) {
            renderer.bindTexture(this.texture);
            if (spriteUV != null && textureSize != null) {
                ReadableDimension spriteSize = this.spriteSize != null ? this.spriteSize : getMinSize();
                renderer.drawTexturedRect(0, 0, spriteUV.getX(), spriteUV.getY(), size.getWidth(), size.getHeight(),
                        spriteSize.getWidth(), spriteSize.getHeight(),
                        textureSize.getWidth(), textureSize.getHeight());
            } else {
                renderer.drawTexturedRect(0, 0, 0, 0, size.getWidth(), size.getHeight());
            }
        }

        if (label != null) {
            renderer.drawCenteredString(halfWidth, (size.getHeight() - 8) / 2, color, label, true);
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        if (label != null) {
            FontRenderer fontRenderer = MCVer.getFontRenderer();
            return new Dimension(fontRenderer.getStringWidth(label), 20);
        } else {
            return new Dimension(0, 0);
        }
    }

    @Override
    public void onClick() {
        playClickSound(getMinecraft());
        super.onClick();
    }

    public static void playClickSound(Minecraft mc) {
        playClickSound(mc, SoundEvents.UI_BUTTON_CLICK);
    }

    public static void playClickSound(Minecraft mc, SoundEvent sound) {
        mc.getSoundHandler().play(SimpleSound.master(sound, 1.0F));
    }

    @Override
    public T setLabel(String label) {
        this.label = label;
        return getThis();
    }

    @Override
    public T setSound(SoundEvent sound) {
        this.sound = sound;
        return getThis();
    }

    public SoundEvent getSound() {
        return this.sound;
    }

    @Override
    public T setI18nLabel(String label, Object... args) {
        return setLabel(I18n.format(label, args));
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    @Override
    public ResourceLocation getTexture() {
        return texture;
    }

    @Override
    public T setTexture(ResourceLocation texture) {
        this.texture = texture;
        return getThis();
    }

    @Override
    public ReadableDimension getTextureSize() {
        return textureSize;
    }

    @Override
    public T setTextureSize(ReadableDimension textureSize) {
        this.textureSize = textureSize;
        return getThis();
    }

    @Override
    public ReadablePoint getSpriteUV() {
        return spriteUV;
    }

    @Override
    public T setSpriteUV(ReadablePoint spriteUV) {
        this.spriteUV = spriteUV;
        return getThis();
    }

    @Override
    public ReadableDimension getSpriteSize() {
        return spriteSize;
    }

    @Override
    public T setSpriteSize(ReadableDimension spriteSize) {
        this.spriteSize = spriteSize;
        return getThis();
    }
}
