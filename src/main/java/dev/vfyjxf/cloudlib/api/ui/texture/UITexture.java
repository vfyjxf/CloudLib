package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base interface for all renderable textures.
 */
@FunctionalInterface
public interface UITexture {

    /**
     * Renders this texture at the specified position and size.
     */
    void render(GuiGraphics graphics, int x, int y, int width, int height);

    /**
     * An empty texture that renders nothing.
     */
    UITexture EMPTY = (graphics, x, y, width, height) -> {};
}
