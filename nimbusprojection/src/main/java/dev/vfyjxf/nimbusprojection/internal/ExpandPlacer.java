package dev.vfyjxf.nimbusprojection.internal;

/**
 * Pure keep/rescan/hide decisions for expand (hologram) panels. The world-side
 * ring scan stays in {@link InworldManager} because it needs block access; the
 * <em>decision rules</em> live here so the stability contract is testable.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Keep fast path</b>: a spot that is uncontested and whose projected
 *       screen rect covers ≤{@value #keepCover} of the foreground keeps its
 *       position without rescanning — the scan's scores are noisy and
 *       near-tied candidates flipping every frame is what makes holograms
 *       wander.</li>
 *   <li><b>Rescan margin</b>: when a rescan does happen, the current spot only
 *       loses when the challenger is better by an absolute
 *       {@value #margin} score points — not a relative factor, so the
 *       coverage-weighted term can't flip the choice on a coin toss.</li>
 *   <li><b>Hide hysteresis</b>: hide above {@value #hideCover} projected
 *       coverage, re-show below {@value #showCover} — the band between the
 *       two keeps the show/hide edge from flickering.</li>
 * </ul>
 */
final class ExpandPlacer {

    /** Current spot keeps its place while projected coverage stays below this. */
    static final double keepCover = 0.30;
    /** Current spot's total score must stay under this for the fast path. */
    static final double keepScore = 320;
    /** Absolute score margin a challenger needs to displace the current spot. */
    static final double margin = 48;
    /** Projected coverage above which a hidden-stay decision holds. */
    static final double hideCover = 0.35;
    /** A hidden panel may reappear once coverage drops below this. */
    static final double showCover = 0.15;

    private ExpandPlacer() {}

    /**
     * Fast path: keep the current spot without rescanning when it is still
     * uncontested and reasonably clear on screen.
     *
     * @param contested   another hologram occupies this spot's volume
     * @param coverFrac   fraction of the spot's projected rect covering chrome
     * @param totalScore  spot score including the coverage-weighted term
     */
    static boolean keepSpot(boolean contested, double coverFrac, double totalScore) {
        return !contested && coverFrac <= keepCover && totalScore < keepScore;
    }

    /**
     * Rescan hysteresis: the incumbent keeps its spot unless the challenger is
     * better by more than {@link #margin} absolute points.
     */
    static boolean preferCurrent(double incumbentScore, double challengerScore) {
        return incumbentScore <= challengerScore + margin;
    }

    /**
     * Whether the panel hides this frame. A shown panel hides once its best
     * spot's coverage exceeds {@link #hideCover}; a hidden one only returns
     * below {@link #showCover} — the deadband between is the anti-flicker.
     */
    static boolean shouldHide(double bestCoverFrac, boolean wasHidden) {
        return bestCoverFrac > (wasHidden ? showCover : hideCover);
    }
}
