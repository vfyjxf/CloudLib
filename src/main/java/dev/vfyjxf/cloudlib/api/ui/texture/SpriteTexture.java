package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Sprite texture from a texture atlas.
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

    @Override
    public ResourceLocation textureLocation() {
        return sprite().atlasLocation();
    }

    @Override
    public float[] uvCoordinates() {
        TextureAtlasSprite s = sprite();
        return new float[]{s.getU0(), s.getV0(), s.getU1(), s.getV1()};
    }
}
