package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

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
 */
public record ImageTexture(
        ResourceLocation location,
        int u, int v,
        int width, int height,
        int textureWidth, int textureHeight,
        boolean atlasSprite
) implements SizedTexture, BatchableTexture {

    /**
     * Creates a standard image texture.
     */
    public ImageTexture(ResourceLocation location, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        this(location, u, v, width, height, textureWidth, textureHeight, false);
    }

    /**
     * Creates a standard image texture with UV starting at (0,0).
     */
    public ImageTexture(ResourceLocation location, int width, int height) {
        this(location, 0, 0, width, height, width, height, false);
    }

    //region factory

    /**
     * Creates a standard image texture from a texture file.
     */
    public static ImageTexture of(ResourceLocation location, int width, int height) {
        return new ImageTexture(location, 0, 0, width, height, width, height, false);
    }

    /**
     * Creates a standard image texture with UV region.
     */
    public static ImageTexture of(ResourceLocation location, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        return new ImageTexture(location, u, v, width, height, textureWidth, textureHeight, false);
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
    public static ImageTexture sprite(ResourceLocation spriteLocation, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        return new ImageTexture(spriteLocation, u, v, width, height, textureWidth, textureHeight, true);
    }

    //endregion

    //region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (atlasSprite) {
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);
            graphics.blit(x, y, 0, width, height, sprite);
        } else {
            graphics.blit(location, x, y, width, height, u, v, this.width, this.height, textureWidth, textureHeight);
        }
    }

    //endregion

    //region modification

    /**
     * Creates a sub-region texture.
     */
    public ImageTexture subTexture(int u, int v, int width, int height) {
        return new ImageTexture(location, this.u + u, this.v + v, width, height, textureWidth, textureHeight, atlasSprite);
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

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        if (atlasSprite) {
            // For atlas sprites, get the actual UV coordinates from the sprite
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);
            emitter.textured(sprite.atlasLocation(), x, y, w, h,
                    sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), color);
        } else {
            float u0 = (float) this.u / textureWidth;
            float v0 = (float) this.v / textureHeight;
            float u1 = (float) (this.u + width) / textureWidth;
            float v1 = (float) (this.v + height) / textureHeight;
            emitter.textured(location, x, y, w, h, u0, v0, u1, v1, color);
        }
    }

    //endregion
}
