package dev.vfyjxf.cloudlib.api.ui;

public class Constraints {

    public static final int Infinity = Integer.MAX_VALUE;

    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;

    public static Constraints create(int width, int height) {
        return new Constraints(width, height, width, height);
    }

    public static Constraints create(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        return new Constraints(minWidth, minHeight, maxWidth, maxHeight);
    }

    public Constraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public boolean isZero() {
        return minWidth == 0 && minHeight == 0 && maxWidth == 0 && maxHeight == 0;
    }

    public Constraints copy() {
        return new Constraints(minWidth, minHeight, maxWidth, maxHeight);
    }

    public Constraints withMinWidth(int minWidth) {
        return new Constraints(minWidth, minHeight, maxWidth, maxHeight);
    }

    public Constraints withMinHeight(int minHeight) {
        return new Constraints(minWidth, minHeight, maxWidth, maxHeight);
    }

    public Constraints withMaxWidth(int maxWidth) {
        return new Constraints(minWidth, minHeight, maxWidth, maxHeight);
    }

    public Constraints withMaxHeight(int maxHeight) {
        return new Constraints(minWidth, minHeight, maxWidth, maxHeight);
    }

    public Constraints withWidth(int width) {
        return new Constraints(width, minHeight, width, maxHeight);
    }

    public Constraints withHeight(int height) {
        return new Constraints(minWidth, height, maxWidth, height);
    }

    public Constraints withSize(int size) {
        return new Constraints(size, size, size, size);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Constraints that)) return false;

        return minWidth == that.minWidth &&
                minHeight == that.minHeight &&
                maxWidth == that.maxWidth &&
                maxHeight == that.maxHeight;
    }

    @Override
    public int hashCode() {
        int result = minWidth;
        result = 31 * result + minHeight;
        result = 31 * result + maxWidth;
        result = 31 * result + maxHeight;
        return result;
    }


    @Override
    public String toString() {
        return "Constraints{" +
                "minWidth=" + minWidth +
                ", minHeight=" + minHeight +
                ", maxWidth=" + maxWidth +
                ", maxHeight=" + maxHeight +
                '}';
    }
}
