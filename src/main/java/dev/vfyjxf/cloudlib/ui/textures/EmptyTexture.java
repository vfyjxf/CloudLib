package dev.vfyjxf.cloudlib.ui.textures;

import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import net.minecraft.client.gui.GuiGraphics;

public class EmptyTexture implements IRenderableTexture {
    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset) {
        //NOOP
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {
        //NOOP
    }
}
