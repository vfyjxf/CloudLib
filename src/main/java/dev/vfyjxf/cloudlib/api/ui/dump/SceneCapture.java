package dev.vfyjxf.cloudlib.api.ui.dump;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Captures the rendering output of a UI scene into off-screen framebuffer (FBO),
 * then downloads the pixel data and writes the result to an image file.
 * <p>
 * Usage:
 * <pre>{@code
 * SceneCapture capture = SceneCapture.create(320, 240);
 * capture.beginCapture();
 * // render your scene into the GuiGraphics from capture.createGraphics()
 * NativeImage image = capture.endCapture();
 * image.writeToFile(path);
 * image.close();
 * capture.close();
 * }</pre>
 * <p>
 * Or use the convenience method:
 * <pre>{@code
 * SceneCapture.renderAndSave(scene, 320, 240, Path.of("output.png"));
 * }</pre>
 */
public final class SceneCapture implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("CloudLib SceneCapture");

    private final int width;
    private final int height;
    private final TextureTarget renderTarget;
    private boolean capturing;

    private SceneCapture(int width, int height) {
        this.width = width;
        this.height = height;
        this.renderTarget = new TextureTarget(width, height, true, true);
        this.renderTarget.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    /**
     * Creates a new SceneCapture with the specified dimensions.
     * Must be called on the render thread.
     */
    public static SceneCapture create(int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();
        return new SceneCapture(width, height);
    }

    /**
     * Gets the width of the capture surface.
     */
    public int width() {
        return width;
    }

    /**
     * Gets the height of the capture surface.
     */
    public int height() {
        return height;
    }

    /**
     * Gets the underlying render target.
     */
    public RenderTarget renderTarget() {
        return renderTarget;
    }

    /**
     * Begins capturing by binding the off-screen FBO and setting the viewport.
     * After calling this, render your scene using the GuiGraphics from {@link #createGraphics()}.
     * Finish by calling {@link #endCapture()}.
     */
    public void beginCapture() {
        RenderSystem.assertOnRenderThread();
        renderTarget.clear(Minecraft.ON_OSX);
        renderTarget.bindWrite(true);
        capturing = true;
    }

    /**
     * Creates a new GuiGraphics targeting the currently bound FBO.
     * Call this after {@link #beginCapture()}.
     */
    public GuiGraphics createGraphics() {
        Minecraft mc = Minecraft.getInstance();
        return new GuiGraphics(mc, mc.renderBuffers().bufferSource());
    }

    /**
     * Ends capturing by flushing rendering, unbinding the FBO,
     * and downloading the pixels into a NativeImage.
     *
     * @return a NativeImage containing the rendered pixels. Caller must close it.
     */
    public NativeImage endCapture() {
        RenderSystem.assertOnRenderThread();
        if (!capturing) {
            throw new IllegalStateException("Not currently capturing - call beginCapture() first");
        }
        capturing = false;

        // Unbind the FBO, restore MC's main render target
        renderTarget.unbindWrite();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);

        // Download pixel data from the FBO's color texture
        NativeImage image = new NativeImage(width, height, false);
        RenderSystem.bindTexture(renderTarget.getColorTextureId());
        image.downloadTexture(0, false);

        // Flip vertically - OpenGL renders bottom-up but images are top-down
        flipVertically(image);

        return image;
    }

    /**
     * Convenience: begins capture, renders using the provided callback, and returns the captured image.
     *
     * @param renderAction a callback that receives GuiGraphics to render into
     * @return a NativeImage containing the rendered pixels. Caller must close it.
     */
    public NativeImage capture(RenderAction renderAction) {
        beginCapture();
        GuiGraphics graphics = createGraphics();
        renderAction.render(graphics);
        graphics.flush();
        return endCapture();
    }

    /**
     * Convenience: capture and save to file in one call.
     *
     * @param renderAction the rendering callback
     * @param outputPath   path to write the PNG file to
     */
    public void captureAndSave(RenderAction renderAction, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        try (NativeImage image = capture(renderAction)) {
            image.writeToFile(outputPath);
            LOGGER.info("Captured {}x{} image to {}", width, height, outputPath);
        }
    }

    @Override
    public void close() {
        renderTarget.destroyBuffers();
    }

    /**
     * Flips the image vertically in-place.
     * OpenGL framebuffers are bottom-up, but PNG files are top-down.
     */
    private static void flipVertically(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        for (int y = 0; y < h / 2; y++) {
            int oppositeY = h - 1 - y;
            for (int x = 0; x < w; x++) {
                int top = image.getPixelRGBA(x, y);
                int bottom = image.getPixelRGBA(x, oppositeY);
                image.setPixelRGBA(x, y, bottom);
                image.setPixelRGBA(x, oppositeY, top);
            }
        }
    }

    /**
     * Functional interface for rendering actions.
     */
    @FunctionalInterface
    public interface RenderAction {
        void render(GuiGraphics graphics);
    }
}
