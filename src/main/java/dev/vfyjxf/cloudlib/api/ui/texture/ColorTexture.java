package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Solid color texture.
 */
public record ColorTexture(int color) implements VisualTexture {

    public ColorTexture(int r, int g, int b) {
        this(0xFF000000 | (r << 16) | (g << 8) | b);
    }

    public ColorTexture(int a, int r, int g, int b) {
        this((a << 24) | (r << 16) | (g << 8) | b);
    }

    public ColorTexture setColor(int argb) {
        return new ColorTexture(argb);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, color);
    }
}
