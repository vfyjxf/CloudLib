package dev.vfyjxf.cloudlib.api.math;

public record Size(int width, int height) {

    public static final Size POINT = new Size(0, 0);

    public boolean contains(double x, double y) {
        return x >= 0 && x < width() && y >= 0 && y < height();
    }

    public Size scale(double scale) {
        return new Size((int) (width() * scale), (int) (height() * scale));
    }
}
