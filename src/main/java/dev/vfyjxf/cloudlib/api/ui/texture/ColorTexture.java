package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Solid color texture.
 */
public class ColorTexture implements Texture {

    private int color;

    public ColorTexture(int argb) {
        this.color = argb;
    }

    public ColorTexture(int r, int g, int b) {
        this(0xFF000000 | (r << 16) | (g << 8) | b);
    }

    public ColorTexture(int a, int r, int g, int b) {
        this((a << 24) | (r << 16) | (g << 8) | b);
    }

    public int color() {
        return color;
    }

    public ColorTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, color);
    }
}
