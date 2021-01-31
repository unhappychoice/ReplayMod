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
package com.replaymod.gui.utils;

import com.replaymod.gui.versions.MCVer;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;

public class MouseUtils {
    private static final Minecraft mc = com.replaymod.gui.versions.MCVer.getMinecraft();

    public static Point getMousePos() {
        int mouseX = (int) mc.mouseHelper.getMouseX();
        int mouseY = (int) mc.mouseHelper.getMouseY();
        MainWindow mainWindow = com.replaymod.gui.versions.MCVer.newScaledResolution(mc);
        mouseX = (int) Math.round((double) mouseX * mainWindow.getScaledWidth() / mainWindow.getWidth());
        mouseY = (int) Math.round((double) mouseY * mainWindow.getScaledHeight() / mainWindow.getHeight());

        return new Point(mouseX, mouseY);
    }

    public static Point getScaledDimensions() {
        MainWindow
                res = MCVer.newScaledResolution(mc);
        return new Point(res.getScaledWidth(), res.getScaledHeight());
    }
}
