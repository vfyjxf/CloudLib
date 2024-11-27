package dev.vfyjxf.cloudlib.api.math;

/**
 * Represents a position in 2D space.
 */
public class Pos {

    public static final Pos ZERO = new Pos(0, 0);

    public int x;
    public int y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Pos copy() {
        return new Pos(x, y);
    }

    public void translate(int x, int y) {
        this.x += x;
        this.y += y;
    }
}
