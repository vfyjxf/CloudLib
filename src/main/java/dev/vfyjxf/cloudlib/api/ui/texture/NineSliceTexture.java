package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
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
 * Corners maintain their original size while the edges and the center fill the span
 * between them — either stretched in one piece ({@link Wrap#stretch}) or repeated
 * ({@link Wrap#repeat}), see {@link #wrap}.
 * <p>
 * Can operate in two modes:
 * <ul>
 *   <li>Standard mode - renders from a standalone texture file, either whole or a
 *       {@link #region region} of it ({@code u,v} + the sheet's size)</li>
 *   <li>Atlas sprite mode - renders from GUI sprite atlas using blitSprite</li>
 * </ul>
 * <p>
 * {@code textureWidth}/{@code textureHeight} are the sheet's pixel size, needed to
 * normalize the region's {@code (u,v)}. {@link #region} leaves them at {@code 0},
 * which resolves them from the file itself on first draw ({@link TextureFileSize}).
 */
public record NineSliceTexture(
    ResourceLocation location,
    int u,
    int v,
    int width,
    int height,
    int textureWidth,
    int textureHeight,
    int left,
    int right,
    int top,
    int bottom,
    boolean atlasSprite,
    Wrap wrap
) implements SizedTexture, BatchableTexture {

    /**
     * How the edges and the center fill the span left between two corners. Mirrors
     * LDLib2's {@code SpriteTexture.WrapMode} — {@link #stretch} is its {@code CLAMP}
     * default, {@link #repeat} its {@code REPEAT}.
     */
    public enum Wrap {
        /** One quad per region, stretched across the whole span it must fill. */
        stretch,
        /** The region's pixels repeat across the span, the last tile clipped. */
        repeat
    }

    private static final Map<ResourceLocation, SpriteRegion> spriteRegions = new ConcurrentHashMap<>();

    /**
     * Creates a nine-slice texture with a uniform border (standard mode).
     */
    public NineSliceTexture(ResourceLocation location, int width, int height, int border) {
        this(location, 0, 0, width, height, width, height, border, border, border, border, false, Wrap.repeat);
    }

    /**
     * Creates a nine-slice texture with custom border sizes (standard mode).
     */
    public NineSliceTexture(
        ResourceLocation location,
        int width,
        int height,
        int left,
        int right,
        int top,
        int bottom
    ) {
        this(location, 0, 0, width, height, width, height, left, right, top, bottom, false, Wrap.repeat);
    }

    /**
     * The wrap-less signature — the edges and the center repeat, so callers written
     * before {@link Wrap} existed keep the tiling they were drawn with.
     */
    public NineSliceTexture(
        ResourceLocation location,
        int u,
        int v,
        int width,
        int height,
        int textureWidth,
        int textureHeight,
        int left,
        int right,
        int top,
        int bottom,
        boolean atlasSprite
    ) {
        this(
            location,
            u,
            v,
            width,
            height,
            textureWidth,
            textureHeight,
            left,
            right,
            top,
            bottom,
            atlasSprite,
            Wrap.repeat
        );
    }

    public NineSliceTexture {}

    // region factory

    /**
     * Creates a standard nine-slice texture with uniform border.
     */
    public static NineSliceTexture of(ResourceLocation location, int width, int height, int border) {
        return new NineSliceTexture(location, width, height, border);
    }

    /**
     * Creates a standard nine-slice texture with custom borders.
     */
    public static NineSliceTexture of(
        ResourceLocation location,
        int width,
        int height,
        int left,
        int right,
        int top,
        int bottom
    ) {
        return new NineSliceTexture(location, width, height, left, right, top, bottom);
    }

    /**
     * Creates a nine-slice texture from a region of a texture file — {@code (u,v)} is
     * the region's top-left corner in sheet pixels, {@code width}/{@code height} its
     * size. The sheet's own size is read from the file on first draw
     * ({@link TextureFileSize}), so sheet definitions state the sprite's pixels only.
     * <p>
     * The sprite repeats; {@link #withWrap} switches it to {@link Wrap#stretch}.
     *
     * @param location texture resource location
     * @param u        region's left edge in sheet pixels
     * @param v        region's top edge in sheet pixels
     * @param width    region width in pixels
     * @param height   region height in pixels
     * @param left     left border width
     * @param right    right border width
     * @param top      top border height
     * @param bottom   bottom border height
     */
    public static NineSliceTexture region(
        ResourceLocation location,
        int u,
        int v,
        int width,
        int height,
        int left,
        int right,
        int top,
        int bottom
    ) {
        return new NineSliceTexture(location, u, v, width, height, 0, 0, left, right, top, bottom, false, Wrap.repeat);
    }

    /**
     * Creates an atlas sprite nine-slice texture with uniform border.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static NineSliceTexture sprite(ResourceLocation spriteLocation, int width, int height, int border) {
        return new NineSliceTexture(spriteLocation, 0, 0, width, height, 0, 0, border, border, border, border, true);
    }

    /**
     * Creates an atlas sprite nine-slice texture with custom borders.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static NineSliceTexture sprite(
        ResourceLocation spriteLocation,
        int width,
        int height,
        int left,
        int right,
        int top,
        int bottom
    ) {
        return new NineSliceTexture(spriteLocation, 0, 0, width, height, 0, 0, left, right, top, bottom, true);
    }

    // endregion

    // region rendering

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
        addQuad(
            buffer,
            matrix,
            region.uRight(),
            region.vMin(),
            region.uMax(),
            region.vTop(),
            x + w - right,
            y,
            right,
            top
        );
        addQuad(
            buffer,
            matrix,
            region.uMin(),
            region.vBottom(),
            region.uLeft(),
            region.vMax(),
            x,
            y + h - bottom,
            left,
            bottom
        );
        addQuad(
            buffer,
            matrix,
            region.uRight(),
            region.vBottom(),
            region.uMax(),
            region.vMax(),
            x + w - right,
            y + h - bottom,
            right,
            bottom
        );

        boolean hasHorizontalSpan = tiledMiddleWidth > 0 && middleWidth > 0;
        if (hasHorizontalSpan && top > 0) {
            // Top edge
            addSpan(
                buffer,
                matrix,
                region.uLeft(),
                region.vMin(),
                region.uRight(),
                region.vTop(),
                x + left,
                y,
                tiledMiddleWidth,
                top,
                middleWidth,
                top
            );
        }
        if (hasHorizontalSpan && bottom > 0) {
            // Bottom edge
            addSpan(
                buffer,
                matrix,
                region.uLeft(),
                region.vBottom(),
                region.uRight(),
                region.vMax(),
                x + left,
                y + h - bottom,
                tiledMiddleWidth,
                bottom,
                middleWidth,
                bottom
            );
        }
        boolean hasVerticalSpan = tiledMiddleHeight > 0 && middleHeight > 0;
        if (hasVerticalSpan && left > 0) {
            // Left edge
            addSpan(
                buffer,
                matrix,
                region.uMin(),
                region.vTop(),
                region.uLeft(),
                region.vBottom(),
                x,
                y + top,
                left,
                tiledMiddleHeight,
                left,
                middleHeight
            );
        }
        if (hasVerticalSpan && right > 0) {
            // Right edge
            addSpan(
                buffer,
                matrix,
                region.uRight(),
                region.vTop(),
                region.uMax(),
                region.vBottom(),
                x + w - right,
                y + top,
                right,
                tiledMiddleHeight,
                right,
                middleHeight
            );
        }
        if (tiledMiddleWidth > 0 && tiledMiddleHeight > 0 && middleWidth > 0 && middleHeight > 0) {
            // Center
            addSpan(
                buffer,
                matrix,
                region.uLeft(),
                region.vTop(),
                region.uRight(),
                region.vBottom(),
                x + left,
                y + top,
                tiledMiddleWidth,
                tiledMiddleHeight,
                middleWidth,
                middleHeight
            );
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /**
     * Fills the span between two corners with the {@code (uMin,vMin)-(uMax,vMax)}
     * region of the source sheet — a single stretched quad under
     * {@link Wrap#stretch}, repeated source-sized quads under {@link Wrap#repeat}.
     */
    private void addSpan(
        BufferBuilder buffer,
        Matrix4f matrix,
        float uMin,
        float vMin,
        float uMax,
        float vMax,
        int x,
        int y,
        int w,
        int h,
        int sourceWidth,
        int sourceHeight
    ) {
        if (wrap == Wrap.stretch) {
            addQuad(buffer, matrix, uMin, vMin, uMax, vMax, x, y, w, h);
            return;
        }
        addTiled(buffer, matrix, uMin, vMin, uMax, vMax, x, y, w, h, sourceWidth, sourceHeight);
    }

    /**
     * Adds tiled quads to the buffer, repeating the texture region to fill the target area.
     */
    private static void addTiled(
        BufferBuilder buffer,
        Matrix4f matrix,
        float uMin,
        float vMin,
        float uMax,
        float vMax,
        int xOffset,
        int yOffset,
        int tiledWidth,
        int tiledHeight,
        int tileWidth,
        int tileHeight
    ) {
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
        BufferBuilder buffer,
        Matrix4f matrix,
        float uMin,
        float vMin,
        float uMax,
        float vMax,
        int x,
        int y,
        int w,
        int h
    ) {
        buffer.addVertex(matrix, x, y + h, 0).setUv(uMin, vMax);
        buffer.addVertex(matrix, x + w, y + h, 0).setUv(uMax, vMax);
        buffer.addVertex(matrix, x + w, y, 0).setUv(uMax, vMin);
        buffer.addVertex(matrix, x, y, 0).setUv(uMin, vMin);
    }

    // endregion

    // region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        TextureRegion region = textureRegion();

        int middleWidth = width - left - right;
        int middleHeight = height - top - bottom;
        float tiledMiddleWidth = w - left - right;
        float tiledMiddleHeight = h - top - bottom;

        // Four corners
        emitter.textured(
            region.texture(),
            x,
            y,
            left,
            top,
            region.uMin(),
            region.vMin(),
            region.uLeft(),
            region.vTop(),
            color
        );
        emitter.textured(
            region.texture(),
            x + w - right,
            y,
            right,
            top,
            region.uRight(),
            region.vMin(),
            region.uMax(),
            region.vTop(),
            color
        );
        emitter.textured(
            region.texture(),
            x,
            y + h - bottom,
            left,
            bottom,
            region.uMin(),
            region.vBottom(),
            region.uLeft(),
            region.vMax(),
            color
        );
        emitter.textured(
            region.texture(),
            x + w - right,
            y + h - bottom,
            right,
            bottom,
            region.uRight(),
            region.vBottom(),
            region.uMax(),
            region.vMax(),
            color
        );

        boolean hasHorizontalSpan = tiledMiddleWidth > 0 && middleWidth > 0;
        if (hasHorizontalSpan && top > 0) {
            // Top edge
            emitSpan(
                emitter,
                region.texture(),
                region.uLeft(),
                region.vMin(),
                region.uRight(),
                region.vTop(),
                x + left,
                y,
                tiledMiddleWidth,
                top,
                middleWidth,
                top,
                color
            );
        }
        if (hasHorizontalSpan && bottom > 0) {
            // Bottom edge
            emitSpan(
                emitter,
                region.texture(),
                region.uLeft(),
                region.vBottom(),
                region.uRight(),
                region.vMax(),
                x + left,
                y + h - bottom,
                tiledMiddleWidth,
                bottom,
                middleWidth,
                bottom,
                color
            );
        }
        boolean hasVerticalSpan = tiledMiddleHeight > 0 && middleHeight > 0;
        if (hasVerticalSpan && left > 0) {
            // Left edge
            emitSpan(
                emitter,
                region.texture(),
                region.uMin(),
                region.vTop(),
                region.uLeft(),
                region.vBottom(),
                x,
                y + top,
                left,
                tiledMiddleHeight,
                left,
                middleHeight,
                color
            );
        }
        if (hasVerticalSpan && right > 0) {
            // Right edge
            emitSpan(
                emitter,
                region.texture(),
                region.uRight(),
                region.vTop(),
                region.uMax(),
                region.vBottom(),
                x + w - right,
                y + top,
                right,
                tiledMiddleHeight,
                right,
                middleHeight,
                color
            );
        }
        if (tiledMiddleWidth > 0 && tiledMiddleHeight > 0 && middleWidth > 0 && middleHeight > 0) {
            // Center
            emitSpan(
                emitter,
                region.texture(),
                region.uLeft(),
                region.vTop(),
                region.uRight(),
                region.vBottom(),
                x + left,
                y + top,
                tiledMiddleWidth,
                tiledMiddleHeight,
                middleWidth,
                middleHeight,
                color
            );
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
            float sheetWidth = textureWidth > 0 ? textureWidth : TextureFileSize.of(location).width();
            float sheetHeight = textureHeight > 0 ? textureHeight : TextureFileSize.of(location).height();
            uMin = u / sheetWidth;
            vMin = v / sheetHeight;
            uMax = (u + width) / sheetWidth;
            vMax = (v + height) / sheetHeight;
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
    ) {}

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
    ) {}

    /**
     * Fills the span between two corners with the {@code (uMin,vMin)-(uMax,vMax)}
     * region of the source sheet — a single stretched quad under
     * {@link Wrap#stretch}, repeated source-sized quads under {@link Wrap#repeat}.
     */
    private void emitSpan(
        VertexEmitter emitter,
        ResourceLocation texture,
        float uMin,
        float vMin,
        float uMax,
        float vMax,
        float xOffset,
        float yOffset,
        float tiledWidth,
        float tiledHeight,
        int sourceWidth,
        int sourceHeight,
        int color
    ) {
        if (wrap == Wrap.stretch) {
            emitter.textured(texture, xOffset, yOffset, tiledWidth, tiledHeight, uMin, vMin, uMax, vMax, color);
            return;
        }
        emitTiled(
            emitter,
            texture,
            uMin,
            vMin,
            uMax,
            vMax,
            xOffset,
            yOffset,
            tiledWidth,
            tiledHeight,
            sourceWidth,
            sourceHeight,
            color
        );
    }

    /**
     * Emits tiled quads via the emitter, repeating the texture region to fill the target area.
     */
    private static void emitTiled(
        VertexEmitter emitter,
        ResourceLocation texture,
        float uMin,
        float vMin,
        float uMax,
        float vMax,
        float xOffset,
        float yOffset,
        float tiledWidth,
        float tiledHeight,
        int tileWidth,
        int tileHeight,
        int color
    ) {
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
                    emitter.textured(
                        texture,
                        tx,
                        ty + maskTop,
                        tw,
                        th,
                        uMin,
                        vMin + vOffset,
                        uMax - uOffset,
                        vMax,
                        color
                    );
                }
            }
        }
    }

    // endregion

    // region modification

    /**
     * Converts this texture to an atlas sprite texture. The region is dropped —
     * a GUI atlas sprite carries its own UV rect.
     */
    public NineSliceTexture asAtlasSprite() {
        return atlasSprite
                ? this
                : new NineSliceTexture(location, 0, 0, width, height, 0, 0, left, right, top, bottom, true, wrap);
    }

    /**
     * Converts this texture to a standard texture, keeping the region as the sheet.
     */
    public NineSliceTexture asStandardTexture() {
        return atlasSprite
                ? new NineSliceTexture(
                    location,
                    0,
                    0,
                    width,
                    height,
                    width,
                    height,
                    left,
                    right,
                    top,
                    bottom,
                    false,
                    wrap
                )
                : this;
    }

    /**
     * The same texture under another wrap mode — the region and the borders are
     * untouched. {@link Wrap#stretch} is how LDLib2's {@code CLAMP} sprites (its
     * default) fill their span, {@link Wrap#repeat} this class's historical tiling.
     */
    public NineSliceTexture withWrap(Wrap wrapMode) {
        return wrapMode == wrap
                ? this
                : new NineSliceTexture(
                    location,
                    u,
                    v,
                    width,
                    height,
                    textureWidth,
                    textureHeight,
                    left,
                    right,
                    top,
                    bottom,
                    atlasSprite,
                    wrapMode
                );
    }

    // endregion
}
