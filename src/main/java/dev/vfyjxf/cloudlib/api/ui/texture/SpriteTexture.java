package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Sprite texture from a texture atlas.
 * <p>
 * Supports batch rendering via {@link BatchableTexture}.
 * <p>
 * This class works with {@link TextureAtlasSprite} directly. For simpler use cases
 * where you just have a sprite ResourceLocation, consider using
 * {@link ImageTexture#sprite(ResourceLocation, int, int)} instead.
 */
public class SpriteTexture implements SizedTexture, BatchableTexture {

    private final Supplier<TextureAtlasSprite> spriteSupplier;
    private int width, height; // <0 = intrinsic, resolved from the sprite on first access

    /**
     * Creates with a sprite supplier.
     *
     * @param spriteSupplier the sprite source, must return a non-null sprite on every call
     * @param width the width, or a negative value to resolve it from the sprite on first access
     * @param height the height, or a negative value to resolve it from the sprite on first access
     */
    public SpriteTexture(Supplier<TextureAtlasSprite> spriteSupplier, int width, int height) {
        this.spriteSupplier = spriteSupplier;
        this.width = width;
        this.height = height;
    }

    public SpriteTexture(TextureAtlasSprite sprite, int width, int height) {
        this(() -> sprite, width, height);
    }

    // region factory

    /**
     * Creates a sprite texture from a TextureAtlasSprite.
     */
    public static SpriteTexture of(TextureAtlasSprite sprite, int width, int height) {
        return new SpriteTexture(sprite, width, height);
    }

    /**
     * Creates a sprite texture from a supplier (useful for lazy loading).
     */
    public static SpriteTexture of(Supplier<TextureAtlasSprite> spriteSupplier, int width, int height) {
        return new SpriteTexture(spriteSupplier, width, height);
    }

    /**
     * Creates a sprite texture from a GUI sprite ResourceLocation.
     * This looks up the sprite from the GUI sprites atlas.
     */
    public static SpriteTexture fromGuiSprite(ResourceLocation spriteLocation, int width, int height) {
        return new SpriteTexture(() -> {
            var minecraft = Minecraft.getInstance();
            return minecraft.getGuiSprites().getSprite(spriteLocation);
        }, width, height);
    }

    /**
     * Creates a GUI sprite texture with intrinsic sizing — the sprite's atlas
     * dimensions are read lazily on first access.
     */
    public static SpriteTexture fromGuiSprite(ResourceLocation spriteLocation) {
        return new SpriteTexture(() -> {
            var minecraft = Minecraft.getInstance();
            return minecraft.getGuiSprites().getSprite(spriteLocation);
        }, -1, -1);
    }

    // endregion

    // region query

    /**
     * Returns the sprite from the configured supplier, which must return a non-null sprite.
     */
    public TextureAtlasSprite sprite() {
        return spriteSupplier.get();
    }

    @Override
    public int width() {
        if (width < 0) {
            width = sprite().contents().width();
        }
        return width;
    }

    @Override
    public int height() {
        if (height < 0) {
            height = sprite().contents().height();
        }
        return height;
    }

    // endregion

    // region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = spriteSupplier.get();
        graphics.blit(x, y, 0, width, height, sprite);
    }

    // endregion

    // region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        TextureAtlasSprite sprite = spriteSupplier.get();
        emitter.textured(
            sprite.atlasLocation(),
            x,
            y,
            w,
            h,
            sprite.getU0(),
            sprite.getV0(),
            sprite.getU1(),
            sprite.getV1(),
            color
        );
    }

    // endregion
}
