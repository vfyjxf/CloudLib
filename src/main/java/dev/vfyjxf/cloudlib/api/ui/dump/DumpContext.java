package dev.vfyjxf.cloudlib.api.ui.dump;

import com.mojang.blaze3d.platform.NativeImage;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Context provided to {@link UIDumpTest} methods, carrying the dump configuration
 * and providing convenience methods for capturing Scenes to image files.
 */
public final class DumpContext {

    private final String name;
    private final int width;
    private final int height;
    private final Path outputDir;

    DumpContext(String name, int width, int height, Path outputDir) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.outputDir = outputDir;
    }

    /**
     * The dump test name from {@link UIDumpTest#name()}.
     */
    public String name() {
        return name;
    }

    /**
     * Logical width from {@link UIDumpTest#width()}.
     */
    public int width() {
        return width;
    }

    /**
     * Logical height from {@link UIDumpTest#height()}.
     */
    public int height() {
        return height;
    }

    /**
     * The output directory for this mod's dumps.
     * Typically {@code <gameDir>/dumps/<modid>/}.
     */
    public Path outputDir() {
        return outputDir;
    }

    /**
     * Captures a Scene and writes it to {@code <outputDir>/<name>.png}.
     * Uses the configured width/height and default mouse position (0,0).
     */
    public void capture(Scene scene) throws IOException {
        capture(scene, 0, 0, 0f);
    }

    /**
     * Captures a Scene with the specified mouse position and writes it to
     * {@code <outputDir>/<name>.png}.
     */
    public void capture(Scene scene, int mouseX, int mouseY, float partialTick) throws IOException {
        Path outputFile = outputDir.resolve(name + ".png");
        UICapture.renderToFile(scene, width, height, mouseX, mouseY, partialTick, outputFile);
    }

    /**
     * Captures a Scene and returns the NativeImage without writing to file.
     * Caller must close the returned image.
     */
    public NativeImage render(Scene scene) {
        return UICapture.render(scene, width, height);
    }

    /**
     * Captures a Scene with the specified mouse position and returns the NativeImage.
     * Caller must close the returned image.
     */
    public NativeImage render(Scene scene, int mouseX, int mouseY, float partialTick) {
        return UICapture.render(scene, width, height, mouseX, mouseY, partialTick);
    }

    /**
     * Captures arbitrary GUI rendering and writes it to {@code <outputDir>/<filename>}.
     *
     * @param renderAction the rendering callback
     * @param filename     output filename (e.g. "raw_items.png")
     * @param pixelWidth   FBO width in pixels
     * @param pixelHeight  FBO height in pixels
     */
    public void captureRaw(SceneCapture.RenderAction renderAction, String filename,
                           int pixelWidth, int pixelHeight) throws IOException {
        Path outputFile = outputDir.resolve(filename);
        try (SceneCapture capture = SceneCapture.create(pixelWidth, pixelHeight)) {
            capture.captureAndSave(renderAction, outputFile);
        }
    }

    // ==================== Multi-variant support ====================

    /**
     * Consumer that receives a {@link VariantContext} for each display variant.
     */
    @FunctionalInterface
    public interface VariantConsumer {
        void accept(VariantContext variant) throws IOException;
    }

    /**
     * Iterates over the {@link DisplayVariant#standardSet() standard set} of display variants,
     * invoking the consumer for each one.
     * <p>
     * Output files are named {@code <name>_<variant>.png}.
     *
     * <pre>{@code
     * ctx.forEachVariant(variant -> {
     *     Scene scene = buildScene(variant.width(), variant.height());
     *     variant.capture(scene);
     * });
     * }</pre>
     */
    public void forEachVariant(VariantConsumer consumer) throws IOException {
        forEachVariant(DisplayVariant.standardSet(), consumer);
    }

    /**
     * Iterates over a custom list of display variants.
     */
    public void forEachVariant(List<DisplayVariant> variants, VariantConsumer consumer) throws IOException {
        for (DisplayVariant v : variants) {
            consumer.accept(new VariantContext(v, outputDir, name));
        }
    }
}
