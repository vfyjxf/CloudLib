package dev.vfyjxf.cloudlib.api.ui.floating;

/**
 * FloatingMiddleware that translates the floating element along the specified axes,
 * adding distance (gutter/margin) between the reference and floating elements.
 * <p>
 * It should generally be placed at the beginning of the middleware array.
 */
public final class OffsetMiddleware implements FloatingMiddleware {

    private final int mainAxis;
    private final int crossAxis;
    private final Integer alignmentAxis;

    //region factory

    /**
     * Creates an offset middleware with equal distance on the main axis.
     *
     * @param mainAxis the distance between reference and floating element
     * @return the middleware
     */
    public static OffsetMiddleware of(int mainAxis) {
        return new OffsetMiddleware(mainAxis, 0, null);
    }

    /**
     * Creates an offset middleware with main and cross axis values.
     *
     * @param mainAxis  the distance on the main axis (gutter)
     * @param crossAxis the skidding on the cross axis
     * @return the middleware
     */
    public static OffsetMiddleware of(int mainAxis, int crossAxis) {
        return new OffsetMiddleware(mainAxis, crossAxis, null);
    }

    /**
     * Creates an offset middleware with full axis control.
     *
     * @param mainAxis      the distance on the main axis
     * @param crossAxis     the skidding on the cross axis
     * @param alignmentAxis the alignment axis offset (overrides crossAxis for aligned placements)
     * @return the middleware
     */
    public static OffsetMiddleware of(int mainAxis, int crossAxis, int alignmentAxis) {
        return new OffsetMiddleware(mainAxis, crossAxis, alignmentAxis);
    }

    private OffsetMiddleware(int mainAxis, int crossAxis, Integer alignmentAxis) {
        this.mainAxis = mainAxis;
        this.crossAxis = crossAxis;
        this.alignmentAxis = alignmentAxis;
    }

    //endregion

    @Override
    public String name() {
        return "offset";
    }

    @Override
    public Result run(FloatingState state) {
        FloatingPlacement placement = state.placement();
        FloatingPlacement.Side side = placement.side();
        FloatingPlacement.Alignment alignment = placement.alignment();
        boolean isVertical = placement.sideAxis() == FloatingPlacement.Axis.y;

        // Main axis multiplier: sides on the "origin" side (top, left) move in -1 direction
        double mainAxisMulti = side.isOrigin() ? -1.0 : 1.0;
        double crossAxisMulti = 1.0;

        double effectiveCrossAxis = this.crossAxis;
        if (alignment != null && alignmentAxis != null) {
            effectiveCrossAxis = alignment == FloatingPlacement.Alignment.end
                ? -alignmentAxis
                : alignmentAxis;
        }

        double dx;
        double dy;
        if (isVertical) {
            dx = effectiveCrossAxis * crossAxisMulti;
            dy = mainAxis * mainAxisMulti;
        } else {
            dx = mainAxis * mainAxisMulti;
            dy = effectiveCrossAxis * crossAxisMulti;
        }

        state.setX(state.x() + dx);
        state.setY(state.y() + dy);

        state.putData(name(), "x", dx);
        state.putData(name(), "y", dy);
        state.putData(name(), "placement", placement);

        return Result.done();
    }
}
