package dev.vfyjxf.cloudlib.api.math;

public class Dimension {

    public static final Dimension ZERO = new Dimension(0, 0);

    public int width;
    public int height;

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
