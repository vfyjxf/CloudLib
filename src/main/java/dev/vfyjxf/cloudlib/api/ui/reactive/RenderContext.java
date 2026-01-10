package dev.vfyjxf.cloudlib.api.ui.reactive;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Context for rendering operations.
 * <p>
 * Wraps the Minecraft GuiGraphics and provides additional rendering utilities.
 */
@ApiStatus.Experimental
public class RenderContext {

    private final GuiGraphics graphics;
    private double offsetX = 0;
    private double offsetY = 0;

    public RenderContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    /**
     * Gets the underlying GuiGraphics.
     *
     * @return the GuiGraphics
     */
    public GuiGraphics getGraphics() {
        return graphics;
    }

    /**
     * Gets the current X offset.
     *
     * @return the X offset
     */
    public double getOffsetX() {
        return offsetX;
    }

    /**
     * Gets the current Y offset.
     *
     * @return the Y offset
     */
    public double getOffsetY() {
        return offsetY;
    }

    /**
     * Translates the rendering context by the given offsets.
     *
     * @param dx the X offset
     * @param dy the Y offset
     */
    public void translate(double dx, double dy) {
        this.offsetX += dx;
        this.offsetY += dy;
        graphics.pose().pushPose();
        graphics.pose().translate((float) dx, (float) dy, 0);
    }

    /**
     * Restores the previous translation.
     *
     * @param dx the X offset to restore
     * @param dy the Y offset to restore
     */
    public void restore(double dx, double dy) {
        this.offsetX -= dx;
        this.offsetY -= dy;
        graphics.pose().popPose();
    }

    /**
     * Executes a block with a translation applied.
     *
     * @param dx   the X offset
     * @param dy   the Y offset
     * @param task the task to execute
     */
    public void withTranslation(double dx, double dy, Runnable task) {
        translate(dx, dy);
        try {
            task.run();
        } finally {
            restore(dx, dy);
        }
    }

    /**
     * Fills a rectangle with the specified color.
     *
     * @param x      the X position
     * @param y      the Y position
     * @param width  the width
     * @param height the height
     * @param color  the ARGB color
     */
    public void fill(int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param text  the text to draw
     * @param x     the X position
     * @param y     the Y position
     * @param color the color
     */
    public void drawString(String text, int x, int y, int color) {
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, text, x, y, color);
    }

    /**
     * Enables scissoring (clipping) to a region.
     *
     * @param x      the X position
     * @param y      the Y position
     * @param width  the width
     * @param height the height
     */
    public void enableScissor(int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
    }

    /**
     * Disables scissoring.
     */
    public void disableScissor() {
        graphics.disableScissor();
    }
}
