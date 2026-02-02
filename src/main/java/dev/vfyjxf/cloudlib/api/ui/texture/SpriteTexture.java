package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.function.Supplier;

/**
 * Sprite texture from a texture atlas.
 * <p>
 * Supports batch rendering via {@link BatchableTexture}.
 */
public class SpriteTexture implements SizedTexture, BatchableTexture {

    private final Supplier<TextureAtlasSprite> spriteSupplier;
    private final int width, height;

    public SpriteTexture(Supplier<TextureAtlasSprite> spriteSupplier, int width, int height) {
        this.spriteSupplier = spriteSupplier;
        this.width = width;
        this.height = height;
    }

    public SpriteTexture(TextureAtlasSprite sprite, int width, int height) {
        this(() -> sprite, width, height);
    }

    public TextureAtlasSprite sprite() {
        return spriteSupplier.get();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        TextureAtlasSprite sprite = spriteSupplier.get();
        graphics.blit(x, y, 0, width, height, sprite);
    }

    // ==================== BatchableTexture Implementation ====================

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        TextureAtlasSprite sprite = spriteSupplier.get();
        emitter.textured(sprite.atlasLocation(), x, y, w, h,
            sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), color);
    }
}
