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
package com.replaymod.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.gui.utils.NonNull;
import de.johni0702.minecraft.gui.utils.lwjgl.Color;
import de.johni0702.minecraft.gui.utils.lwjgl.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public class MinecraftGuiRenderer implements GuiRenderer {

    private final AbstractGui gui = new AbstractGui() {
    };
    private final Minecraft mc = com.replaymod.gui.versions.MCVer.getMinecraft();

    private final MatrixStack matrixStack;

    @NonNull
    private final int scaledWidth = com.replaymod.gui.versions.MCVer.newScaledResolution(mc).getScaledWidth();
    private final int scaledHeight = com.replaymod.gui.versions.MCVer.newScaledResolution(mc).getScaledHeight();
    private final double scaleFactor = com.replaymod.gui.versions.MCVer.newScaledResolution(mc).getGuiScaleFactor();

    public MinecraftGuiRenderer(MatrixStack matrixStack) {
        this.matrixStack = matrixStack;
    }

    @Override
    public ReadablePoint getOpenGlOffset() {
        return new Point(0, 0);
    }

    @Override
    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    @Override
    public ReadableDimension getSize() {
        return new ReadableDimension() {
            @Override
            public int getWidth() {
                return scaledWidth;
            }

            @Override
            public int getHeight() {
                return scaledHeight;
            }

            @Override
            public void getSize(WritableDimension dest) {
                dest.setSize(getWidth(), getHeight());
            }
        };
    }

    @Override
    public void setDrawingArea(int x, int y, int width, int height) {
        // glScissor origin is bottom left corner whereas otherwise it's top left
        y = scaledHeight - y - height;

        int f = (int) scaleFactor;
        GL11.glScissor(x * f, y * f, width * f, height * f);
    }

    @Override
    public void bindTexture(ResourceLocation location) {
        com.replaymod.gui.versions.MCVer.getMinecraft().getTextureManager().bindTexture(location);
    }

    @Override
    public void bindTexture(int glId) {
        GlStateManager.bindTexture(glId);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        gui.blit(matrixStack, x, y, u, v, width, height);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        color(1, 1, 1);
        AbstractGui.blit(matrixStack, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        AbstractGui.fill(
                matrixStack,
                x, y, x + width, y + height, color);
        color(1, 1, 1);
        enableBlend();
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor color) {
        drawRect(x, y, width, height, color(color));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        drawRect(x, y, width, height, color(topLeftColor), color(topRightColor), color(bottomLeftColor), color(bottomRightColor));
    }

    @Override
    public void drawRect(int x, int y, int width, int height, ReadableColor tl, ReadableColor tr, ReadableColor bl, ReadableColor br) {
        disableTexture();
        enableBlend();
        disableAlphaTest();
        blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        shadeModel(GL_SMOOTH);
        com.replaymod.gui.versions.MCVer.drawRect(x, y, width, height, tl, tr, bl, br);
        shadeModel(GL_FLAT);
        enableAlphaTest();
        enableTexture();
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return drawString(x, y, color, text, false);
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text) {
        return drawString(x, y, color(color), text);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return drawCenteredString(x, y, color, text, false);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return drawCenteredString(x, y, color(color), text);
    }

    @Override
    public int drawString(int x, int y, int color, String text, boolean shadow) {
        FontRenderer fontRenderer = com.replaymod.gui.versions.MCVer.getFontRenderer();
        try {
            if (shadow) {
                return fontRenderer.drawStringWithShadow(
                        matrixStack,
                        text, x, y, color);
            } else {
                return fontRenderer.drawString(
                        matrixStack,
                        text, x, y, color);
            }
        } finally {
            color(1, 1, 1);
        }
    }

    @Override
    public int drawString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawString(x, y, color(color), text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        FontRenderer fontRenderer = com.replaymod.gui.versions.MCVer.getFontRenderer();
        x -= fontRenderer.getStringWidth(text) / 2;
        return drawString(x, y, color, text, shadow);
    }

    @Override
    public int drawCenteredString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawCenteredString(x, y, color(color), text, shadow);
    }

    private int color(ReadableColor color) {
        return color.getAlpha() << 24
                | color.getRed() << 16
                | color.getGreen() << 8
                | color.getBlue();
    }

    private ReadableColor color(int color) {
        return new Color((color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff, (color >> 24) & 0xff);
    }

    private void color(int r, int g, int b) {
        GlStateManager.color4f(r, g, b, 1);
    }

    @Override
    public void invertColors(int right, int bottom, int left, int top) {
        if (left >= right || top >= bottom) return;

        color(0, 0, 1);
        disableTexture();
        enableColorLogicOp();
        logicOp(GL11.GL_OR_REVERSE);

        com.replaymod.gui.versions.MCVer.drawRect(right, bottom, left, top);

        disableColorLogicOp();
        enableTexture();
        color(1, 1, 1);
    }
}
