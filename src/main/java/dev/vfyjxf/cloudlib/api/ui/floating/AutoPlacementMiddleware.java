package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.Insets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * FloatingMiddleware that automatically chooses the placement with the most available space.
 * <p>
 * Unlike {@link FlipMiddleware} which uses a "no space" fallback strategy,
 * this middleware always picks the placement with the most room.
 * <p>
 * <b>Note:</b> Cannot be used together with {@link FlipMiddleware}.
 * <p>
 */
public final class AutoPlacementMiddleware implements FloatingMiddleware {

    private final boolean crossAxis;
    private final @Nullable FloatingPlacement.Alignment alignment;
    private final boolean autoAlignment;
    private final @Nullable List<FloatingPlacement> allowedPlacements;
    private final int padding;

    //region factory

    /**
     * Creates an auto-placement middleware with default options.
     */
    public static AutoPlacementMiddleware create() {
        return new AutoPlacementMiddleware(false, null, true, null, 0);
    }

    /**
     * Creates an auto-placement middleware with the given padding.
     *
     * @param padding the padding for overflow detection
     */
    public static AutoPlacementMiddleware create(int padding) {
        return new AutoPlacementMiddleware(false, null, true, null, padding);
    }

    /**
     * Creates an auto-placement middleware with full options.
     *
     * @param crossAxis         whether to also check the cross axis for most space
     * @param alignment         restrict to placements with this alignment (or null for any)
     * @param autoAlignment     whether to also consider opposite alignment placements
     * @param allowedPlacements restrict to these placements (or null for all)
     * @param padding           the padding for overflow detection
     */
    public static AutoPlacementMiddleware create(
        boolean crossAxis,
        @Nullable FloatingPlacement.Alignment alignment,
        boolean autoAlignment,
        @Nullable List<FloatingPlacement> allowedPlacements,
        int padding
    ) {
        return new AutoPlacementMiddleware(crossAxis, alignment, autoAlignment, allowedPlacements, padding);
    }

    private AutoPlacementMiddleware(
        boolean crossAxis,
        @Nullable FloatingPlacement.Alignment alignment,
        boolean autoAlignment,
        @Nullable List<FloatingPlacement> allowedPlacements,
        int padding
    ) {
        this.crossAxis = crossAxis;
        this.alignment = alignment;
        this.autoAlignment = autoAlignment;
        this.allowedPlacements = allowedPlacements;
        this.padding = padding;
    }

    //endregion

    @Override
    public String name() {
        return "autoPlacement";
    }

    @Override
    public Result run(FloatingState state) {
        FloatingPlacement placement = state.placement();

        // Build filtered placement list
        List<FloatingPlacement> candidates = buildPlacementList();

        // Read pipeline index from state
        Integer currentIndex = state.getData(name(), "index");
        if (currentIndex == null) currentIndex = 0;

        FloatingPlacement currentPlacement = currentIndex < candidates.size()
            ? candidates.get(currentIndex)
            : null;

        if (currentPlacement == null) {
            return Result.done();
        }

        // If our current placement doesn't match, reset to the first candidate
        if (placement != currentPlacement) {
            return Result.reset(candidates.get(0));
        }

        // Detect overflow at this placement
        Insets overflow = state.detectOverflow(padding);
        FloatingPlacement.Side side = placement.side();
        int[] alignmentOverflow = FloatingPositioning.alignmentSides(overflow, placement, state.referenceRect(), state.floatingRect());

        int[] currentOverflows = {
            FloatingPositioning.getSide(overflow, side),
            alignmentOverflow[0],
            alignmentOverflow[1]
        };

        @SuppressWarnings("unchecked")
        List<OverflowEntry> allOverflows = state.getData(name(), "overflows");
        if (allOverflows == null) allOverflows = new ArrayList<>();
        allOverflows = new ArrayList<>(allOverflows);
        allOverflows.add(new OverflowEntry(currentPlacement, currentOverflows));

        int nextIndex = currentIndex + 1;

        // More placements to check?
        if (nextIndex < candidates.size()) {
            state.putData(name(), "index", nextIndex);
            state.putData(name(), "overflows", allOverflows);
            return Result.reset(candidates.get(nextIndex));
        }

        // All checked — find the best placement
        FloatingPlacement bestPlacement = findBestPlacement(allOverflows);

        if (bestPlacement != placement) {
            state.putData(name(), "index", nextIndex);
            state.putData(name(), "overflows", allOverflows);
            return Result.reset(bestPlacement);
        }

        return Result.done();
    }

    //region helpers

    private List<FloatingPlacement> buildPlacementList() {
        FloatingPlacement[] all = allowedPlacements != null
            ? allowedPlacements.toArray(FloatingPlacement[]::new)
            : FloatingPlacement.all();

        List<FloatingPlacement> result = new ArrayList<>();

        if (alignment != null) {
            // Prefer placements with this alignment first
            for (FloatingPlacement p : all) {
                if (p.alignment() == alignment) result.add(p);
            }
            if (autoAlignment) {
                FloatingPlacement.Alignment opposite = alignment.opposite();
                for (FloatingPlacement p : all) {
                    if (p.alignment() == opposite) result.add(p);
                }
            }
        } else {
            // Only base placements (no alignment)
            for (FloatingPlacement p : all) {
                if (p.alignment() == null) result.add(p);
            }
        }

        return result;
    }

    private FloatingPlacement findBestPlacement(List<OverflowEntry> allOverflows) {
        // First try: placements that fully fit
        FloatingPlacement fitting = null;
        double fittingScore = Double.MAX_VALUE;

        for (OverflowEntry entry : allOverflows) {
            int checkCount = entry.placement.alignment() != null ? 2 : 3;
            boolean fits = true;
            for (int i = 0; i < checkCount && i < entry.overflows.length; i++) {
                if (entry.overflows[i] > 0) {
                    fits = false;
                    break;
                }
            }
            if (fits) {
                double score = computeScore(entry);
                if (score < fittingScore) {
                    fittingScore = score;
                    fitting = entry.placement;
                }
            }
        }

        if (fitting != null) return fitting;

        // Fallback: placement with least total overflow
        FloatingPlacement best = allOverflows.get(0).placement;
        double bestScore = Double.MAX_VALUE;
        for (OverflowEntry entry : allOverflows) {
            double score = computeScore(entry);
            if (score < bestScore) {
                bestScore = score;
                best = entry.placement;
            }
        }
        return best;
    }

    private double computeScore(OverflowEntry entry) {
        if (crossAxis) {
            // Check main axis and main cross axis side
            double total = 0;
            for (int i = 0; i < Math.min(2, entry.overflows.length); i++) {
                total += entry.overflows[i];
            }
            return total;
        } else {
            return entry.overflows[0]; // main axis only
        }
    }

    //endregion

    //region overflow entry

    private record OverflowEntry(FloatingPlacement placement, int[] overflows) {
    }

    //endregion
}
