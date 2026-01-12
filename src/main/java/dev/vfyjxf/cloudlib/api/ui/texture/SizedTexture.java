package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A texture with intrinsic dimensions.
 */
public interface SizedTexture extends UITexture {

    int width();

    int height();

    /**
     * Renders using the intrinsic dimensions.
     */
    default void render(GuiGraphics graphics, int x, int y) {
        render(graphics, x, y, width(), height());
    }
}
