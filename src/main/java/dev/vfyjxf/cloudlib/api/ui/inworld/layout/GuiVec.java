package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * A 2D vector in gui-scaled pixels — the coordinate space screen-space
 * panels, leader polylines and HUD regions live in.
 */
public record GuiVec(double x, double y) {

    public static final GuiVec zero = new GuiVec(0, 0);

    public GuiVec add(GuiVec other) {
        return new GuiVec(x + other.x, y + other.y);
    }

    public GuiVec sub(GuiVec other) {
        return new GuiVec(x - other.x, y - other.y);
    }

    public GuiVec mul(double scale) {
        return new GuiVec(x * scale, y * scale);
    }

    public double dot(GuiVec other) {
        return x * other.x + y * other.y;
    }

    public double cross(GuiVec other) {
        return x * other.y - y * other.x;
    }

    public double len() {
        return Math.sqrt(x * x + y * y);
    }

    public double lenSqr() {
        return x * x + y * y;
    }

    /** The unit vector, or {@link #zero} when the vector is degenerate. */
    public GuiVec unit() {
        double len = len();
        return len > 1.0E-10 ? mul(1.0 / len) : zero;
    }

    public double distance(GuiVec other) {
        return sub(other).len();
    }

    public double distanceSqr(GuiVec other) {
        return sub(other).lenSqr();
    }
}
