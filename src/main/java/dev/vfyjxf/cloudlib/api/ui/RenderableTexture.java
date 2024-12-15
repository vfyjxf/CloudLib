package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.client.gui.GuiGraphics;

//TODO:refactor texture interface
public interface RenderableTexture {

    default void render(GuiGraphics graphics) {
        render(graphics, 0, 0);
    }

    void render(GuiGraphics graphics, int xOffset, int yOffset);

    default void render(GuiGraphics graphics, int xOffset, int yOffset, int width, int height) {
        render(graphics, xOffset, yOffset);
    }

    void render(GuiGraphics graphics, int xOffset, int yOffset, int width, int height, int maskTop, int maskBottom, int maskLeft, int maskRight);

}
