package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;

/**
 * Layout constraints passed to RenderWidget during layout.
 * <p>
 * Constraints define the minimum and maximum dimensions a widget
 * can occupy. The widget chooses its size within these bounds.
 *
 * @param minWidth  the minimum width
 * @param maxWidth  the maximum width
 * @param minHeight the minimum height
 * @param maxHeight the maximum height
 */
@ApiStatus.Experimental
public record Constraints(
        double minWidth,
        double maxWidth,
        double minHeight,
        double maxHeight
) {

    /**
     * Unconstrained value - represents infinity.
     */
    public static final double UNCONSTRAINED = Double.POSITIVE_INFINITY;

    /**
     * Creates tight constraints (exact size).
     *
     * @param width  the exact width
     * @param height the exact height
     * @return tight constraints
     */
    public static Constraints tight(double width, double height) {
        return new Constraints(width, width, height, height);
    }

    /**
     * Creates loose constraints (0 to max).
     *
     * @param maxWidth  the maximum width
     * @param maxHeight the maximum height
     * @return loose constraints
     */
    public static Constraints loose(double maxWidth, double maxHeight) {
        return new Constraints(0, maxWidth, 0, maxHeight);
    }

    /**
     * Creates unbounded constraints.
     *
     * @return unbounded constraints
     */
    public static Constraints unbounded() {
        return new Constraints(0, UNCONSTRAINED, 0, UNCONSTRAINED);
    }

    /**
     * Creates constraints with only a maximum width.
     *
     * @param maxWidth the maximum width
     * @return the constraints
     */
    public static Constraints maxWidth(double maxWidth) {
        return new Constraints(0, maxWidth, 0, UNCONSTRAINED);
    }

    /**
     * Creates constraints with only a maximum height.
     *
     * @param maxHeight the maximum height
     * @return the constraints
     */
    public static Constraints maxHeight(double maxHeight) {
        return new Constraints(0, UNCONSTRAINED, 0, maxHeight);
    }

    /**
     * Checks if the width is tight (min == max).
     *
     * @return true if width is tight
     */
    public boolean hasTightWidth() {
        return minWidth == maxWidth;
    }

    /**
     * Checks if the height is tight (min == max).
     *
     * @return true if height is tight
     */
    public boolean hasTightHeight() {
        return minHeight == maxHeight;
    }

    /**
     * Checks if both dimensions are tight.
     *
     * @return true if tight
     */
    public boolean isTight() {
        return hasTightWidth() && hasTightHeight();
    }

    /**
     * Checks if the constraints are bounded (finite max values).
     *
     * @return true if bounded
     */
    public boolean isBounded() {
        return maxWidth < UNCONSTRAINED && maxHeight < UNCONSTRAINED;
    }

    /**
     * Constrains a width value to be within bounds.
     *
     * @param width the width to constrain
     * @return the constrained width
     */
    public double constrainWidth(double width) {
        return Math.min(Math.max(width, minWidth), maxWidth);
    }

    /**
     * Constrains a height value to be within bounds.
     *
     * @param height the height to constrain
     * @return the constrained height
     */
    public double constrainHeight(double height) {
        return Math.min(Math.max(height, minHeight), maxHeight);
    }

    /**
     * Creates new constraints tightened to the given size.
     *
     * @param width  the width to tighten to
     * @param height the height to tighten to
     * @return tightened constraints
     */
    public Constraints tighten(double width, double height) {
        return new Constraints(
                constrainWidth(width),
                constrainWidth(width),
                constrainHeight(height),
                constrainHeight(height)
        );
    }

    /**
     * Creates new constraints with different maximum values.
     *
     * @param maxWidth  the new maximum width
     * @param maxHeight the new maximum height
     * @return new constraints
     */
    public Constraints copyWith(double maxWidth, double maxHeight) {
        return new Constraints(minWidth, maxWidth, minHeight, maxHeight);
    }
}
