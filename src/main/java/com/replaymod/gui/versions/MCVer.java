package com.replaymod.gui.versions;

import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReportCategory;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.Callable;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static MainWindow newScaledResolution(Minecraft mc) {
        return mc.getMainWindow();
    }

    public static void addDetail(CrashReportCategory category, String name, Callable<String> callable) {
        category.addDetail(name, callable::call);
    }

    public static void drawRect(int right, int bottom, int left, int top) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        vertexBuffer.pos(right, top, 0).endVertex();
        vertexBuffer.pos(left, top, 0).endVertex();
        vertexBuffer.pos(left, bottom, 0).endVertex();
        vertexBuffer.pos(right, bottom, 0).endVertex();
        tessellator.draw();
    }

    public static void drawRect(int x, int y, int width, int height, ReadableColor tl, ReadableColor tr, ReadableColor bl, ReadableColor br) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertexBuffer.pos(x, y + height, 0).color(bl.getRed(), bl.getGreen(), bl.getBlue(), bl.getAlpha()).endVertex();
        vertexBuffer.pos(x + width, y + height, 0).color(br.getRed(), br.getGreen(), br.getBlue(), br.getAlpha()).endVertex();
        vertexBuffer.pos(x + width, y, 0).color(tr.getRed(), tr.getGreen(), tr.getBlue(), tr.getAlpha()).endVertex();
        vertexBuffer.pos(x, y, 0).color(tl.getRed(), tl.getGreen(), tl.getBlue(), tl.getAlpha()).endVertex();
        tessellator.draw();
    }

    public static FontRenderer getFontRenderer() {
        return getMinecraft().fontRenderer;
    }

    public static RenderGameOverlayEvent.ElementType getType(RenderGameOverlayEvent event) {
        return event.getType();
    }

    public static int getMouseX(GuiScreenEvent.DrawScreenEvent.Post event) {
        return event.getMouseX();
    }

    public static int getMouseY(GuiScreenEvent.DrawScreenEvent.Post event) {
        return event.getMouseY();
    }

    public static void setClipboardString(String text) {
        getMinecraft().keyboardListener.setClipboardString(text);
    }

    public static String getClipboardString() {
        return getMinecraft().keyboardListener.getClipboardString();
    }


    public static abstract class Keyboard {
        public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
        public static final int KEY_HOME = GLFW.GLFW_KEY_HOME;
        public static final int KEY_END = GLFW.GLFW_KEY_END;
        public static final int KEY_UP = GLFW.GLFW_KEY_UP;
        public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
        public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
        public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
        public static final int KEY_BACK = GLFW.GLFW_KEY_BACKSPACE;
        public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;
        public static final int KEY_RETURN = GLFW.GLFW_KEY_ENTER;
        public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
        public static final int KEY_A = GLFW.GLFW_KEY_A;
        public static final int KEY_C = GLFW.GLFW_KEY_C;
        public static final int KEY_V = GLFW.GLFW_KEY_V;
        public static final int KEY_X = GLFW.GLFW_KEY_X;

        public static void enableRepeatEvents(boolean enabled) {
            getMinecraft().keyboardListener.enableRepeatEvents(enabled);
        }
    }
}
