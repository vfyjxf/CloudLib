package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;

/**
 * FloatingMiddleware that constrains the size of the floating element so it doesn't
 * overflow the clipping boundary.
 * <p>
 * It computes {@code availableWidth} and {@code availableHeight} and passes
 * them to a user-provided {@link SizeApplier} callback.
 * <p>
 */
public final class SizeMiddleware implements FloatingMiddleware {

    private final SizeApplier applier;
    private final int padding;

    //region callback

    /**
     * Callback invoked with the available dimensions. Implementations should
     * apply constraints (e.g. maxWidth / maxHeight) to the floating element.
     */
    @FunctionalInterface
    public interface SizeApplier {
        /**
         * Called to apply size constraints.
         *
         * @param state           the current positioning state
         * @param availableWidth  maximum width before overflow
         * @param availableHeight maximum height before overflow
         */
        void apply(FloatingState state, int availableWidth, int availableHeight);
    }

    //endregion

    //region factory

    /**
     * Creates a size middleware with the given applier.
     *
     * @param applier the callback to apply size constraints
     * @return the middleware
     */
    public static SizeMiddleware create(SizeApplier applier) {
        return new SizeMiddleware(applier, 0);
    }

    /**
     * Creates a size middleware with the given applier and padding.
     *
     * @param applier the callback to apply size constraints
     * @param padding the padding for overflow detection
     * @return the middleware
     */
    public static SizeMiddleware create(SizeApplier applier, int padding) {
        return new SizeMiddleware(applier, padding);
    }

    private SizeMiddleware(SizeApplier applier, int padding) {
        this.applier = applier;
        this.padding = padding;
    }

    //endregion

    @Override
    public String name() {
        return "size";
    }

    @Override
    public Result run(FloatingState state) {
        FloatingPlacement placement = state.placement();
        Insets overflow = state.detectOverflow(padding);

        FloatingPlacement.Side side = placement.side();
        boolean isYAxis = placement.sideAxis() == FloatingPlacement.Axis.y;

        double floatingWidth = state.floatingRect().width();
        double floatingHeight = state.floatingRect().height();

        // Compute the side that constrains height and width
        FloatingPlacement.Side heightSide;
        FloatingPlacement.Side widthSide;
        if (side == FloatingPlacement.Side.top || side == FloatingPlacement.Side.bottom) {
            heightSide = side;
            FloatingPlacement.Alignment alignment = placement.alignment();
            widthSide = alignment == FloatingPlacement.Alignment.end
                    ? FloatingPlacement.Side.left
                    : FloatingPlacement.Side.right;
        } else {
            widthSide = side;
            heightSide = placement.alignment() == FloatingPlacement.Alignment.end
                    ? FloatingPlacement.Side.top
                    : FloatingPlacement.Side.bottom;
        }

        double maxClipHeight = floatingHeight - overflow.top() - overflow.bottom();
        double maxClipWidth = floatingWidth - overflow.left() - overflow.right();

        double availableHeight = Math.min(
                floatingHeight - FloatingPositioning.getSide(overflow, heightSide),
                maxClipHeight
        );
        double availableWidth = Math.min(
                floatingWidth - FloatingPositioning.getSide(overflow, widthSide),
                maxClipWidth
        );

        // Clamp to non-negative
        int width = (int) Math.max(0, availableWidth);
        int height = (int) Math.max(0, availableHeight);

        state.putData(name(), "availableWidth", width);
        state.putData(name(), "availableHeight", height);

        applier.apply(state, width, height);

        return Result.done();
    }
}
