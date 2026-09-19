package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

/**
 * The follow-panel screen-position stabilizer: the composition facade that
 * wires two {@link OneEuroFilter}s (one per axis, shared parameters) into one
 * {@link PixelStabilizer}. Per frame, the raw projected screen position is
 * first de-jittered adaptively (heavy smoothing at rest, light while fast),
 * then passed to the pixel layer that locks it to the integer lattice when
 * stationary and lets it render sub-pixel when genuinely moving.
 * <p>
 * The escape gate mirrors the composition: {@link #bypass(double, double,
 * double)} seeds both filter stages at the raw position — the 1€ filters are
 * reset first so no pre-teleport position or derivative estimate leaks into
 * the new regime — and the pixel layer re-sticks directly. The caller owns
 * the trigger (tracking error above ~4% of screen height, or camera angular
 * speed above ~100°/s); this class only provides the mechanism.
 * <p>
 * Construct once with the full parameter set — the 1€ parameters are shared
 * by both axes, the pixel knobs ride along in a
 * {@link PixelStabilizer.Config} — then call {@link #accept} per frame and
 * {@link #reset()} on rebind. All times are seconds on the caller's clock;
 * see {@link OneEuroFilter} for the {@code beta} unit warning on
 * pixel-domain signals.
 */
public final class FollowStabilizer {

    private final OneEuroFilter filterX;
    private final OneEuroFilter filterY;
    private final PixelStabilizer pixels;

    /** A facade on the survey-synthesis defaults of both stages. */
    public FollowStabilizer() {
        this(
                OneEuroFilter.defaultMinCutoff,
                OneEuroFilter.defaultBeta,
                OneEuroFilter.defaultDCutoff,
                OneEuroFilter.defaultMaxCutoff,
                PixelStabilizer.Config.ofDefaults());
    }

    /**
     * @param minCutoff the 1€ baseline cutoff in Hz — smoothing at rest
     * @param beta the 1€ speed-to-cutoff gain in Hz per (px/s); see the unit
     *        warning in {@link OneEuroFilter}
     * @param dCutoff the 1€ derivative low-pass cutoff in Hz
     * @param maxCutoff the 1€ adaptive cutoff ceiling in Hz
     * @param pixelConfig the pixel-layer knobs; see {@link PixelStabilizer.Config}
     */
    public FollowStabilizer(
            double minCutoff, double beta, double dCutoff, double maxCutoff, PixelStabilizer.Config pixelConfig) {
        this.filterX = new OneEuroFilter(minCutoff, beta, dCutoff, maxCutoff);
        this.filterY = new OneEuroFilter(minCutoff, beta, dCutoff, maxCutoff);
        this.pixels = new PixelStabilizer(pixelConfig);
    }

    /**
     * One frame of the raw, unfiltered screen position.
     *
     * @return the stabilized position to render; integer-valued while the
     *         panel is at rest, sub-pixel while it moves
     */
    public PixelStabilizer.Output accept(double tSeconds, double x, double y) {
        double fx = filterX.filter(tSeconds, x);
        double fy = filterY.filter(tSeconds, y);
        return pixels.accept(tSeconds, fx, fy);
    }

    /**
     * The escape hatch for teleport-class events: both stages are reset and
     * re-seeded at the raw {@code (x, y)} — no glide from the old position,
     * no phantom velocity — and the output sticks there immediately.
     *
     * @return the re-stuck output position
     */
    public PixelStabilizer.Output bypass(double tSeconds, double x, double y) {
        filterX.reset();
        filterY.reset();
        filterX.filter(tSeconds, x);
        filterY.filter(tSeconds, y);
        return pixels.snapBypass(tSeconds, x, y);
    }

    /**
     * Drops all history in both stages; the next {@link #accept} re-seeds.
     * Call on rebind or when the anchor identity changes.
     */
    public void reset() {
        filterX.reset();
        filterY.reset();
        pixels.reset();
    }

    /** The current stabilized output x. */
    public double x() {
        return pixels.x();
    }

    /** The current stabilized output y. */
    public double y() {
        return pixels.y();
    }

    /** Whether the pixel layer is not yet lattice-locked (move or settle). */
    public boolean isMoving() {
        return pixels.isMoving();
    }

    /** The pixel layer's current phase. */
    public PixelStabilizer.State state() {
        return pixels.state();
    }
}
