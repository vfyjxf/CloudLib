package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * FloatingMiddleware that flips the placement to keep the floating element in view.
 * <p>
 * When the floating element overflows the clipping boundary, it will try
 * alternative placements in order (opposite side first), and select the one
 * that fits best.
 * <p>
 */
public final class FlipMiddleware implements FloatingMiddleware {

    private final boolean checkMainAxis;
    private final boolean checkCrossAxis;
    private final @Nullable List<FloatingPlacement> fallbackPlacements;
    private final FallbackStrategy fallbackStrategy;
    private final FallbackAxisSideDirection fallbackAxisSideDirection;
    private final boolean flipAlignment;
    private final int padding;

    //region options

    /**
     * Strategy when no placement fits.
     */
    public enum FallbackStrategy {
        /**
         * Use the placement that fits best (least total overflow).
         */
        bestFit,
        /**
         * Fall back to the initial placement.
         */
        initialPlacement
    }

    /**
     * Direction to try on the perpendicular axis when the main axis doesn't fit.
     */
    public enum FallbackAxisSideDirection {
        none, start, end
    }

    //endregion

    //region factory

    /**
     * Creates a flip middleware with default settings.
     */
    public static FlipMiddleware create() {
        return new FlipMiddleware(true, true, null, FallbackStrategy.bestFit, FallbackAxisSideDirection.none, true, 0);
    }

    /**
     * Creates a flip middleware with the given padding.
     *
     * @param padding the padding for overflow detection
     */
    public static FlipMiddleware create(int padding) {
        return new FlipMiddleware(true, true, null, FallbackStrategy.bestFit, FallbackAxisSideDirection.none, true, padding);
    }

    /**
     * Creates a flip middleware with full options.
     *
     * @param checkMainAxis             whether to check the main axis for overflow
     * @param checkCrossAxis            whether to check the cross axis for overflow
     * @param fallbackPlacements        explicit placements to try, or null for computed
     * @param fallbackStrategy          what to do when no placement fits
     * @param fallbackAxisSideDirection which side to prefer on the perpendicular axis
     * @param flipAlignment             whether to flip alignment (start ↔ end)
     * @param padding                   padding for overflow detection
     */
    public static FlipMiddleware create(
            boolean checkMainAxis,
            boolean checkCrossAxis,
            @Nullable List<FloatingPlacement> fallbackPlacements,
            FallbackStrategy fallbackStrategy,
            FallbackAxisSideDirection fallbackAxisSideDirection,
            boolean flipAlignment,
            int padding
    ) {
        return new FlipMiddleware(checkMainAxis, checkCrossAxis, fallbackPlacements, fallbackStrategy, fallbackAxisSideDirection, flipAlignment, padding);
    }

    private FlipMiddleware(
            boolean checkMainAxis,
            boolean checkCrossAxis,
            @Nullable List<FloatingPlacement> fallbackPlacements,
            FallbackStrategy fallbackStrategy,
            FallbackAxisSideDirection fallbackAxisSideDirection,
            boolean flipAlignment,
            int padding
    ) {
        this.checkMainAxis = checkMainAxis;
        this.checkCrossAxis = checkCrossAxis;
        this.fallbackPlacements = fallbackPlacements;
        this.fallbackStrategy = fallbackStrategy;
        this.fallbackAxisSideDirection = fallbackAxisSideDirection;
        this.flipAlignment = flipAlignment;
        this.padding = padding;
    }

    //endregion

    @Override
    public String name() {
        return "flip";
    }

    @Override
    public Result run(FloatingState state) {
        FloatingPlacement placement = state.placement();
        FloatingPlacement initialPlacement = state.initialPlacement();
        FloatingPlacement.Axis initialSideAxis = initialPlacement.sideAxis();
        boolean isBasePlacement = initialPlacement.alignment() == null;

        // Build the list of fallback placements
        List<FloatingPlacement> computedFallbacks;
        if (fallbackPlacements != null) {
            computedFallbacks = new ArrayList<>(fallbackPlacements);
        } else {
            computedFallbacks = new ArrayList<>();
            if (isBasePlacement || !flipAlignment) {
                computedFallbacks.add(initialPlacement.opposite());
            } else {
                // Expanded placements: opposite alignment, opposite side, opposite side + opposite alignment
                computedFallbacks.add(initialPlacement.oppositeAlignment());
                computedFallbacks.add(initialPlacement.opposite());
                computedFallbacks.add(initialPlacement.opposite().oppositeAlignment());
            }

            // Add perpendicular axis placements if configured
            if (fallbackAxisSideDirection != FallbackAxisSideDirection.none) {
                List<FloatingPlacement> perpendicularPlacements = getOppositeAxisPlacements(initialPlacement, fallbackAxisSideDirection);
                for (FloatingPlacement p : perpendicularPlacements) {
                    if (!computedFallbacks.contains(p)) {
                        computedFallbacks.add(p);
                    }
                }
            }
        }

        List<FloatingPlacement> placements = new ArrayList<>();
        placements.add(initialPlacement);
        placements.addAll(computedFallbacks);

        // Detect overflow at current position
        Insets overflow = state.detectOverflow(padding);
        FloatingPlacement.Side side = placement.side();

        // Collect overflow values to check
        List<Integer> overflowValues = new ArrayList<>();
        if (checkMainAxis) {
            overflowValues.add(FloatingPositioning.getSide(overflow, side));
        }
        if (checkCrossAxis) {
            int[] alignmentOverflow = FloatingPositioning.alignmentSides(overflow, placement, state.referenceRect(), state.floatingRect());
            overflowValues.add(alignmentOverflow[0]);
            overflowValues.add(alignmentOverflow[1]);
        }

        // Read previous overflow data from middleware state
        Integer currentIndex = state.getData(name(), "index");
        if (currentIndex == null) currentIndex = 0;

        List<OverflowEntry> overflowsData = state.getData(name(), "overflows");
        if (overflowsData == null) overflowsData = new ArrayList<>();
        overflowsData = new ArrayList<>(overflowsData);
        overflowsData.add(new OverflowEntry(placement, overflowValues));

        // Check if any side is overflowing
        boolean anyOverflow = overflowValues.stream().anyMatch(v -> v > 0);

        if (anyOverflow) {
            int nextIndex = currentIndex + 1;

            if (nextIndex < placements.size()) {
                FloatingPlacement nextPlacement = placements.get(nextIndex);

                // Check if we should leave the current main axis
                boolean allMainAxisOverflow = overflowsData.stream()
                        .allMatch(d -> d.placement.sideAxis() == initialSideAxis
                                ? d.mainAxisOverflow() > 0
                                : true);

                boolean shouldTryNext = allMainAxisOverflow || nextPlacement.sideAxis() == initialSideAxis;

                if (shouldTryNext) {
                    state.putData(name(), "index", nextIndex);
                    state.putData(name(), "overflows", overflowsData);
                    return Result.reset(nextPlacement);
                }
            }

            // No more fallbacks — pick the best
            FloatingPlacement resetPlacement = findBestPlacement(overflowsData, initialSideAxis, initialPlacement);

            if (resetPlacement != placement) {
                return Result.reset(resetPlacement);
            }
        }

        return Result.done();
    }

    //region helpers

    private FloatingPlacement findBestPlacement(List<OverflowEntry> overflowsData, FloatingPlacement.Axis initialSideAxis, FloatingPlacement initialPlacement) {
        // First, find candidates that don't overflow the main axis
        FloatingPlacement candidate = overflowsData.stream()
                .filter(d -> d.mainAxisOverflow() <= 0)
                .min((a, b) -> Integer.compare(a.crossAxisTotalOverflow(), b.crossAxisTotalOverflow()))
                .map(d -> d.placement)
                .orElse(null);

        if (candidate != null) return candidate;

        // Fallback strategy
        return switch (fallbackStrategy) {
            case bestFit -> overflowsData.stream()
                    .min(Comparator.comparingInt(OverflowEntry::totalPositiveOverflow))
                    .map(d -> d.placement)
                    .orElse(initialPlacement);
            case initialPlacement -> initialPlacement;
        };
    }

    /**
     * Gets placements on the opposite axis.
     */
    private static List<FloatingPlacement> getOppositeAxisPlacements(FloatingPlacement placement, FallbackAxisSideDirection direction) {
        FloatingPlacement.Side side = placement.side();
        FloatingPlacement.Alignment alignment = placement.alignment();
        List<FloatingPlacement> result = new ArrayList<>();

        FloatingPlacement.Side[] perpendicularSides;
        boolean isStart = direction == FallbackAxisSideDirection.start;
        switch (side) {
            case top, bottom -> {
                perpendicularSides = isStart
                        ? new FloatingPlacement.Side[]{FloatingPlacement.Side.left, FloatingPlacement.Side.right}
                        : new FloatingPlacement.Side[]{FloatingPlacement.Side.right, FloatingPlacement.Side.left};
            }
            case left, right -> {
                perpendicularSides = isStart
                        ? new FloatingPlacement.Side[]{FloatingPlacement.Side.top, FloatingPlacement.Side.bottom}
                        : new FloatingPlacement.Side[]{FloatingPlacement.Side.bottom, FloatingPlacement.Side.top};
            }
            default -> {
                return result;
            }
        }

        for (FloatingPlacement.Side s : perpendicularSides) {
            if (alignment != null) {
                result.add(FloatingPlacement.of(s, alignment));
                result.add(FloatingPlacement.of(s, alignment.opposite()));
            } else {
                result.add(FloatingPlacement.of(s, null));
            }
        }

        return result;
    }

    //endregion

    //region overflow entry

    /**
     * Records the overflow at a particular placement for later comparison.
     */
    private record OverflowEntry(FloatingPlacement placement, List<Integer> overflows) {

        int mainAxisOverflow() {
            return overflows.isEmpty() ? 0 : overflows.getFirst();
        }

        int crossAxisTotalOverflow() {
            int total = 0;
            for (int i = 1; i < overflows.size(); i++) {
                total += overflows.get(i);
            }
            return total;
        }

        int totalPositiveOverflow() {
            int total = 0;
            for (int v : overflows) {
                if (v > 0) total += v;
            }
            return total;
        }
    }

    //endregion
}
