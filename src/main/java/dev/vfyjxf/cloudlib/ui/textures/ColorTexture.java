package dev.vfyjxf.cloudlib.ui.textures;

import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import net.minecraft.client.gui.GuiGraphics;

public class ColorTexture implements RenderableTexture {

    private int color;

    public ColorTexture(int color) {
        this.color = color;
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset) {
        graphics.fill(xOffset, yOffset, xOffset + 1, yOffset + 1, color);
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int width, int height, int maskTop, int maskBottom, int maskLeft, int maskRight) {

    }
}
