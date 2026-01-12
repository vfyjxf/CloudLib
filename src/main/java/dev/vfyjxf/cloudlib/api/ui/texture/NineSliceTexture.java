package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Nine-slice texture that scales while preserving corner regions.
 */
public class NineSliceTexture implements SizedTexture {

    private final ResourceLocation location;
    private final int width, height;
    private final int left, right, top, bottom;

    public NineSliceTexture(ResourceLocation location, int width, int height, int border) {
        this(location, width, height, border, border, border, border);
    }

    public NineSliceTexture(ResourceLocation location, int width, int height,
                            int left, int right, int top, int bottom) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public ResourceLocation location() {
        return location;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
