package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<ResourceLocation, SpriteRegion> spriteRegions = new ConcurrentHashMap<>();

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
        TextureRegion region = textureRegion();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, region.texture());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = graphics.pose().last().pose();

        int middleWidth = width - left - right;
        int middleHeight = height - top - bottom;
        int tiledMiddleWidth = w - left - right;
        int tiledMiddleHeight = h - top - bottom;

        // Four corners (fixed size, never tiled)
        addQuad(buffer, matrix, region.uMin(), region.vMin(), region.uLeft(), region.vTop(), x, y, left, top);
        addQuad(buffer, matrix, region.uRight(), region.vMin(), region.uMax(), region.vTop(), x + w - right, y, right, top);
        addQuad(buffer, matrix, region.uMin(), region.vBottom(), region.uLeft(), region.vMax(), x, y + h - bottom, left, bottom);
        addQuad(buffer, matrix, region.uRight(), region.vBottom(), region.uMax(), region.vMax(), x + w - right, y + h - bottom, right, bottom);

        boolean hasHorizontalTiling = tiledMiddleWidth > 0 && middleWidth > 0;
        if (hasHorizontalTiling && top > 0) {
            // Top edge
            addTiled(buffer, matrix, region.uLeft(), region.vMin(), region.uRight(), region.vTop(), x + left, y, tiledMiddleWidth, top, middleWidth, top);
        }
        if (hasHorizontalTiling && bottom > 0) {
            // Bottom edge
            addTiled(buffer, matrix, region.uLeft(), region.vBottom(), region.uRight(), region.vMax(), x + left, y + h - bottom, tiledMiddleWidth, bottom, middleWidth, bottom);
        }
        boolean hasVerticalTiling = tiledMiddleHeight > 0 && middleHeight > 0;
        if (hasVerticalTiling && left > 0) {
            // Left edge
            addTiled(buffer, matrix, region.uMin(), region.vTop(), region.uLeft(), region.vBottom(), x, y + top, left, tiledMiddleHeight, left, middleHeight);
        }
        if (hasVerticalTiling && right > 0) {
            // Right edge
            addTiled(buffer, matrix, region.uRight(), region.vTop(), region.uMax(), region.vBottom(), x + w - right, y + top, right, tiledMiddleHeight, right, middleHeight);
        }
        if (tiledMiddleWidth > 0 && tiledMiddleHeight > 0 && middleWidth > 0 && middleHeight > 0) {
            // Center
            addTiled(buffer, matrix, region.uLeft(), region.vTop(), region.uRight(), region.vBottom(), x + left, y + top, tiledMiddleWidth, tiledMiddleHeight, middleWidth, middleHeight);
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
        TextureRegion region = textureRegion();

        int middleWidth = width - left - right;
        int middleHeight = height - top - bottom;
        float tiledMiddleWidth = w - left - right;
        float tiledMiddleHeight = h - top - bottom;

        // Four corners
        emitter.textured(region.texture(), x, y, left, top, region.uMin(), region.vMin(), region.uLeft(), region.vTop(), color);
        emitter.textured(region.texture(), x + w - right, y, right, top, region.uRight(), region.vMin(), region.uMax(), region.vTop(), color);
        emitter.textured(region.texture(), x, y + h - bottom, left, bottom, region.uMin(), region.vBottom(), region.uLeft(), region.vMax(), color);
        emitter.textured(region.texture(), x + w - right, y + h - bottom, right, bottom, region.uRight(), region.vBottom(), region.uMax(), region.vMax(), color);

        boolean hasHorizontalTiling = tiledMiddleWidth > 0 && middleWidth > 0;
        if (hasHorizontalTiling && top > 0) {
            // Top edge
            emitTiled(emitter, region.texture(), region.uLeft(), region.vMin(), region.uRight(), region.vTop(), x + left, y, tiledMiddleWidth, top, middleWidth, top, color);
        }
        if (hasHorizontalTiling && bottom > 0) {
            // Bottom edge
            emitTiled(emitter, region.texture(), region.uLeft(), region.vBottom(), region.uRight(), region.vMax(), x + left, y + h - bottom, tiledMiddleWidth, bottom, middleWidth, bottom, color);
        }
        boolean hasVerticalTiling = tiledMiddleHeight > 0 && middleHeight > 0;
        if (hasVerticalTiling && left > 0) {
            // Left edge
            emitTiled(emitter, region.texture(), region.uMin(), region.vTop(), region.uLeft(), region.vBottom(), x, y + top, left, tiledMiddleHeight, left, middleHeight, color);
        }
        if (hasVerticalTiling && right > 0) {
            // Right edge
            emitTiled(emitter, region.texture(), region.uRight(), region.vTop(), region.uMax(), region.vBottom(), x + w - right, y + top, right, tiledMiddleHeight, right, middleHeight, color);
        }
        if (tiledMiddleWidth > 0 && tiledMiddleHeight > 0 && middleWidth > 0 && middleHeight > 0) {
            // Center
            emitTiled(emitter, region.texture(), region.uLeft(), region.vTop(), region.uRight(), region.vBottom(), x + left, y + top, tiledMiddleWidth, tiledMiddleHeight, middleWidth, middleHeight, color);
        }
    }

    private TextureRegion textureRegion() {
        ResourceLocation textureLocation;
        float uMin;
        float vMin;
        float uMax;
        float vMax;
        if (atlasSprite) {
            SpriteRegion sprite = spriteRegion(location);
            textureLocation = sprite.texture();
            uMin = sprite.uMin();
            vMin = sprite.vMin();
            uMax = sprite.uMax();
            vMax = sprite.vMax();
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
        return new TextureRegion(textureLocation, uMin, vMin, uMax, vMax, uLeft, uRight, vTop, vBottom);
    }

    private static SpriteRegion spriteRegion(ResourceLocation location) {
        GuiSpriteManager sprites = Minecraft.getInstance().getGuiSprites();
        SpriteRegion cached = spriteRegions.get(location);
        if (cached != null && cached.owner() == sprites) {
            return cached;
        }
        TextureAtlasSprite sprite = sprites.getSprite(location);
        SpriteRegion region = new SpriteRegion(
                sprites,
                sprite.atlasLocation(),
                sprite.getU0(),
                sprite.getV0(),
                sprite.getU1(),
                sprite.getV1()
        );
        spriteRegions.put(location, region);
        return region;
    }

    private record SpriteRegion(
            GuiSpriteManager owner,
            ResourceLocation texture,
            float uMin,
            float vMin,
            float uMax,
            float vMax
    ) {
    }

    private record TextureRegion(
            ResourceLocation texture,
            float uMin,
            float vMin,
            float uMax,
            float vMax,
            float uLeft,
            float uRight,
            float vTop,
            float vBottom
    ) {
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
