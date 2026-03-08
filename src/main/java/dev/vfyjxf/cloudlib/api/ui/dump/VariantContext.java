package dev.vfyjxf.cloudlib.api.ui.dump;

import com.mojang.blaze3d.platform.NativeImage;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Context for capturing a Scene under a specific {@link DisplayVariant}.
 * <p>
 * Created by {@link DumpContext#forEachVariant} and provides capture methods
 * that use the variant's logical dimensions and GUI scale.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ctx.forEachVariant(variant -> {
 *     Scene scene = buildScene(variant.width(), variant.height());
 *     variant.capture(scene);
 *     // output: <baseName>_<variant.name>.png
 * });
 * }</pre>
 */
public final class VariantContext {

    private final DisplayVariant variant;
    private final Path outputDir;
    private final String baseName;

    VariantContext(DisplayVariant variant, Path outputDir, String baseName) {
        this.variant = variant;
        this.outputDir = outputDir;
        this.baseName = baseName;
    }

    /**
     * The display variant being tested.
     */
    public DisplayVariant variant() {
        return variant;
    }

    /**
     * Logical width for layout (physicalWidth / guiScale).
     */
    public int width() {
        return variant.logicalWidth();
    }

    /**
     * Logical height for layout (physicalHeight / guiScale).
     */
    public int height() {
        return variant.logicalHeight();
    }

    /**
     * The GUI scale for this variant.
     */
    public int guiScale() {
        return variant.guiScale();
    }

    /**
     * Captures a Scene and writes it to {@code <outputDir>/<baseName>_<variant.name>.png}.
     */
    public void capture(Scene scene) throws IOException {
        capture(scene, 0, 0, 0f);
    }

    /**
     * Captures a Scene with the specified mouse position.
     */
    public void capture(Scene scene, int mouseX, int mouseY, float partialTick) throws IOException {
        Path outputFile = outputDir.resolve(baseName + "_" + variant.name() + ".png");
        UICapture.renderToFile(scene, variant.logicalWidth(), variant.logicalHeight(),
                mouseX, mouseY, partialTick, variant.guiScale(), outputFile);
    }

    /**
     * Captures a Scene and returns the NativeImage without writing to file.
     * Caller must close the returned image.
     */
    public NativeImage render(Scene scene) {
        return UICapture.render(scene, variant.logicalWidth(), variant.logicalHeight(),
                0, 0, 0f, variant.guiScale());
    }
}
