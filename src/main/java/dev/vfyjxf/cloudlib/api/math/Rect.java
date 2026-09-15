package dev.vfyjxf.cloudlib.api.math;

import org.jetbrains.annotations.Contract;

/**
 * Represents a rectangle.
 */
public record Rect(int x, int y, int width, int height) {

    public static final Rect empty = new Rect(0, 0, 0, 0);

    public Rect(Pos pos, Size size) {
        this(pos.x(), pos.y(), size.width(), size.height());
    }

    public Rect(Pos pos, int width, int height) {
        this(pos.x(), pos.y(), width, height);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public Pos pos() {
        return new Pos(x, y);
    }

    public Size size() {
        return new Size(width, height);
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public Pos center() {
        return new Pos(centerX(), centerY());
    }

    public Rect copy() {
        return new Rect(x, y, width, height);
    }

    public boolean contains(int x, int y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public boolean contains(Pos pos) {
        return contains(pos.x(), pos.y());
    }

    public boolean contains(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public boolean contains(Rect rect) {
        return x <= rect.x && y <= rect.y && x + width >= rect.x + rect.width && y + height >= rect.y + rect.height;
    }

    public boolean intersects(Rect rect) {
        return x < rect.x + rect.width && x + width > rect.x && y < rect.y + rect.height && y + height > rect.y;
    }

    public boolean intersects(int x, int y, int width, int height) {
        return this.x < x + width && this.x + this.width > x && this.y < y + height && this.y + this.height > y;
    }

    /**
     * Returns the intersection of this rectangle with another.
     *
     * @param other the other rectangle
     * @return the intersection, or a zero-area rect if no intersection
     */
    @Contract("_ -> new")
    public Rect intersection(Rect other) {
        int x1 = Math.max(this.x, other.x);
        int y1 = Math.max(this.y, other.y);
        int x2 = Math.min(this.right(), other.right());
        int y2 = Math.min(this.bottom(), other.bottom());
        int w = Math.max(0, x2 - x1);
        int h = Math.max(0, y2 - y1);
        return new Rect(x1, y1, w, h);
    }

    @Contract("_ -> new")
    public Rect move(Pos pos) {
        return new Rect(pos.x() + x, pos.y() + y, width, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rect rect = (Rect) o;
        return x == rect.x && y == rect.y && width == rect.width && height == rect.height;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Rect{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
