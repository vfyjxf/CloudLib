package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Image texture - the most commonly used texture type.
 * <p>
 * Supports batch rendering via {@link BatchableTexture}.
 * <p>
 * Can operate in two modes:
 * <ul>
 *   <li>Standard mode - renders from a standalone texture file using UV coordinates</li>
 *   <li>Atlas sprite mode - renders from GUI sprite atlas using blitSprite</li>
 * </ul>
 * <p>
 * {@code textureWidth}/{@code textureHeight} are the sheet's pixel size, needed to
 * normalize {@code (u,v)}. {@link #region} leaves them at {@code 0}, which resolves
 * them from the file itself on first draw.
 */
public record ImageTexture(
    ResourceLocation location,
    int u,
    int v,
    int width,
    int height,
    int textureWidth,
    int textureHeight,
    boolean atlasSprite
) implements SizedTexture, BatchableTexture {
    private static final Map<ResourceLocation, SpriteRegion> spriteRegions = new ConcurrentHashMap<>();

    /**
     * Creates a standard image texture.
     */
    public ImageTexture(
        ResourceLocation location,
        int u,
        int v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        this(location, u, v, width, height, textureWidth, textureHeight, false);
    }

    /**
     * Creates a standard image texture with UV starting at (0,0).
     */
    public ImageTexture(ResourceLocation location, int width, int height) {
        this(location, 0, 0, width, height, width, height, false);
    }

    // region factory

    /**
     * Creates a standard image texture from a texture file.
     */
    public static ImageTexture of(ResourceLocation location, int width, int height) {
        return new ImageTexture(location, 0, 0, width, height, width, height, false);
    }

    /**
     * Creates a standard image texture with UV region.
     */
    public static ImageTexture of(
        ResourceLocation location,
        int u,
        int v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        return new ImageTexture(location, u, v, width, height, textureWidth, textureHeight, false);
    }

    /**
     * Creates a standard image texture for a region of a texture file —
     * {@code (u,v)} is the region's top-left corner in sheet pixels,
     * {@code width}/{@code height} its size. The sheet's own size is read from
     * the file on first draw ({@link TextureFileSize}).
     */
    public static ImageTexture region(ResourceLocation location, int u, int v, int width, int height) {
        return new ImageTexture(location, u, v, width, height, 0, 0, false);
    }

    /**
     * Creates an atlas sprite texture that renders using blitSprite.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static ImageTexture sprite(ResourceLocation spriteLocation, int width, int height) {
        return new ImageTexture(spriteLocation, 0, 0, width, height, width, height, true);
    }

    /**
     * Creates an atlas sprite texture with specified region.
     * Note: For atlas sprites, u/v/textureWidth/textureHeight are used only for subTexture calculations.
     */
    public static ImageTexture sprite(
        ResourceLocation spriteLocation,
        int u,
        int v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        return new ImageTexture(spriteLocation, u, v, width, height, textureWidth, textureHeight, true);
    }

    // endregion

    // region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (atlasSprite) {
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);
            graphics.blit(x, y, 0, width, height, sprite);
        } else {
            graphics.blit(location, x, y, width, height, u, v, this.width, this.height, sheetWidth(), sheetHeight());
        }
    }

    // endregion

    // region internal

    /** The sheet's pixel width — the declared one, or the file's when left at {@code 0}. */
    private int sheetWidth() {
        return textureWidth > 0 ? textureWidth : TextureFileSize.of(location).width();
    }

    /** The sheet's pixel height — the declared one, or the file's when left at {@code 0}. */
    private int sheetHeight() {
        return textureHeight > 0 ? textureHeight : TextureFileSize.of(location).height();
    }

    // endregion

    // region modification

    /**
     * Creates a sub-region texture.
     */
    public ImageTexture subTexture(int u, int v, int width, int height) {
        return new ImageTexture(
            location,
            this.u + u,
            this.v + v,
            width,
            height,
            textureWidth,
            textureHeight,
            atlasSprite
        );
    }

    /**
     * Returns whether this texture is an atlas sprite.
     */
    public boolean isAtlasSprite() {
        return atlasSprite;
    }

    /**
     * Converts this texture to an atlas sprite texture.
     */
    public ImageTexture asAtlasSprite() {
        return atlasSprite ? this : new ImageTexture(location, u, v, width, height, textureWidth, textureHeight, true);
    }

    /**
     * Converts this texture to a standard texture.
     */
    public ImageTexture asStandardTexture() {
        return atlasSprite ? new ImageTexture(location, u, v, width, height, textureWidth, textureHeight, false) : this;
    }

    // endregion

    // region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        if (atlasSprite) {
            // For atlas sprites, get the actual UV coordinates from the sprite
            SpriteRegion sprite = spriteRegion(location);
            emitter.textured(
                sprite.texture(),
                x,
                y,
                w,
                h,
                sprite.uMin(),
                sprite.vMin(),
                sprite.uMax(),
                sprite.vMax(),
                color
            );
        } else {
            float u0 = (float) this.u / sheetWidth();
            float v0 = (float) this.v / sheetHeight();
            float u1 = (float) (this.u + width) / sheetWidth();
            float v1 = (float) (this.v + height) / sheetHeight();
            emitter.textured(location, x, y, w, h, u0, v0, u1, v1, color);
        }
    }

    // endregion

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
}
