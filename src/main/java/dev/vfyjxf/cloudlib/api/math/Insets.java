package dev.vfyjxf.cloudlib.api.math;

/**
 * Immutable offsets on each side of a rectangle.
 * <p>
 * Commonly used to represent padding, margin, border widths, or overflow amounts.
 *
 * @param top    the top offset
 * @param right  the right offset
 * @param bottom the bottom offset
 * @param left   the left offset
 */
public record Insets(int top, int right, int bottom, int left) {

    public static final Insets zero = new Insets(0, 0, 0, 0);

    /**
     * Creates insets with the same value on all four sides.
     */
    public static Insets uniform(int value) {
        return new Insets(value, value, value, value);
    }

    /**
     * Creates insets with symmetric horizontal and vertical values.
     *
     * @param vertical   the top and bottom offset
     * @param horizontal the left and right offset
     */
    public static Insets symmetric(int vertical, int horizontal) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }
}
