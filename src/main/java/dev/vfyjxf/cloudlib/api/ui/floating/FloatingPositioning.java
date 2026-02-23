package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Core positioning engine for floating elements.
 * <p>
 * Computes the (x, y) coordinates to position a floating widget next to a reference
 * widget, applying an ordered pipeline of {@link FloatingMiddleware} to adjust position,
 * flip sides, shift along axes, constrain size, etc.
 */
public final class FloatingPositioning {

    /**
     * The result returned by {@link #compute}.
     *
     * @param x              the final x-coordinate (scene-space)
     * @param y              the final y-coordinate (scene-space)
     * @param placement      the final placement (may differ from the requested one)
     * @param middlewareData all data produced by middleware
     */
    public record PositionResult(
            double x,
            double y,
            FloatingPlacement placement,
            Map<String, Map<String, Object>> middlewareData
    ) {
    }

    private FloatingPositioning() {
    }

    //region computePosition

    /**
     * Computes the floating position.
     *
     * @param referenceRect the reference element bounds (scene-space)
     * @param floatingRect  the floating element bounds (only width/height are used)
     * @param boundary      the clipping boundary (typically the screen rect)
     * @param placement     the preferred placement
     * @param middleware    ordered list of middleware (nulls are filtered out)
     * @return the computed position result
     */
    public static PositionResult compute(
            Rect referenceRect,
            Rect floatingRect,
            Rect boundary,
            FloatingPlacement placement,
            List<@Nullable FloatingMiddleware> middleware
    ) {
        List<FloatingMiddleware> validMiddleware = middleware.stream()
                .filter(m -> m != null)
                .toList();

        // Compute initial coords from placement
        double[] coords = computeCoordsFromPlacement(referenceRect, floatingRect, placement);
        double x = coords[0];
        double y = coords[1];

        FloatingPlacement currentPlacement = placement;
        FloatingState state = new FloatingState(
                x, y,
                placement, currentPlacement,
                referenceRect, floatingRect,
                boundary
        );

        int resetCount = 0;

        for (int i = 0; i < validMiddleware.size(); i++) {
            FloatingMiddleware mw = validMiddleware.get(i);

            // Update state with current coords and placement
            state.setX(x);
            state.setY(y);
            state.setPlacement(currentPlacement);

            FloatingMiddleware.Result result = mw.run(state);

            // Read back potentially modified coords
            x = state.x();
            y = state.y();

            if (result.shouldReset() && resetCount <= 50) {
                resetCount++;

                if (result.resetPlacement() != null) {
                    currentPlacement = result.resetPlacement();
                }

                if (result.rectsChanged()) {
                    // re-read floating rect from state (middleware may have updated it)
                    floatingRect = state.floatingRect();
                }

                // Recompute base coords for the (possibly new) placement
                coords = computeCoordsFromPlacement(referenceRect, floatingRect, currentPlacement);
                x = coords[0];
                y = coords[1];

                state.setPlacement(currentPlacement);

                // Restart the pipeline
                i = -1;
            }
        }

        return new PositionResult(x, y, currentPlacement, state.allMiddlewareData());
    }

    //endregion

    //region computeCoordsFromPlacement

    /**
     * Computes the initial (x, y) coordinates for the floating element given the
     * reference rect, floating rect, and placement.
     *
     * @param reference the reference element rect
     * @param floating  the floating element rect
     * @param placement the desired placement
     * @return {x, y} coordinates
     */
    public static double[] computeCoordsFromPlacement(
            Rect reference,
            Rect floating,
            FloatingPlacement placement
    ) {
        FloatingPlacement.Side side = placement.side();
        FloatingPlacement.Alignment alignment = placement.alignment();
        FloatingPlacement.Axis sideAxis = placement.sideAxis();
        FloatingPlacement.Axis alignmentAxis = placement.alignmentAxis();

        double alignLength = length(floating, alignmentAxis);
        double refAlignLength = length(reference, alignmentAxis);

        // Center of reference minus half of floating on the alignment axis
        double commonAlign = refAlignLength / 2.0 - alignLength / 2.0;

        double x;
        double y;

        switch (side) {
            case top -> {
                x = reference.x() + reference.width() / 2.0 - floating.width() / 2.0;
                y = reference.y() - floating.height();
            }
            case bottom -> {
                x = reference.x() + reference.width() / 2.0 - floating.width() / 2.0;
                y = reference.y() + reference.height();
            }
            case right -> {
                x = reference.x() + reference.width();
                y = reference.y() + reference.height() / 2.0 - floating.height() / 2.0;
            }
            case left -> {
                x = reference.x() - floating.width();
                y = reference.y() + reference.height() / 2.0 - floating.height() / 2.0;
            }
            default -> {
                x = reference.x();
                y = reference.y();
            }
        }

        // Apply alignment
        if (alignment != null) {
            boolean isVerticalSide = sideAxis == FloatingPlacement.Axis.y;
            double alignCoord = isVerticalSide ? x : y;
            double refLen = isVerticalSide ? reference.width() : reference.height();
            double floatLen = isVerticalSide ? floating.width() : floating.height();
            double alignOffset = refLen / 2.0 - floatLen / 2.0;

            switch (alignment) {
                case start -> alignCoord -= alignOffset;
                case end -> alignCoord += alignOffset;
            }

            if (isVerticalSide) {
                x = alignCoord;
            } else {
                y = alignCoord;
            }
        }

        return new double[]{x, y};
    }

    //endregion

    //region utility methods

    /**
     * Gets the inset value for a given side.
     */
    public static int getSide(Insets insets, FloatingPlacement.Side side) {
        return switch (side) {
            case top -> insets.top();
            case right -> insets.right();
            case bottom -> insets.bottom();
            case left -> insets.left();
        };
    }

    /**
     * Gets the dimension of a rect on the given axis.
     */
    public static int length(Rect rect, FloatingPlacement.Axis axis) {
        return axis == FloatingPlacement.Axis.x ? rect.width() : rect.height();
    }

    /**
     * Returns the overflow values for the two alignment-axis sides of a placement.
     */
    public static int[] alignmentSides(Insets overflow, FloatingPlacement placement, Rect reference, Rect floating) {
        FloatingPlacement.Alignment alignment = placement.alignment();
        FloatingPlacement.Axis alignmentAxis = placement.alignmentAxis();

        FloatingPlacement.Side mainAlignmentSide;
        if (alignmentAxis == FloatingPlacement.Axis.x) {
            mainAlignmentSide = alignment == FloatingPlacement.Alignment.start
                    ? FloatingPlacement.Side.right
                    : FloatingPlacement.Side.left;
        } else {
            mainAlignmentSide = alignment == FloatingPlacement.Alignment.start
                    ? FloatingPlacement.Side.bottom
                    : FloatingPlacement.Side.top;
        }

        if (length(reference, alignmentAxis) > length(floating, alignmentAxis)) {
            mainAlignmentSide = mainAlignmentSide.opposite();
        }

        return new int[]{getSide(overflow, mainAlignmentSide), getSide(overflow, mainAlignmentSide.opposite())};
    }

    //endregion
}
