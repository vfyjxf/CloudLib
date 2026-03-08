package dev.vfyjxf.cloudlib.api.ui.dump;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * High-level utility for capturing rendered CloudLib Scenes to image files.
 * <p>
 * This wraps {@link SceneCapture} and handles Scene-specific rendering lifecycle
 * (tick, layout, render, flush).
 * <p>
 * Usage:
 * <pre>{@code
 * Scene scene = ...; // build your scene
 * UICapture.renderToFile(scene, 320, 240, Path.of("screenshots/test.png"));
 * }</pre>
 */
public final class UICapture {

    private static final Logger LOGGER = LoggerFactory.getLogger("CloudLib UICapture");

    private UICapture() {
    }

    /**
     * Renders a Scene into an off-screen FBO and returns the captured image.
     * <p>
     * Replicates the full GUI rendering setup that Minecraft's {@code GameRenderer}
     * performs before drawing overlays:
     * <ol>
     *   <li>FBO created at {@code logicalSize × guiScale} pixels for crisp output</li>
     *   <li>Orthographic projection matching logical dimensions</li>
     *   <li>ModelView matrix with depth translation (same as MC's GUI pipeline)</li>
     *   <li>3D item lighting via {@link Lighting#setupFor3DItems()}</li>
     * </ol>
     *
     * @param scene       the Scene to render
     * @param width       logical width (Scene coordinate space)
     * @param height      logical height (Scene coordinate space)
     * @param mouseX      simulated mouse X position
     * @param mouseY      simulated mouse Y position
     * @param partialTick partial tick value (typically 0 or 1)
     * @return NativeImage with the rendered result. Caller must close it.
     */
    public static NativeImage render(Scene scene, int width, int height, int mouseX, int mouseY, float partialTick) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int pixelW = (int) (width * guiScale);
        int pixelH = (int) (height * guiScale);

        try (SceneCapture capture = SceneCapture.create(pixelW, pixelH)) {
            capture.beginCapture();

            // --- Replicate GameRenderer's GUI render state setup ---

            // 1. Save and set orthographic projection for our logical dimensions
            Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            float farPlane = ClientHooks.getGuiFarPlane();
            Matrix4f ortho = new Matrix4f().setOrtho(
                    0.0f, (float) width, (float) height, 0.0f, 1000.0f, farPlane
            );
            RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);

            // 2. Set ModelView matrix with depth translation.
            //    MC's shaders use RenderSystem.getModelViewMatrix() as a uniform;
            //    without resetting it, leftover state from world rendering
            //    (perspective, rotation) corrupts text/item vertex positions.
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.identity();
            modelViewStack.translation(0.0f, 0.0f, 10000.0f - farPlane);
            RenderSystem.applyModelViewMatrix();

            // 3. Set up lighting for 3D item rendering
            Lighting.setupFor3DItems();

            // 4. Render the scene
            GuiGraphics graphics = capture.createGraphics();
            scene.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();

            // --- Restore state ---
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorting.ORTHOGRAPHIC_Z);

            return capture.endCapture();
        }
    }

    /**
     * Renders a Scene into an off-screen FBO and returns the captured image.
     * Uses default mouse position (0,0) and partialTick (0).
     */
    public static NativeImage render(Scene scene, int width, int height) {
        return render(scene, width, height, 0, 0, 0f);
    }

    /**
     * Renders a Scene with an explicit GUI scale factor.
     * The FBO size is {@code width * guiScale} by {@code height * guiScale} pixels.
     *
     * @param scene       the Scene to render
     * @param width       logical width (Scene coordinate space)
     * @param height      logical height (Scene coordinate space)
     * @param mouseX      simulated mouse X position
     * @param mouseY      simulated mouse Y position
     * @param partialTick partial tick value
     * @param guiScale    GUI scale factor (1-4)
     * @return NativeImage with the rendered result. Caller must close it.
     */
    public static NativeImage render(Scene scene, int width, int height,
                                     int mouseX, int mouseY, float partialTick,
                                     int guiScale) {
        int pixelW = width * guiScale;
        int pixelH = height * guiScale;

        try (SceneCapture capture = SceneCapture.create(pixelW, pixelH)) {
            capture.beginCapture();

            Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            float farPlane = ClientHooks.getGuiFarPlane();
            Matrix4f ortho = new Matrix4f().setOrtho(
                    0.0f, (float) width, (float) height, 0.0f, 1000.0f, farPlane
            );
            RenderSystem.setProjectionMatrix(ortho, VertexSorting.ORTHOGRAPHIC_Z);

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.identity();
            modelViewStack.translation(0.0f, 0.0f, 10000.0f - farPlane);
            RenderSystem.applyModelViewMatrix();

            Lighting.setupFor3DItems();

            GuiGraphics graphics = capture.createGraphics();
            scene.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();

            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorting.ORTHOGRAPHIC_Z);

            return capture.endCapture();
        }
    }

    /**
     * Renders a Scene and writes the result to a PNG file.
     *
     * @param scene       the Scene to render
     * @param width       capture width in pixels
     * @param height      capture height in pixels
     * @param mouseX      simulated mouse X position
     * @param mouseY      simulated mouse Y position
     * @param partialTick partial tick value
     * @param outputPath  path to write the PNG file to
     */
    public static void renderToFile(Scene scene, int width, int height,
                                    int mouseX, int mouseY, float partialTick,
                                    Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        try (NativeImage image = render(scene, width, height, mouseX, mouseY, partialTick)) {
            image.writeToFile(outputPath);
            LOGGER.info("Captured Scene {}x{} to {}", width, height, outputPath);
        }
    }

    /**
     * Renders a Scene and writes the result to a PNG file with an explicit GUI scale.
     */
    public static void renderToFile(Scene scene, int width, int height,
                                    int mouseX, int mouseY, float partialTick,
                                    int guiScale, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        try (NativeImage image = render(scene, width, height, mouseX, mouseY, partialTick, guiScale)) {
            image.writeToFile(outputPath);
            LOGGER.info("Captured Scene {}x{} (scale={}) to {}", width, height, guiScale, outputPath);
        }
    }

    /**
     * Renders a Scene and writes the result to a PNG file.
     * Uses default mouse position (0,0) and partialTick (0).
     */
    public static void renderToFile(Scene scene, int width, int height, Path outputPath) throws IOException {
        renderToFile(scene, width, height, 0, 0, 0f, outputPath);
    }

    /**
     * Renders arbitrary GUI content and writes the result to a PNG file.
     *
     * @param renderAction rendering callback that receives GuiGraphics
     * @param width        capture width in pixels
     * @param height       capture height in pixels
     * @param outputPath   path to write the PNG file to
     */
    public static void renderToFile(SceneCapture.RenderAction renderAction, int width, int height,
                                    Path outputPath) throws IOException {
        try (SceneCapture capture = SceneCapture.create(width, height)) {
            capture.captureAndSave(renderAction, outputPath);
        }
    }

    /**
     * Captures the raw pixel data from rendering a Scene.
     * Useful for programmatic assertions — e.g. checking pixel colors.
     *
     * @param scene  the Scene to render
     * @param width  capture width in pixels
     * @param height capture height in pixels
     * @return a PixelData snapshot. Caller must close it when done.
     */
    public static PixelData capturePixels(Scene scene, int width, int height) {
        NativeImage image = render(scene, width, height);
        return new PixelData(image);
    }

    /**
     * Wraps a NativeImage to provide convenient pixel inspection methods.
     */
    public static final class PixelData implements AutoCloseable {

        private final NativeImage image;

        PixelData(NativeImage image) {
            this.image = image;
        }

        public int width() {
            return image.getWidth();
        }

        public int height() {
            return image.getHeight();
        }

        /**
         * Gets the raw RGBA pixel value at (x, y).
         * Note: NativeImage uses ABGR internal format, so the returned int is
         * in NativeImage's encoding (ABGR). Use the extraction helpers below.
         */
        public int getPixelRGBA(int x, int y) {
            return image.getPixelRGBA(x, y);
        }

        /**
         * Extracts the red component (0-255) from a pixel at (x, y).
         */
        public int red(int x, int y) {
            int pixel = image.getPixelRGBA(x, y);
            return pixel & 0xFF;
        }

        /**
         * Extracts the green component (0-255) from a pixel at (x, y).
         */
        public int green(int x, int y) {
            int pixel = image.getPixelRGBA(x, y);
            return (pixel >> 8) & 0xFF;
        }

        /**
         * Extracts the blue component (0-255) from a pixel at (x, y).
         */
        public int blue(int x, int y) {
            int pixel = image.getPixelRGBA(x, y);
            return (pixel >> 16) & 0xFF;
        }

        /**
         * Extracts the alpha component (0-255) from a pixel at (x, y).
         */
        public int alpha(int x, int y) {
            int pixel = image.getPixelRGBA(x, y);
            return (pixel >> 24) & 0xFF;
        }

        /**
         * Checks if the pixel at (x, y) is transparent (alpha == 0).
         */
        public boolean isTransparent(int x, int y) {
            return alpha(x, y) == 0;
        }

        /**
         * Checks if the pixel at (x, y) has the expected color (ignoring alpha).
         *
         * @param x expected x
         * @param y expected y
         * @param r expected red (0-255)
         * @param g expected green (0-255)
         * @param b expected blue (0-255)
         */
        public boolean isColor(int x, int y, int r, int g, int b) {
            return red(x, y) == r && green(x, y) == g && blue(x, y) == b;
        }

        /**
         * Saves the captured image to a file.
         */
        public void writeToFile(Path path) throws IOException {
            Files.createDirectories(path.getParent());
            image.writeToFile(path);
        }

        /**
         * Returns the underlying NativeImage.
         */
        public NativeImage image() {
            return image;
        }

        @Override
        public void close() {
            image.close();
        }
    }
}
