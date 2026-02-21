package dev.vfyjxf.cloudlib.api.ui.floating;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static DSL entry point for creating {@link FloatingMiddleware} instances.
 * <p>
 * Example usage:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares.*;
 *
 * widget.useEffect(floating(referenceWidget, FloatingPlacement.bottom,
 *     offset(8),
 *     flip(),
 *     shift()
 * ));
 * }</pre>
 *
 * @see FloatingMiddleware
 * @see FloatingPositioning
 */
public final class FloatingMiddlewares {

    private FloatingMiddlewares() {
        throw new UnsupportedOperationException("Utility class");
    }

    //region offset

    /**
     * Creates an offset middleware with the given main axis distance.
     *
     * @param mainAxis the distance between reference and floating element
     * @see OffsetMiddleware
     */
    public static OffsetMiddleware offset(int mainAxis) {
        return OffsetMiddleware.of(mainAxis);
    }

    /**
     * Creates an offset middleware with main and cross axis values.
     *
     * @param mainAxis  the distance on the main axis
     * @param crossAxis the skidding on the cross axis
     * @see OffsetMiddleware
     */
    public static OffsetMiddleware offset(int mainAxis, int crossAxis) {
        return OffsetMiddleware.of(mainAxis, crossAxis);
    }

    /**
     * Creates an offset middleware with full axis control.
     *
     * @param mainAxis      the distance on the main axis
     * @param crossAxis     the skidding on the cross axis
     * @param alignmentAxis the alignment axis offset (overrides crossAxis for aligned placements)
     * @see OffsetMiddleware
     */
    public static OffsetMiddleware offset(int mainAxis, int crossAxis, int alignmentAxis) {
        return OffsetMiddleware.of(mainAxis, crossAxis, alignmentAxis);
    }

    //endregion

    //region flip

    /**
     * Creates a flip middleware with default settings.
     *
     * @see FlipMiddleware
     */
    public static FlipMiddleware flip() {
        return FlipMiddleware.create();
    }

    /**
     * Creates a flip middleware with the given padding.
     *
     * @param padding the padding for overflow detection
     * @see FlipMiddleware
     */
    public static FlipMiddleware flip(int padding) {
        return FlipMiddleware.create(padding);
    }

    /**
     * Creates a flip middleware with full options.
     *
     * @see FlipMiddleware#create(boolean, boolean, List, FlipMiddleware.FallbackStrategy, FlipMiddleware.FallbackAxisSideDirection, boolean, double)
     */
    public static FlipMiddleware flip(
        boolean checkMainAxis,
        boolean checkCrossAxis,
        @Nullable List<FloatingPlacement> fallbackPlacements,
        FlipMiddleware.FallbackStrategy fallbackStrategy,
        FlipMiddleware.FallbackAxisSideDirection fallbackAxisSideDirection,
        boolean flipAlignment,
        int padding
    ) {
        return FlipMiddleware.create(checkMainAxis, checkCrossAxis, fallbackPlacements, fallbackStrategy, fallbackAxisSideDirection, flipAlignment, padding);
    }

    //endregion

    //region shift

    /**
     * Creates a shift middleware with default settings.
     *
     * @see ShiftMiddleware
     */
    public static ShiftMiddleware shift() {
        return ShiftMiddleware.create();
    }

    /**
     * Creates a shift middleware with the given padding.
     *
     * @param padding the padding for overflow detection
     * @see ShiftMiddleware
     */
    public static ShiftMiddleware shift(int padding) {
        return ShiftMiddleware.create(padding);
    }

    /**
     * Creates a shift middleware with full options.
     *
     * @param checkMainAxis  whether to shift on the main axis
     * @param checkCrossAxis whether to shift on the cross axis
     * @param padding        the padding for overflow detection
     * @see ShiftMiddleware
     */
    public static ShiftMiddleware shift(boolean checkMainAxis, boolean checkCrossAxis, int padding) {
        return ShiftMiddleware.create(checkMainAxis, checkCrossAxis, padding);
    }

    //endregion

    //region arrow

    /**
     * Creates an arrow middleware.
     *
     * @param arrowWidth  the width of the arrow element
     * @param arrowHeight the height of the arrow element
     * @see ArrowMiddleware
     */
    public static ArrowMiddleware arrow(int arrowWidth, int arrowHeight) {
        return ArrowMiddleware.of(arrowWidth, arrowHeight);
    }

    /**
     * Creates an arrow middleware with padding.
     *
     * @param arrowWidth  the width of the arrow element
     * @param arrowHeight the height of the arrow element
     * @param padding     padding between the arrow and the floating element edges
     * @see ArrowMiddleware
     */
    public static ArrowMiddleware arrow(int arrowWidth, int arrowHeight, int padding) {
        return ArrowMiddleware.of(arrowWidth, arrowHeight, padding);
    }

    //endregion

    //region autoPlacement

    /**
     * Creates an auto-placement middleware with default settings.
     *
     * @see AutoPlacementMiddleware
     */
    public static AutoPlacementMiddleware autoPlacement() {
        return AutoPlacementMiddleware.create();
    }

    /**
     * Creates an auto-placement middleware with the given padding.
     *
     * @param padding the padding for overflow detection
     * @see AutoPlacementMiddleware
     */
    public static AutoPlacementMiddleware autoPlacement(int padding) {
        return AutoPlacementMiddleware.create(padding);
    }

    //endregion

    //region size

    /**
     * Creates a size middleware with the given applier.
     *
     * @param applier the callback to apply size constraints
     * @see SizeMiddleware
     */
    public static SizeMiddleware size(SizeMiddleware.SizeApplier applier) {
        return SizeMiddleware.create(applier);
    }

    /**
     * Creates a size middleware with the given applier and padding.
     *
     * @param applier the callback to apply size constraints
     * @param padding the padding for overflow detection
     * @see SizeMiddleware
     */
    public static SizeMiddleware size(SizeMiddleware.SizeApplier applier, int padding) {
        return SizeMiddleware.create(applier, padding);
    }

    //endregion

    //region hide

    /**
     * Creates a hide middleware with the default strategy.
     *
     * @see HideMiddleware
     */
    public static HideMiddleware hide() {
        return HideMiddleware.create();
    }

    /**
     * Creates a hide middleware with the given strategy.
     *
     * @param strategy the detection strategy
     * @see HideMiddleware
     */
    public static HideMiddleware hide(HideMiddleware.Strategy strategy) {
        return HideMiddleware.create(strategy);
    }

    //endregion
}
