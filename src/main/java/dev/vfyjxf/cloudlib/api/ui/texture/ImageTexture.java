package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Image texture - the most commonly used texture type.
 */
public class ImageTexture implements SizedTexture, BatchableTexture {

    private final ResourceLocation location;
    private final int u, v;
    private final int width, height;
    private final int textureWidth, textureHeight;
    private final float[] uv;

    public ImageTexture(ResourceLocation location, int width, int height) {
        this(location, 0, 0, width, height, width, height);
    }

    public ImageTexture(ResourceLocation location, int u, int v, int width, int height,
                        int textureWidth, int textureHeight) {
        this.location = location;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.uv = new float[]{
                (float) u / textureWidth,
                (float) v / textureHeight,
                (float) (u + width) / textureWidth,
                (float) (v + height) / textureHeight
        };
    }

    public ResourceLocation location() {
        return location;
    }

    public int u() {
        return u;
    }

    public int v() {
        return v;
    }

    public int textureWidth() {
        return textureWidth;
    }

    public int textureHeight() {
        return textureHeight;
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
    public ResourceLocation textureLocation() {
        return location;
    }

    @Override
    public float[] uvCoordinates() {
        return uv;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(location, x, y, width, height, u, v, this.width, this.height, textureWidth, textureHeight);
    }

    /**
     * Creates a sub-region texture.
     */
    public ImageTexture subTexture(int u, int v, int width, int height) {
        return new ImageTexture(location, this.u + u, this.v + v, width, height, textureWidth, textureHeight);
    }
}
