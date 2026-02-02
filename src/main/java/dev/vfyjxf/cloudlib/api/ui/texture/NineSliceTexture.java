package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Nine-slice texture that scales while preserving corner regions.
 * <p>
 * The texture is divided into 9 regions:
 * <pre>
 * ┌─────┬───────────┬─────┐
 * │ TL  │    Top    │ TR  │
 * ├─────┼───────────┼─────┤
 * │Left │  Center   │Right│
 * ├─────┼───────────┼─────┤
 * │ BL  │  Bottom   │ BR  │
 * └─────┴───────────┴─────┘
 * </pre>
 * Corners maintain their original size, edges stretch in one direction,
 * and the center stretches in both directions.
 */
public record NineSliceTexture(
    ResourceLocation location, int width, int height,
    int left, int right, int top, int bottom
) implements SizedTexture, BatchableTexture {

    /**
     * Creates a nine-slice texture with uniform border.
     */
    public NineSliceTexture(ResourceLocation location, int width, int height, int border) {
        this(location, width, height, border, border, border, border);
    }

    /**
     * Creates a nine-slice texture with custom border sizes.
     *
     * @param location texture resource location
     * @param width    texture width in pixels
     * @param height   texture height in pixels
     * @param left     left border width
     * @param right    right border width
     * @param top      top border height
     * @param bottom   bottom border height
     */
    public NineSliceTexture {
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        // Fallback to individual blit calls
        int centerWidth = w - left - right;
        int centerHeight = h - top - bottom;
        int texCenterW = width - left - right;
        int texCenterH = height - top - bottom;

        // Top row
        graphics.blit(location, x, y, left, top, 0, 0, left, top, width, height);
        graphics.blit(location, x + left, y, centerWidth, top, left, 0, texCenterW, top, width, height);
        graphics.blit(location, x + w - right, y, right, top, width - right, 0, right, top, width, height);

        // Middle row
        graphics.blit(location, x, y + top, left, centerHeight, 0, top, left, texCenterH, width, height);
        graphics.blit(location, x + left, y + top, centerWidth, centerHeight, left, top, texCenterW, texCenterH, width, height);
        graphics.blit(location, x + w - right, y + top, right, centerHeight, width - right, top, right, texCenterH, width, height);

        // Bottom row
        graphics.blit(location, x, y + h - bottom, left, bottom, 0, height - bottom, left, bottom, width, height);
        graphics.blit(location, x + left, y + h - bottom, centerWidth, bottom, left, height - bottom, texCenterW, bottom, width, height);
        graphics.blit(location, x + w - right, y + h - bottom, right, bottom, width - right, height - bottom, right, bottom, width, height);
    }

    // ==================== BatchableTexture Implementation ====================

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        float centerWidth = w - left - right;
        float centerHeight = h - top - bottom;

        // UV coordinates
        float uLeft = (float) left / width;
        float uRight = (float) (width - right) / width;
        float vTop = (float) top / height;
        float vBottom = (float) (height - bottom) / height;

        // Top row
        emitter.textured(location, x, y, left, top, 0, 0, uLeft, vTop, color);
        emitter.textured(location, x + left, y, centerWidth, top, uLeft, 0, uRight, vTop, color);
        emitter.textured(location, x + w - right, y, right, top, uRight, 0, 1, vTop, color);

        // Middle row
        emitter.textured(location, x, y + top, left, centerHeight, 0, vTop, uLeft, vBottom, color);
        emitter.textured(location, x + left, y + top, centerWidth, centerHeight, uLeft, vTop, uRight, vBottom, color);
        emitter.textured(location, x + w - right, y + top, right, centerHeight, uRight, vTop, 1, vBottom, color);

        // Bottom row
        emitter.textured(location, x, y + h - bottom, left, bottom, 0, vBottom, uLeft, 1, color);
        emitter.textured(location, x + left, y + h - bottom, centerWidth, bottom, uLeft, vBottom, uRight, 1, color);
        emitter.textured(location, x + w - right, y + h - bottom, right, bottom, uRight, vBottom, 1, 1, color);
    }
}
