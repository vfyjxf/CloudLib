package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A visual divider line widget.
 * <p>
 * Used to separate content visually. Supports horizontal and vertical orientations.
 */
public class DividerWidget extends Widget {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private Orientation orientation = Orientation.HORIZONTAL;
    private int thickness = 1;
    private VisualTexture texture = new ColorTexture(0xFFAAAAAA);

    public static DividerWidget horizontal() {
        return new DividerWidget().setOrientation(Orientation.HORIZONTAL);
    }

    public static DividerWidget vertical() {
        return new DividerWidget().setOrientation(Orientation.VERTICAL);
    }

    public static DividerWidget create() {
        return new DividerWidget();
    }

    private DividerWidget() {}

    public Orientation orientation() {
        return orientation;
    }

    public DividerWidget setOrientation(Orientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public int thickness() {
        return thickness;
    }

    public DividerWidget setThickness(int thickness) {
        this.thickness = thickness;
        return this;
    }

    public DividerWidget setTexture(VisualTexture texture) {
        this.texture = texture;
        return this;
    }

    public DividerWidget setColor(int color) {
        this.texture = new ColorTexture(color);
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int w, h;
        if (orientation == Orientation.HORIZONTAL) {
            w = width();
            h = thickness;
        } else {
            w = thickness;
            h = height();
        }

        int x = (width() - w) / 2;
        int y = (height() - h) / 2;

        texture.render(graphics, x, y, w, h);
    }
}
