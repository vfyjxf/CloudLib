package dev.vfyjxf.inworldui.internal;

/**
 * Pure keep/rescan/hide decisions for expand (hologram) panels. The world-side
 * ring scan stays in {@link InworldManager} because it needs block access; the
 * <em>decision rules</em> live here so the stability contract is testable.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Keep fast path</b>: a spot that is uncontested and whose projected
 *       screen rect covers ≤{@value #KEEP_COVER} of the foreground keeps its
 *       position without rescanning — the scan's scores are noisy and
 *       near-tied candidates flipping every frame is what makes holograms
 *       wander.</li>
 *   <li><b>Rescan margin</b>: when a rescan does happen, the current spot only
 *       loses when the challenger is better by an absolute
 *       {@value #MARGIN} score points — not a relative factor, so the
 *       coverage-weighted term can't flip the choice on a coin toss.</li>
 *   <li><b>Hide hysteresis</b>: hide above {@value #HIDE_COVER} projected
 *       coverage, re-show below {@value #SHOW_COVER} — the band between the
 *       two keeps the show/hide edge from flickering.</li>
 * </ul>
 */
final class ExpandPlacer {

    /** Current spot keeps its place while projected coverage stays below this. */
    static final double KEEP_COVER = 0.30;
    /** Current spot's total score must stay under this for the fast path. */
    static final double KEEP_SCORE = 320;
    /** Absolute score margin a challenger needs to displace the current spot. */
    static final double MARGIN = 48;
    /** Projected coverage above which a hidden-stay decision holds. */
    static final double HIDE_COVER = 0.35;
    /** A hidden panel may reappear once coverage drops below this. */
    static final double SHOW_COVER = 0.15;

    private ExpandPlacer() {
    }

    /**
     * Fast path: keep the current spot without rescanning when it is still
     * uncontested and reasonably clear on screen.
     *
     * @param contested   another hologram occupies this spot's volume
     * @param coverFrac   fraction of the spot's projected rect covering chrome
     * @param totalScore  spot score including the coverage-weighted term
     */
    static boolean keepSpot(boolean contested, double coverFrac, double totalScore) {
        return !contested && coverFrac <= KEEP_COVER && totalScore < KEEP_SCORE;
    }

    /**
     * Rescan hysteresis: the incumbent keeps its spot unless the challenger is
     * better by more than {@link #MARGIN} absolute points.
     */
    static boolean preferCurrent(double incumbentScore, double challengerScore) {
        return incumbentScore <= challengerScore + MARGIN;
    }

    /**
     * Whether the panel hides this frame. A shown panel hides once its best
     * spot's coverage exceeds {@link #HIDE_COVER}; a hidden one only returns
     * below {@link #SHOW_COVER} — the deadband between is the anti-flicker.
     */
    static boolean shouldHide(double bestCoverFrac, boolean wasHidden) {
        return bestCoverFrac > (wasHidden ? SHOW_COVER : HIDE_COVER);
    }

}
