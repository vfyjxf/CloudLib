package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

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
 * <p>
 * Can operate in two modes:
 * <ul>
 *   <li>Standard mode - renders from a standalone texture file</li>
 *   <li>Atlas sprite mode - renders from GUI sprite atlas using blitSprite</li>
 * </ul>
 */
public record NineSliceTexture(
        ResourceLocation location, int width, int height,
        int left, int right, int top, int bottom,
        boolean atlasSprite
) implements SizedTexture, BatchableTexture {

    /**
     * Creates a nine-slice texture with uniform border (standard mode).
     */
    public NineSliceTexture(ResourceLocation location, int width, int height, int border) {
        this(location, width, height, border, border, border, border, false);
    }

    /**
     * Creates a nine-slice texture with custom border sizes (standard mode).
     */
    public NineSliceTexture(ResourceLocation location, int width, int height, int left, int right, int top, int bottom) {
        this(location, width, height, left, right, top, bottom, false);
    }

    //region factory

    /**
     * Creates a standard nine-slice texture with uniform border.
     */
    public static NineSliceTexture of(ResourceLocation location, int width, int height, int border) {
        return new NineSliceTexture(location, width, height, border, border, border, border, false);
    }

    /**
     * Creates a standard nine-slice texture with custom borders.
     */
    public static NineSliceTexture of(ResourceLocation location, int width, int height, int left, int right, int top, int bottom) {
        return new NineSliceTexture(location, width, height, left, right, top, bottom, false);
    }

    /**
     * Creates an atlas sprite nine-slice texture with uniform border.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static NineSliceTexture sprite(ResourceLocation spriteLocation, int width, int height, int border) {
        return new NineSliceTexture(spriteLocation, width, height, border, border, border, border, true);
    }

    /**
     * Creates an atlas sprite nine-slice texture with custom borders.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static NineSliceTexture sprite(ResourceLocation spriteLocation, int width, int height, int left, int right, int top, int bottom) {
        return new NineSliceTexture(spriteLocation, width, height, left, right, top, bottom, true);
    }

    //endregion

    /**
     * Creates a nine-slice texture with custom border sizes.
     *
     * @param location    texture resource location
     * @param width       texture width in pixels
     * @param height      texture height in pixels
     * @param left        left border width
     * @param right       right border width
     * @param top         top border height
     * @param bottom      bottom border height
     * @param atlasSprite whether this is an atlas sprite
     */
    public NineSliceTexture {
    }

    //endregion

    //region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        ResourceLocation textureLocation;
        float uMin, vMin, uMax, vMax;

        if (atlasSprite) {
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);
            textureLocation = sprite.atlasLocation();
            uMin = sprite.getU0();
            vMin = sprite.getV0();
            uMax = sprite.getU1();
            vMax = sprite.getV1();
        } else {
            textureLocation = location;
            uMin = 0;
            vMin = 0;
            uMax = 1;
            vMax = 1;
        }

        float uSize = uMax - uMin;
        float vSize = vMax - vMin;

        float uLeft = uMin + uSize * (left / (float) width);
        float uRight = uMax - uSize * (right / (float) width);
        float vTop = vMin + vSize * (top / (float) height);
        float vBottom = vMax - vSize * (bottom / (float) height);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, textureLocation);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = graphics.pose().last().pose();

        int middleWidth = width - left - right;
        int middleHeight = height - top - bottom;
        int tiledMiddleWidth = w - left - right;
        int tiledMiddleHeight = h - top - bottom;

        // Four corners (fixed size, never tiled)
        addQuad(buffer, matrix, uMin, vMin, uLeft, vTop, x, y, left, top);
        addQuad(buffer, matrix, uRight, vMin, uMax, vTop, x + w - right, y, right, top);
        addQuad(buffer, matrix, uMin, vBottom, uLeft, vMax, x, y + h - bottom, left, bottom);
        addQuad(buffer, matrix, uRight, vBottom, uMax, vMax, x + w - right, y + h - bottom, right, bottom);

        boolean hasHorizontalTiling = tiledMiddleWidth > 0 && middleWidth > 0;
        if (hasHorizontalTiling && top > 0) {
            // Top edge
            addTiled(buffer, matrix, uLeft, vMin, uRight, vTop, x + left, y, tiledMiddleWidth, top, middleWidth, top);
        }
        if (hasHorizontalTiling && bottom > 0) {
            // Bottom edge
            addTiled(buffer, matrix, uLeft, vBottom, uRight, vMax, x + left, y + h - bottom, tiledMiddleWidth, bottom, middleWidth, bottom);
        }
        boolean hasVerticalTiling = tiledMiddleHeight > 0 && middleHeight > 0;
        if (hasVerticalTiling && left > 0) {
            // Left edge
            addTiled(buffer, matrix, uMin, vTop, uLeft, vBottom, x, y + top, left, tiledMiddleHeight, left, middleHeight);
        }
        if (hasVerticalTiling && right > 0) {
            // Right edge
            addTiled(buffer, matrix, uRight, vTop, uMax, vBottom, x + w - right, y + top, right, tiledMiddleHeight, right, middleHeight);
        }
        if (tiledMiddleWidth > 0 && tiledMiddleHeight > 0 && middleWidth > 0 && middleHeight > 0) {
            // Center
            addTiled(buffer, matrix, uLeft, vTop, uRight, vBottom, x + left, y + top, tiledMiddleWidth, tiledMiddleHeight, middleWidth, middleHeight);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /**
     * Adds tiled quads to the buffer, repeating the texture region to fill the target area.
     */
    private static void addTiled(
            BufferBuilder buffer, Matrix4f matrix,
            float uMin, float vMin, float uMax, float vMax,
            int xOffset, int yOffset, int tiledWidth, int tiledHeight,
            int tileWidth, int tileHeight) {
        int xTileCount = tiledWidth / tileWidth;
        int xRemainder = tiledWidth - (xTileCount * tileWidth);
        int yTileCount = tiledHeight / tileHeight;
        int yRemainder = tiledHeight - (yTileCount * tileHeight);

        float uSize = uMax - uMin;
        float vSize = vMax - vMin;

        int yStart = yOffset + tiledHeight;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int tw = (xTile == xTileCount) ? xRemainder : tileWidth;
                int th = (yTile == yTileCount) ? yRemainder : tileHeight;
                int tx = xOffset + (xTile * tileWidth);
                int ty = yStart - ((yTile + 1) * tileHeight);
                if (tw > 0 && th > 0) {
                    int maskRight = tileWidth - tw;
                    int maskTop = tileHeight - th;
                    float uOffset = (maskRight / (float) tileWidth) * uSize;
                    float vOffset = (maskTop / (float) tileHeight) * vSize;
                    addQuad(buffer, matrix, uMin, vMin + vOffset, uMax - uOffset, vMax, tx, ty + maskTop, tw, th);
                }
            }
        }
    }

    /**
     * Adds a single textured quad to the buffer.
     */
    private static void addQuad(
            BufferBuilder buffer, Matrix4f matrix,
            float uMin, float vMin, float uMax, float vMax,
            int x, int y, int w, int h) {
        buffer.addVertex(matrix, x, y + h, 0).setUv(uMin, vMax);
        buffer.addVertex(matrix, x + w, y + h, 0).setUv(uMax, vMax);
        buffer.addVertex(matrix, x + w, y, 0).setUv(uMax, vMin);
        buffer.addVertex(matrix, x, y, 0).setUv(uMin, vMin);
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        ResourceLocation textureLocation;
        float uMin, vMin, uMax, vMax;

        if (atlasSprite) {
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);
            textureLocation = sprite.atlasLocation();
            uMin = sprite.getU0();
            vMin = sprite.getV0();
            uMax = sprite.getU1();
            vMax = sprite.getV1();
        } else {
            textureLocation = location;
            uMin = 0;
            vMin = 0;
            uMax = 1;
            vMax = 1;
        }

        float uSize = uMax - uMin;
        float vSize = vMax - vMin;

        float uLeft = uMin + uSize * (left / (float) width);
        float uRight = uMax - uSize * (right / (float) width);
        float vTop = vMin + vSize * (top / (float) height);
        float vBottom = vMax - vSize * (bottom / (float) height);

        int middleWidth = width - left - right;
        int middleHeight = height - top - bottom;
        float tiledMiddleWidth = w - left - right;
        float tiledMiddleHeight = h - top - bottom;

        // Four corners
        emitter.textured(textureLocation, x, y, left, top, uMin, vMin, uLeft, vTop, color);
        emitter.textured(textureLocation, x + w - right, y, right, top, uRight, vMin, uMax, vTop, color);
        emitter.textured(textureLocation, x, y + h - bottom, left, bottom, uMin, vBottom, uLeft, vMax, color);
        emitter.textured(textureLocation, x + w - right, y + h - bottom, right, bottom, uRight, vBottom, uMax, vMax, color);

        boolean hasHorizontalTiling = tiledMiddleWidth > 0 && middleWidth > 0;
        if (hasHorizontalTiling && top > 0) {
            // Top edge
            emitTiled(emitter, textureLocation, uLeft, vMin, uRight, vTop, x + left, y, tiledMiddleWidth, top, middleWidth, top, color);
        }
        if (hasHorizontalTiling && bottom > 0) {
            // Bottom edge
            emitTiled(emitter, textureLocation, uLeft, vBottom, uRight, vMax, x + left, y + h - bottom, tiledMiddleWidth, bottom, middleWidth, bottom, color);
        }
        boolean hasVerticalTiling = tiledMiddleHeight > 0 && middleHeight > 0;
        if (hasVerticalTiling && left > 0) {
            // Left edge
            emitTiled(emitter, textureLocation, uMin, vTop, uLeft, vBottom, x, y + top, left, tiledMiddleHeight, left, middleHeight, color);
        }
        if (hasVerticalTiling && right > 0) {
            // Right edge
            emitTiled(emitter, textureLocation, uRight, vTop, uMax, vBottom, x + w - right, y + top, right, tiledMiddleHeight, right, middleHeight, color);
        }
        if (tiledMiddleWidth > 0 && tiledMiddleHeight > 0 && middleWidth > 0 && middleHeight > 0) {
            // Center
            emitTiled(emitter, textureLocation, uLeft, vTop, uRight, vBottom, x + left, y + top, tiledMiddleWidth, tiledMiddleHeight, middleWidth, middleHeight, color);
        }
    }

    /**
     * Emits tiled quads via the emitter, repeating the texture region to fill the target area.
     */
    private static void emitTiled(
            VertexEmitter emitter, ResourceLocation texture,
            float uMin, float vMin, float uMax, float vMax,
            float xOffset, float yOffset, float tiledWidth, float tiledHeight,
            int tileWidth, int tileHeight, int color) {
        int xTileCount = (int) (tiledWidth / tileWidth);
        float xRemainder = tiledWidth - (xTileCount * tileWidth);
        int yTileCount = (int) (tiledHeight / tileHeight);
        float yRemainder = tiledHeight - (yTileCount * tileHeight);

        float uSize = uMax - uMin;
        float vSize = vMax - vMin;

        float yStart = yOffset + tiledHeight;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                float tw = (xTile == xTileCount) ? xRemainder : tileWidth;
                float th = (yTile == yTileCount) ? yRemainder : tileHeight;
                float tx = xOffset + (xTile * tileWidth);
                float ty = yStart - ((yTile + 1) * tileHeight);
                if (tw > 0 && th > 0) {
                    float maskRight = tileWidth - tw;
                    float maskTop = tileHeight - th;
                    float uOffset = (maskRight / tileWidth) * uSize;
                    float vOffset = (maskTop / tileHeight) * vSize;
                    emitter.textured(texture, tx, ty + maskTop, tw, th, uMin, vMin + vOffset, uMax - uOffset, vMax, color);
                }
            }
        }
    }

    //endregion

    //region modification

    /**
     * Converts this texture to an atlas sprite texture.
     */
    public NineSliceTexture asAtlasSprite() {
        return atlasSprite ? this : new NineSliceTexture(location, width, height, left, right, top, bottom, true);
    }

    /**
     * Converts this texture to a standard texture.
     */
    public NineSliceTexture asStandardTexture() {
        return atlasSprite ? new NineSliceTexture(location, width, height, left, right, top, bottom, false) : this;
    }

    //endregion
}
