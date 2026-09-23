package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The visual-balance metric (batch 2): a whole-layout perceptual score of
 * <em>where the screen's panel mass sits</em>, as opposed to the per-candidate
 * terms of {@link ZoneCost} — the composition-level companion that answers
 * "is the layout visually centered", not "is this candidate good". Pure
 * geometry on gui pixels (origin top-left, y growing down); headless and
 * deterministic.
 * <p>
 * The model, in scoring order:
 * <ul>
 *   <li><strong>panel weight</strong> — {@code w = area × alpha ×
 *   nineGridWeight(center cell)}: visual mass grows with the covered area,
 *   with opacity, and with the attention the nine-grid cell at the panel's
 *   center carries. The nine-grid weights are the VME calibration (Zhang
 *   2025, <i>Symmetry</i>, fixations 23/14/13/12/11/8/7/6/6 over
 *   center/top-mid/mid-left/mid-right/bot-mid/top-left/top-right/bot-left/
 *   bot-right, r = 0.942 against judged balance) normalized to the center
 *   cell's 1.00: the center of the screen carries the most visual attention,
 *   the bottom corners the least.</li>
 *   <li><strong>centroid</strong> — {@code Σ wᵢcᵢ / Σ wᵢ} over the panel
 *   centers: the layout's center of visual mass.</li>
 *   <li><strong>offset</strong> — the L1 distance from the centroid to the
 *   target point with the lateral axis weighted ×1.5
 *   ({@link #lateralAxisWeight}): left-right imbalance is measurably worse
 *   than top-bottom imbalance of the same magnitude (VME).</li>
 *   <li><strong>target</strong> — the <em>optical center</em> {@code (W/2,
 *   H/2 − 0.035·H)}, the geometric center lifted 3.5% of the height: a
 *   balanced composition sits slightly above the mathematical middle
 *   (Arnheim; Ngo 2003).</li>
 *   <li><strong>score</strong> — {@code Bm = 1 − offset / D_max} with
 *   {@code D_max = (W + H) / 2}: 1.0 for a centroid exactly on the optical
 *   center, decreasing linearly in the (lateral-weighted) L1 offset,
 *   negative for extremes far past the normalization span.</li>
 * </ul>
 * A layout with no weighted panels is vacuously balanced: the centroid is
 * null and {@link #score} reads 1.0. Consumers composing the metric into a
 * cost usually want {@link #normalizedOffset} directly — the penalty shape
 * without the +1 constant.
 */
public final class VisualBalance {

    /**
     * The lateral-axis dominance of the offset: the horizontal component of
     * the centroid-target distance counts ×1.5 against the vertical's ×1
     * (VME — left-right imbalance is judged worse than top-bottom).
     */
    public static final double lateralAxisWeight = 1.5;

    /**
     * The optical center's lift above the geometric center, as a fraction of
     * the screen height (Arnheim; Ngo 2003).
     */
    public static final double opticalCenterLiftFraction = 0.035;

    /**
     * The VME nine-grid fixation counts (Zhang 2025, <i>Symmetry</i>),
     * row-major from the top-left cell: top-left 8, top-mid 14, top-right 7,
     * mid-left 13, center 23, mid-right 12, bot-left 6, bot-mid 11,
     * bot-right 6. Weights are the counts normalized by the center's.
     */
    private static final double[][] nineGridCounts = {{8, 14, 7}, {13, 23, 12}, {6, 11, 6},};

    private static final double centerCount = nineGridCounts[1][1];

    private VisualBalance() {}

    /**
     * One weighted panel of the layout: the rect plus its opacity factor.
     *
     * @param rect the panel's support rect, in gui pixels
     * @param alpha the panel's opacity in [0, 1], 1 fully opaque
     */
    public record Panel(FloatRect rect, double alpha) {

        public Panel {
            Objects.requireNonNull(rect, "rect");
            if (!Double.isFinite(alpha) || alpha < 0.0 || alpha > 1.0) {
                throw new IllegalArgumentException("alpha must be finite in [0, 1]: " + alpha);
            }
        }

        /** A fully opaque panel. */
        public static Panel opaque(FloatRect rect) {
            return new Panel(rect, 1.0);
        }
    }

    /**
     * The nine-grid attention weight of the grid cell containing the point —
     * the VME counts normalized to the center cell's 1.00: center 1.00,
     * top-mid 0.61, mid-left 0.57, mid-right 0.52, bot-mid 0.48, top-left
     * 0.35, top-right 0.30, bot-left 0.26, bot-right 0.26. Points on a cell
     * boundary fall to the cell the half-open thirds assign (the last cell
     * closes the interval).
     *
     * @param x the point's x, gui pixels
     * @param y the point's y, gui pixels
     * @param screenWidth the screen width, positive
     * @param screenHeight the screen height, positive
     */
    public static double nineGridWeight(double x, double y, double screenWidth, double screenHeight) {
        requireScreen(screenWidth, screenHeight);
        int col = gridThird(x, screenWidth);
        int row = gridThird(y, screenHeight);
        return nineGridCounts[row][col] / centerCount;
    }

    /**
     * One panel's visual mass: {@code area × alpha × nineGridWeight(the
     * panel's center cell)}. A zero-area panel weighs nothing.
     */
    public static double panelWeight(Panel panel, double screenWidth, double screenHeight) {
        Objects.requireNonNull(panel, "panel");
        return panel.rect().area()
                * panel.alpha()
                * nineGridWeight(panel.rect().centerX(), panel.rect().centerY(), screenWidth, screenHeight);
    }

    /**
     * The layout's center of visual mass, {@code Σ wᵢcᵢ / Σ wᵢ} over the
     * panel centers. Null when the total weight is zero — nothing on screen
     * has no centroid.
     */
    public static @Nullable FloatPos centroid(List<Panel> panels, double screenWidth, double screenHeight) {
        Objects.requireNonNull(panels, "panels");
        requireScreen(screenWidth, screenHeight);
        double totalWeight = 0.0;
        double x = 0.0;
        double y = 0.0;
        for (Panel panel : panels) {
            double weight = panelWeight(panel, screenWidth, screenHeight);
            if (weight <= 0.0) continue;
            totalWeight += weight;
            x += weight * panel.rect().centerX();
            y += weight * panel.rect().centerY();
        }
        if (totalWeight <= 0.0) return null;
        return new FloatPos(x / totalWeight, y / totalWeight);
    }

    /**
     * The optical center — the balance target: the geometric center lifted
     * {@link #opticalCenterLiftFraction} of the height (up is negative y).
     */
    public static FloatPos opticalCenter(double screenWidth, double screenHeight) {
        requireScreen(screenWidth, screenHeight);
        return new FloatPos(screenWidth * 0.5, screenHeight * (0.5 - opticalCenterLiftFraction));
    }

    /**
     * The lateral-weighted L1 distance between two points: {@code 1.5·|Δx| +
     * |Δy|} — the offset the score normalizes.
     */
    public static double offset(FloatPos centroid, FloatPos target) {
        Objects.requireNonNull(centroid, "centroid");
        Objects.requireNonNull(target, "target");
        return lateralAxisWeight * Math.abs(centroid.x() - target.x()) + Math.abs(centroid.y() - target.y());
    }

    /**
     * The offset's normalization span {@code D_max = (W + H) / 2}: the
     * unit-less balance score's denominator.
     */
    public static double maxOffset(double screenWidth, double screenHeight) {
        requireScreen(screenWidth, screenHeight);
        return (screenWidth + screenHeight) * 0.5;
    }

    /**
     * The balance score {@code Bm = 1 − offset / D_max} of the layout's
     * weighted centroid against the optical center: 1.0 dead on the target,
     * 0.0 at one D_max of lateral-weighted L1 offset, negative beyond. A
     * weightless layout scores 1.0 (vacuously balanced).
     */
    public static double score(List<Panel> panels, double screenWidth, double screenHeight) {
        return 1.0 - normalizedOffset(panels, screenWidth, screenHeight);
    }

    /**
     * The penalty shape of the score — {@code offset / D_max} without the +1
     * constant, 0.0 for a weightless layout. The form a cost function adds
     * (times its own weight).
     */
    public static double normalizedOffset(List<Panel> panels, double screenWidth, double screenHeight) {
        FloatPos centroid = centroid(panels, screenWidth, screenHeight);
        if (centroid == null) return 0.0;
        return offset(centroid, opticalCenter(screenWidth, screenHeight)) / maxOffset(screenWidth, screenHeight);
    }

    private static int gridThird(double value, double extent) {
        double third = Math.min(2.0, Math.max(0.0, Math.floor(value / (extent / 3.0))));
        return (int) third;
    }

    private static void requireScreen(double screenWidth, double screenHeight) {
        if (!Double.isFinite(screenWidth) || screenWidth <= 0.0) {
            throw new IllegalArgumentException("screenWidth must be finite and positive: " + screenWidth);
        }
        if (!Double.isFinite(screenHeight) || screenHeight <= 0.0) {
            throw new IllegalArgumentException("screenHeight must be finite and positive: " + screenHeight);
        }
    }
}
