package dev.vfyjxf.cloudlib.api.ui.texture;

import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Solid color texture that supports batch rendering.
 * <p>
 * ColorTexture implements {@link BatchableTexture} using the {@link VertexEmitter#colored} method,
 * allowing it to be batched efficiently with other fill operations.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // As a VisualTexture
 * ColorTexture red = new ColorTexture(0xFFFF0000);
 * red.render(graphics, x, y, width, height);
 *
 * // With SceneCanvas (automatically batched)
 * canvas.texture(new ColorTexture(0xFF000000), x, y, w, h);
 *
 * // Or directly use fill
 * canvas.fill(x, y, w, h, 0xFF000000);
 * }</pre>
 *
 * @see SceneCanvas#texture(VisualTexture, int, int, int, int)
 * @see SceneCanvas#fill(int, int, int, int, int)
 */
public record ColorTexture(int color) implements BatchableTexture {

    //region factory

    /**
     * Creates a ColorTexture from RGB components (alpha = 255).
     */
    public ColorTexture(int r, int g, int b) {
        this(0xFF000000 | (r << 16) | (g << 8) | b);
    }

    /**
     * Creates a ColorTexture from ARGB components.
     */
    public ColorTexture(int a, int r, int g, int b) {
        this((a << 24) | (r << 16) | (g << 8) | b);
    }

    //endregion

    //region modification

    /**
     * Creates a new ColorTexture with a different color.
     */
    public ColorTexture withColor(int argb) {
        return new ColorTexture(argb);
    }

    /**
     * Creates a new ColorTexture with modified alpha.
     */
    public ColorTexture withAlpha(int alpha) {
        return new ColorTexture((alpha << 24) | (color & 0x00FFFFFF));
    }

    //endregion

    //region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tintColor) {
        // Use our own color, ignore tint (or could blend if desired)
        emitter.colored(x, y, width, height, color);
    }

    //endregion

    //region color utilities

    /**
     * Extracts the alpha component (0-255).
     */
    public int alpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * Extracts the red component (0-255).
     */
    public int red() {
        return (color >> 16) & 0xFF;
    }

    /**
     * Extracts the green component (0-255).
     */
    public int green() {
        return (color >> 8) & 0xFF;
    }

    /**
     * Extracts the blue component (0-255).
     */
    public int blue() {
        return color & 0xFF;
    }

    //endregion
}
