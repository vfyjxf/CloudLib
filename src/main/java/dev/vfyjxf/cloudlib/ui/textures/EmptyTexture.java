package dev.vfyjxf.cloudlib.ui.textures;

import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import net.minecraft.client.gui.GuiGraphics;

public class EmptyTexture implements RenderableTexture {

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset) {
        //NOOP
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int width, int height, int maskTop, int maskBottom, int maskLeft, int maskRight) {
        //NOOP
    }
}
