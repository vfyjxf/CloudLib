package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/**
 * A gui-px rectangle. Unlike {@link dev.vfyjxf.cloudlib.api.math.Rect}
 * this is double precision — the solver works in continuous screen
 * coordinates and lets the renderer snap.
 */
public record GuiRect(double x, double y, double width, double height) {

    public GuiRect {
        if (!Double.isFinite(x + y + width + height) || width <= 0.0 || height <= 0.0) {
            throw new IllegalArgumentException("finite positive rectangle");
        }
    }

    public GuiVec center() {
        return new GuiVec(x + width * 0.5, y + height * 0.5);
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    /** CCW corners: top-left, top-right, bottom-right, bottom-left. */
    public List<GuiVec> polygon() {
        return List.of(
                new GuiVec(x, y),
                new GuiVec(x + width, y),
                new GuiVec(x + width, y + height),
                new GuiVec(x, y + height));
    }

    public GuiRect expanded(double amount) {
        return new GuiRect(x - amount, y - amount, width + 2.0 * amount, height + 2.0 * amount);
    }

    public boolean contains(GuiVec point) {
        return point.x() >= x && point.y() >= y && point.x() <= x + width && point.y() <= y + height;
    }
}
