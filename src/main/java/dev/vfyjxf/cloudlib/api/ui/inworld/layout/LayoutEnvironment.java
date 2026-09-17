package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * One frame's external inputs for the element-side pipeline stages: screen
 * size, exclusion rectangles, the occupancy snapshot of other elements
 * (grouped by space layer, typically taken from the coordinator's last
 * committed result), the resolved anchor, and the frame clock. The driving
 * adapter builds one per frame and hands it to the assembled element before
 * the coordinator runs.
 *
 * @param screenWidth the gui-scaled screen width
 * @param screenHeight the gui-scaled screen height
 * @param exclusionRects this frame's exclusion areas (the registry's output)
 * @param occupancy other elements' committed rects with their space layers
 * @param anchor this element's resolved anchor this frame; {@code null} when
 *        the anchor is gone (entity unloaded) — world-anchored elements
 *        retract into linger
 * @param nowSeconds the frame's absolute time; monotonic across frames —
 *        rewound values are clamped by the time-consuming components, not
 *        rejected
 * @param dtSeconds the elapsed time since the previous frame; must be finite
 *        — a negative value is clamped to zero (a rewound clock), not
 *        rejected
 */
public record LayoutEnvironment(
        int screenWidth,
        int screenHeight,
        List<Rect> exclusionRects,
        List<MaskedRect> occupancy,
        @Nullable AnchorFrame anchor,
        double nowSeconds,
        double dtSeconds) {

    public LayoutEnvironment {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("screen size must be positive: " + screenWidth + "x" + screenHeight);
        }
        exclusionRects = List.copyOf(exclusionRects);
        occupancy = List.copyOf(occupancy);
        if (!Double.isFinite(nowSeconds)) {
            throw new IllegalArgumentException("nowSeconds must be finite: " + nowSeconds);
        }
        if (!Double.isFinite(dtSeconds)) {
            throw new IllegalArgumentException("dtSeconds must be finite: " + dtSeconds);
        }
        dtSeconds = Math.max(0.0, dtSeconds);
    }

    /** An empty environment of the given size at time zero. */
    public static LayoutEnvironment of(int screenWidth, int screenHeight) {
        return new LayoutEnvironment(screenWidth, screenHeight, List.of(), List.of(), null, 0.0, 0.0);
    }

    /** The same environment with the anchor resolved. */
    public LayoutEnvironment withAnchor(AnchorFrame anchor) {
        return new LayoutEnvironment(
                screenWidth, screenHeight, exclusionRects, occupancy, anchor, nowSeconds, dtSeconds);
    }

    /** The same environment with the given exclusion rectangles. */
    public LayoutEnvironment withExclusions(List<Rect> rects) {
        return new LayoutEnvironment(screenWidth, screenHeight, rects, occupancy, anchor, nowSeconds, dtSeconds);
    }

    /** The same environment with the given occupancy snapshot. */
    public LayoutEnvironment withOccupancy(List<MaskedRect> rects) {
        return new LayoutEnvironment(screenWidth, screenHeight, exclusionRects, rects, anchor, nowSeconds, dtSeconds);
    }

    /** The same environment advanced on the clock. */
    public LayoutEnvironment at(double nowSeconds, double dtSeconds) {
        return new LayoutEnvironment(
                screenWidth, screenHeight, exclusionRects, occupancy, anchor, nowSeconds, dtSeconds);
    }

    /** One other element's committed rect, tagged with its space layer. */
    public record MaskedRect(SpaceMask layer, String elementId, Rect rect) {

        public MaskedRect {
            Objects.requireNonNull(layer, "layer");
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(rect, "rect");
        }
    }
}
