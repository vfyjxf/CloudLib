package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;

/**
 * FloatingMiddleware that provides data to hide the floating element when it is no longer
 * visually attached to its reference element.
 * <p>
 * Two strategies are supported:
 * <ul>
 *   <li>{@link Strategy#referenceHidden} — the reference is clipped/hidden from view</li>
 *   <li>{@link Strategy#escaped} — the floating element has escaped the boundary
 *       (e.g. scrolled out of a container)</li>
 * </ul>
 * <p>
 * The consumer should read the data and apply visibility styles accordingly.
 * <p>
 */
public final class HideMiddleware implements FloatingMiddleware {

    /**
     * The detection strategy.
     */
    public enum Strategy {
        /**
         * Check if the reference element is fully clipped / hidden.
         */
        referenceHidden,
        /**
         * Check if the floating element has escaped its boundary.
         */
        escaped
    }

    private final Strategy strategy;
    private final int padding;

    //region factory

    /**
     * Creates a hide middleware with the default strategy ({@link Strategy#referenceHidden}).
     */
    public static HideMiddleware create() {
        return new HideMiddleware(Strategy.referenceHidden, 0);
    }

    /**
     * Creates a hide middleware with the given strategy.
     *
     * @param strategy the detection strategy
     */
    public static HideMiddleware create(Strategy strategy) {
        return new HideMiddleware(strategy, 0);
    }

    /**
     * Creates a hide middleware with the given strategy and padding.
     *
     * @param strategy the detection strategy
     * @param padding  the padding for overflow detection
     */
    public static HideMiddleware create(Strategy strategy, int padding) {
        return new HideMiddleware(strategy, padding);
    }

    private HideMiddleware(Strategy strategy, int padding) {
        this.strategy = strategy;
        this.padding = padding;
    }

    //endregion

    @Override
    public String name() {
        return "hide";
    }

    @Override
    public Result run(FloatingState state) {
        Rect boundary = state.boundary();

        switch (strategy) {
            case referenceHidden -> {
                Rect reference = state.referenceRect();
                Insets refOverflow = computeReferenceOverflow(reference, boundary);
                Insets offsets = computeSideOffsets(refOverflow, reference);
                boolean hidden = isAnySideFullyClipped(offsets);

                state.putData(name(), "referenceHiddenOffsets", offsets);
                state.putData(name(), "referenceHidden", hidden);
            }
            case escaped -> {
                Rect floating = state.floatingRect();
                // Use the floating element's actual position
                Rect floatingActual = new Rect(
                    (int) Math.round(state.x()), (int) Math.round(state.y()),
                    floating.width(), floating.height()
                );
                Insets floatOverflow = computeReferenceOverflow(floatingActual, boundary);
                Insets offsets = computeSideOffsets(floatOverflow, floatingActual);
                boolean escaped = isAnySideFullyClipped(offsets);

                state.putData(name(), "escapedOffsets", offsets);
                state.putData(name(), "escaped", escaped);
            }
        }

        return Result.done();
    }

    //region helpers

    private Insets computeReferenceOverflow(Rect rect, Rect boundary) {
        int top = (boundary.y() + padding) - rect.y();
        int right = (rect.x() + rect.width()) - (boundary.right() - padding);
        int bottom = (rect.y() + rect.height()) - (boundary.bottom() - padding);
        int left = (boundary.x() + padding) - rect.x();
        return new Insets(top, right, bottom, left);
    }

    private static Insets computeSideOffsets(Insets overflow, Rect rect) {
        return new Insets(
            overflow.top() - rect.height(),
            overflow.right() - rect.width(),
            overflow.bottom() - rect.height(),
            overflow.left() - rect.width()
        );
    }

    private static boolean isAnySideFullyClipped(Insets offsets) {
        return offsets.top() >= 0 || offsets.right() >= 0 || offsets.bottom() >= 0 || offsets.left() >= 0;
    }

    //endregion
}
