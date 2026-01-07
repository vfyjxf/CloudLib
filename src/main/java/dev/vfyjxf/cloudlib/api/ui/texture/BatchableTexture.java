package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * A texture that supports batch rendering.
 * <p>
 * Textures implementing this interface can be efficiently batched by {@link TextureBatch}.
 */
public interface BatchableTexture extends Texture {

    /**
     * Returns the texture's ResourceLocation.
     * <p>
     * For sprites, this returns the atlas location.
     */
    ResourceLocation textureLocation();

    /**
     * Returns normalized UV coordinates (0.0 to 1.0).
     *
     * @return [u0, v0, u1, v1]
     */
    float[] uvCoordinates();

    /**
     * Returns whether this texture supports batching.
     * <p>
     * Some dynamic textures may not support batching.
     */
    default boolean supportsBatching() {
        return true;
    }

    /**
     * Adds render data to the batch collector.
     *
     * @param collector the batch collector
     * @param x         screen X position
     * @param y         screen Y position
     * @param width     render width
     * @param height    render height
     * @param color     color (ARGB)
     */
    default void addToBatch(BatchCollector collector, int x, int y, int width, int height, int color) {
        float[] uv = uvCoordinates();
        collector.addQuad(textureLocation(), x, y, width, height, uv[0], uv[1], uv[2], uv[3], color);
    }

    /**
     * Batch collector interface.
     */
    interface BatchCollector {
        void addQuad(ResourceLocation texture, int x, int y, int width, int height,
                     float u0, float v0, float u1, float v1, int color);
    }

    /**
     * Creates a BatchableTexture from ImageTexture parameters.
     */
    static BatchableTexture of(ResourceLocation location, int u, int v, int regionWidth, int regionHeight,
                                int textureWidth, int textureHeight) {
        float u0 = (float) u / textureWidth;
        float v0 = (float) v / textureHeight;
        float u1 = (float) (u + regionWidth) / textureWidth;
        float v1 = (float) (v + regionHeight) / textureHeight;
        float[] uv = {u0, v0, u1, v1};

        return new BatchableTexture() {
            @Override
            public ResourceLocation textureLocation() {
                return location;
            }

            @Override
            public float[] uvCoordinates() {
                return uv;
            }

            @Override
            public void render(GuiGraphics graphics, int x, int y, int width, int height) {
                graphics.blit(location, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
            }
        };
    }

    /**
     * Creates a BatchableTexture from a TextureAtlasSprite.
     */
    static BatchableTexture of(TextureAtlasSprite sprite) {
        return new BatchableTexture() {
            @Override
            public ResourceLocation textureLocation() {
                return sprite.atlasLocation();
            }

            @Override
            public float[] uvCoordinates() {
                return new float[]{sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1()};
            }

            @Override
            public void render(GuiGraphics graphics, int x, int y, int width, int height) {
                graphics.blit(x, y, 0, width, height, sprite);
            }
        };
    }
}
