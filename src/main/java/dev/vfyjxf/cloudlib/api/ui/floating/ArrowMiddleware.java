package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Rect;

/**
 * FloatingMiddleware that provides positioning data for an arrow element.
 * <p>
 * The arrow should be an inner element of the floating element (e.g. a triangle/caret)
 * that visually points toward the reference element.
 * <p>
 * This middleware stores its result in middleware data under the key {@code "arrow"}.
 * The consumer reads {@code x} or {@code y} (one will be set, the other null depending
 * on side) and {@code centerOffset}.
 * <p>
 * It should be placed toward the end of the middleware array, after shift/flip.
 */
public final class ArrowMiddleware implements FloatingMiddleware {

    private final int arrowWidth;
    private final int arrowHeight;
    private final int padding;

    //region factory

    /**
     * Creates an arrow middleware.
     *
     * @param arrowWidth  the width of the arrow element
     * @param arrowHeight the height of the arrow element
     * @return the middleware
     */
    public static ArrowMiddleware of(int arrowWidth, int arrowHeight) {
        return new ArrowMiddleware(arrowWidth, arrowHeight, 0);
    }

    /**
     * Creates an arrow middleware with padding.
     *
     * @param arrowWidth  the width of the arrow element
     * @param arrowHeight the height of the arrow element
     * @param padding     padding between the arrow and the edges of the floating element
     * @return the middleware
     */
    public static ArrowMiddleware of(int arrowWidth, int arrowHeight, int padding) {
        return new ArrowMiddleware(arrowWidth, arrowHeight, padding);
    }

    private ArrowMiddleware(int arrowWidth, int arrowHeight, int padding) {
        this.arrowWidth = arrowWidth;
        this.arrowHeight = arrowHeight;
        this.padding = padding;
    }

    //endregion

    @Override
    public String name() {
        return "arrow";
    }

    @Override
    public Result run(FloatingState state) {
        FloatingPlacement placement = state.placement();
        double x = state.x();
        double y = state.y();
        Rect reference = state.referenceRect();
        Rect floating = state.floatingRect();

        FloatingPlacement.Axis alignmentAxis = placement.alignmentAxis();
        boolean isYAxis = alignmentAxis == FloatingPlacement.Axis.y;

        double arrowLength = isYAxis ? arrowHeight : arrowWidth;
        double clientSize = isYAxis ? floating.height() : floating.width();

        double refCoord = isYAxis ? reference.y() : reference.x();
        double refLength = isYAxis ? reference.height() : reference.width();
        double floatCoord = isYAxis ? y : x;

        // How far does the end of the reference extend past the floating element?
        double endDiff = refLength + refCoord - floatCoord - clientSize;
        // How far does the start of the floating element extend past the reference?
        double startDiff = floatCoord - refCoord;

        double centerToReference = endDiff / 2.0 - startDiff / 2.0;

        double largestPossiblePadding = clientSize / 2.0 - arrowLength / 2.0 - 1;
        double minPadding = Math.min(padding, largestPossiblePadding);
        double maxPadding = Math.min(padding, largestPossiblePadding);

        double min = minPadding;
        double max = clientSize - arrowLength - maxPadding;
        double center = clientSize / 2.0 - arrowLength / 2.0 + centerToReference;
        double offset = clamp(min, center, max);

        double centerOffset = center - offset;

        if (isYAxis) {
            state.putData(name(), "y", offset);
        } else {
            state.putData(name(), "x", offset);
        }
        state.putData(name(), "centerOffset", centerOffset);

        return Result.done();
    }

    private static double clamp(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
}
