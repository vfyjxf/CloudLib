package dev.vfyjxf.cloudlib.api.math;

import org.jetbrains.annotations.Contract;

/**
 * A rectangle in double precision — the working geometry of the projection
 * layout, where int {@link Rect} rounding would jitter the per-frame motion
 * solver.
 */
public record FloatRect(double x, double y, double width, double height) {

    public static final FloatRect empty = new FloatRect(0, 0, 0, 0);

    public static FloatRect of(double x, double y, double width, double height) {
        return new FloatRect(x, y, width, height);
    }

    public static FloatRect around(FloatPos center, double width, double height) {
        return new FloatRect(center.x - width * 0.5, center.y - height * 0.5, width, height);
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public double centerX() {
        return x + width * 0.5;
    }

    public double centerY() {
        return y + height * 0.5;
    }

    public FloatPos center() {
        return new FloatPos(centerX(), centerY());
    }

    public double area() {
        return width * height;
    }

    public boolean intersects(FloatRect other) {
        return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
    }

    /**
     * The overlap with {@code other}; a zero-area rect when disjoint.
     */
    @Contract("_ -> new")
    public FloatRect intersection(FloatRect other) {
        double x1 = Math.max(x, other.x);
        double y1 = Math.max(y, other.y);
        double x2 = Math.min(right(), other.right());
        double y2 = Math.min(bottom(), other.bottom());
        return new FloatRect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    /**
     * Area of this rect lying outside {@code bounds}.
     */
    public double areaOutside(FloatRect bounds) {
        return area() - intersection(bounds).area();
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    @Contract("_, _ -> new")
    public FloatRect translate(double dx, double dy) {
        return new FloatRect(x + dx, y + dy, width, height);
    }

    /**
     * Scales the rect about its own center — the pop-in/out shape change.
     */
    @Contract("_ -> new")
    public FloatRect scaleAboutCenter(double s) {
        double w = width * s;
        double h = height * s;
        return new FloatRect(centerX() - w * 0.5, centerY() - h * 0.5, w, h);
    }

    /**
     * Pulls every edge inward by {@code m}; sizes clamp at zero.
     */
    @Contract("_ -> new")
    public FloatRect inset(double m) {
        return inset(m, m, m, m);
    }

    @Contract("_, _, _, _ -> new")
    public FloatRect inset(double left, double top, double right, double bottom) {
        double nx = x + left;
        double ny = y + top;
        return new FloatRect(nx, ny, Math.max(0, this.right() - right - nx), Math.max(0, this.bottom() - bottom - ny));
    }

    /**
     * The smallest shift that puts this rect inside {@code bounds}; applied as
     * a translate, edges that cannot fit stay at the near side.
     */
    @Contract("_ -> new")
    public FloatRect clampInto(FloatRect bounds) {
        double nx = width <= bounds.width() ? Math.max(bounds.x, Math.min(x, bounds.right() - width)) : bounds.x;
        double ny = height <= bounds.height() ? Math.max(bounds.y, Math.min(y, bounds.bottom() - height)) : bounds.y;
        return nx == x && ny == y ? this : new FloatRect(nx, ny, width, height);
    }

    /**
     * Integer-rounded copy for consumers working in whole pixels.
     */
    public Rect toRect() {
        return new Rect(
                (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(width), (int) Math.round(height));
    }

    @Override
    public String toString() {
        return "FloatRect{x=%.2f, y=%.2f, w=%.2f, h=%.2f}".formatted(x, y, width, height);
    }
}
