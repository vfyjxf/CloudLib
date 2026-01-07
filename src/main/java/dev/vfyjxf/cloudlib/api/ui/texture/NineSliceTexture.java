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
        // Manual nine-slice rendering using basic blit
        int centerWidth = this.width - left - right;
        int centerHeight = this.height - top - bottom;
        int targetCenterWidth = width - left - right;
        int targetCenterHeight = height - top - bottom;
        
        // Four corners (don't scale)
        // Top-left
        graphics.blit(location, x, y, 0, 0, left, top);
        // Top-right
        graphics.blit(location, x + width - right, y, this.width - right, 0, right, top);
        // Bottom-left
        graphics.blit(location, x, y + height - bottom, 0, this.height - bottom, left, bottom);
        // Bottom-right
        graphics.blit(location, x + width - right, y + height - bottom, this.width - right, this.height - bottom, right, bottom);
        
        // Four edges (scale in one direction)
        if (targetCenterWidth > 0) {
            // Top edge
            graphics.blit(location, x + left, y, targetCenterWidth, top, left, 0, centerWidth, top, this.width, this.height);
            // Bottom edge
            graphics.blit(location, x + left, y + height - bottom, targetCenterWidth, bottom, left, this.height - bottom, centerWidth, bottom, this.width, this.height);
        }
        if (targetCenterHeight > 0) {
            // Left edge
            graphics.blit(location, x, y + top, left, targetCenterHeight, 0, top, left, centerHeight, this.width, this.height);
            // Right edge
            graphics.blit(location, x + width - right, y + top, right, targetCenterHeight, this.width - right, top, right, centerHeight, this.width, this.height);
        }
        
        // Center (scale in both directions)
        if (targetCenterWidth > 0 && targetCenterHeight > 0) {
            graphics.blit(location, x + left, y + top, targetCenterWidth, targetCenterHeight, left, top, centerWidth, centerHeight, this.width, this.height);
        }
    }
}
