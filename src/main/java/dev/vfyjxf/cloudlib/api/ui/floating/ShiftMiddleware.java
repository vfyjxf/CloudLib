package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;

/**
 * FloatingMiddleware that shifts the floating element along its axis to keep it in view.
 * <p>
 * Unlike {@link FlipMiddleware} which changes the side, shift preserves the side
 * and slides the element along the alignment axis so it stays within the boundary.
 * <p>
 */
public final class ShiftMiddleware implements FloatingMiddleware {

    private final boolean checkMainAxis;
    private final boolean checkCrossAxis;
    private final int padding;

    //region factory

    /**
     * Creates a shift middleware with default settings (main axis only).
     */
    public static ShiftMiddleware create() {
        return new ShiftMiddleware(true, false, 0);
    }

    /**
     * Creates a shift middleware with the given padding.
     *
     * @param padding the padding for overflow detection
     */
    public static ShiftMiddleware create(int padding) {
        return new ShiftMiddleware(true, false, padding);
    }

    /**
     * Creates a shift middleware with full options.
     *
     * @param checkMainAxis  whether to shift on the main axis
     * @param checkCrossAxis whether to shift on the cross axis
     * @param padding        the padding for overflow detection
     */
    public static ShiftMiddleware create(boolean checkMainAxis, boolean checkCrossAxis, int padding) {
        return new ShiftMiddleware(checkMainAxis, checkCrossAxis, padding);
    }

    private ShiftMiddleware(boolean checkMainAxis, boolean checkCrossAxis, int padding) {
        this.checkMainAxis = checkMainAxis;
        this.checkCrossAxis = checkCrossAxis;
        this.padding = padding;
    }

    //endregion

    @Override
    public String name() {
        return "shift";
    }

    @Override
    public Result run(FloatingState state) {
        FloatingPlacement placement = state.placement();
        double x = state.x();
        double y = state.y();

        Insets overflow = state.detectOverflow(padding);

        // For shift: "mainAxis" means the axis that runs along the alignment (i.e., the cross axis of the side)
        FloatingPlacement.Axis sideAxis = placement.sideAxis();
        FloatingPlacement.Axis alignmentAxis = sideAxis.opposite();

        double mainAxisCoord;
        double crossAxisCoord;

        if (sideAxis == FloatingPlacement.Axis.y) {
            // Side is top/bottom → alignment axis is X → shift on X (mainAxis) and Y (crossAxis)
            mainAxisCoord = x;
            crossAxisCoord = y;
        } else {
            // Side is left/right → alignment axis is Y → shift on Y (mainAxis) and X (crossAxis)
            mainAxisCoord = y;
            crossAxisCoord = x;
        }

        if (checkMainAxis) {
            // Main axis for shift = the alignment axis = the axis along which the element slides
            FloatingPlacement.Side minSide;
            FloatingPlacement.Side maxSide;
            if (alignmentAxis == FloatingPlacement.Axis.y) {
                minSide = FloatingPlacement.Side.top;
                maxSide = FloatingPlacement.Side.bottom;
            } else {
                minSide = FloatingPlacement.Side.left;
                maxSide = FloatingPlacement.Side.right;
            }
            double min = mainAxisCoord + FloatingPositioning.getSide(overflow, minSide);
            double max = mainAxisCoord - FloatingPositioning.getSide(overflow, maxSide);
            mainAxisCoord = clamp(min, mainAxisCoord, max);
        }

        if (checkCrossAxis) {
            FloatingPlacement.Side minSide;
            FloatingPlacement.Side maxSide;
            if (sideAxis == FloatingPlacement.Axis.y) {
                minSide = FloatingPlacement.Side.top;
                maxSide = FloatingPlacement.Side.bottom;
            } else {
                minSide = FloatingPlacement.Side.left;
                maxSide = FloatingPlacement.Side.right;
            }
            double min = crossAxisCoord + FloatingPositioning.getSide(overflow, minSide);
            double max = crossAxisCoord - FloatingPositioning.getSide(overflow, maxSide);
            crossAxisCoord = clamp(min, crossAxisCoord, max);
        }

        double newX;
        double newY;
        if (sideAxis == FloatingPlacement.Axis.y) {
            newX = mainAxisCoord;
            newY = crossAxisCoord;
        } else {
            newX = crossAxisCoord;
            newY = mainAxisCoord;
        }

        state.setX(newX);
        state.setY(newY);

        state.putData(name(), "x", newX - x);
        state.putData(name(), "y", newY - y);

        return Result.done();
    }

    private static double clamp(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
}
