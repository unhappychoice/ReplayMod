package com.replaymod.gui.versions;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * As of LWJGL 3, AWT must never be initialized, otherwise GLFW will be broken on OSX.
 * **Any** usage of BufferedImage will initialize AWT (static initializer).
 * E.g. https://www.replaymod.com/forum/thread/2566
 */
public class Image implements AutoCloseable {
    private NativeImage inner;

    public Image(int width, int height) {
        this(
                new NativeImage(NativeImage.PixelFormat.RGBA, width, height, true)
        );
    }

    public Image(NativeImage inner) {
        this.inner = inner;
    }

    public NativeImage getInner() {
        return inner;
    }

    @Override
    protected void finalize() throws Throwable {
        // Great, now we're using a language with GC but still need to take care of memory management.. thanks MC
        close();
        super.finalize();
    }

    @Override
    public void close() {
        if (inner != null) {
            inner.close();
            inner = null;
        }
    }

    public int getWidth() {
        return inner.getWidth();
    }

    public int getHeight() {
        return inner.getHeight();
    }

    public void setRGBA(int x, int y, int r, int g, int b, int a) {
        // actually takes ABGR, not RGBA
        inner.setPixelRGBA(x, y, ((a & 0xff) << 24) | ((b & 0xff) << 16) | ((g & 0xff) << 8) | (r & 0xff));
    }

    public static Image read(Path path) throws IOException {
        return read(Files.newInputStream(path));
    }

    public static Image read(InputStream in) throws IOException {
        return new Image(NativeImage.read(in));
    }

    public void writePNG(File file) throws IOException {
        inner.write(file);
    }

    public void writePNG(OutputStream outputStream) throws IOException {
        Path tmp = Files.createTempFile("tmp", ".png");
        try {
            inner.write(tmp);
            Files.copy(tmp, outputStream);
        } finally {
            Files.delete(tmp);
        }
    }

    public Image scaledSubRect(int x, int y, int width, int height, int scaledWidth, int scaledHeight) {
        NativeImage dst = new NativeImage(inner.getFormat(), scaledWidth, scaledHeight, false);
        inner.resizeSubRectTo(x, y, width, height, dst);
        return new Image(dst);
    }

    @Deprecated // BufferedImage should not be used on 1.13+, see class docs
    public BufferedImage toBufferedImage() {
        // Not very efficient but certainly the easiest solution.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writePNG(out);
            return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DynamicTexture toTexture() {
        return new DynamicTexture(inner);
    }
}
