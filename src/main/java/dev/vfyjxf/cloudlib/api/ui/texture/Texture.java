package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base interface for all renderable textures.
 */
@FunctionalInterface
public interface Texture {

    /**
     * Renders this texture at the specified position and size.
     */
    void render(GuiGraphics graphics, int x, int y, int width, int height);

    /**
     * An empty texture that renders nothing.
     */
    Texture EMPTY = (graphics, x, y, width, height) -> {};
}
