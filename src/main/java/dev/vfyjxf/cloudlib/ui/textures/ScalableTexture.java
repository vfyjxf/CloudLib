package dev.vfyjxf.cloudlib.ui.textures;

import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import net.minecraft.client.gui.GuiGraphics;

public class ScalableTexture implements RenderableTexture {

    private final RenderableTexture texture;
    private final int width;
    private final int height;

    public ScalableTexture(RenderableTexture texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset) {
        graphics.pose().pushPose();
        {
            graphics.pose().scale((float) width / texture.getWidth(), (float) height / texture.getHeight(), 1);
            texture.render(graphics, xOffset, yOffset);
        }
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int width, int height) {
        graphics.pose().pushPose();
        {
            graphics.pose().scale((float) width / texture.getWidth(), (float) height / texture.getHeight(), 1);
            texture.render(graphics, xOffset, yOffset, width, height);
        }
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {
        graphics.pose().pushPose();
        {
            graphics.pose().scale((float) width / texture.getWidth(), (float) height / texture.getHeight(), 1);
            texture.render(graphics, xOffset, yOffset, maskTop, maskBottom, maskLeft, maskRight);
        }
        graphics.pose().popPose();
    }
}
