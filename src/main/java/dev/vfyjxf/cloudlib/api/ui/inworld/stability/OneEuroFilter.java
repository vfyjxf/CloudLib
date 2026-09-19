package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

/**
 * The 1€ filter — a speed-adaptive low-pass filter for noisy pointing signals
 * (Casiez, Rozendaal, Vogel, Kellogg &amp; Cockburn, CHI 2012, <em>«1€ Filter:
 * A Simple Speed-based Low-pass Filter for Noisy Input in Interactive
 * Systems»</em>) — here in its single-axis form with one extension: a
 * {@code maxCutoff} ceiling the survey explicitly asks for, so a large
 * {@code beta} can never open the gate wide enough to pass jitter through at
 * extreme speeds.
 * <p>
 * The filter is a low-pass on position whose cutoff frequency rises with the
 * estimated speed: slow → heavy smoothing (jitter dies), fast → light
 * smoothing (lag dies). Speed is itself estimated by a low-pass
 * ({@code dCutoff}) on the raw per-sample derivative, so single-frame spikes
 * do not open the gate.
 *
 * <pre>{@code
 * first sample: xHat = x; dxHat = 0
 * each sample (t, x): Te = t - tPrev
 *   aD  = (2π·dCutoff·Te) / (2π·dCutoff·Te + 1)
 *   dx  = (x - xHatPrev) / Te
 *   dxHat = aD·dx + (1-aD)·dxHatPrev
 *   fC  = min(minCutoff + beta·|dxHat|, maxCutoff)   // extension: ceiling
 *   a   = (2π·fC·Te) / (2π·fC·Te + 1)
 *   xHat = a·x + (1-a)·xHatPrev
 * }</pre>
 *
 * <strong>Parameter provenance and units.</strong> The defaults
 * {@code minCutoff = 1.0 Hz}, {@code beta = 0.007}, {@code dCutoff = 1.0 Hz}
 * are the canonical values from the CHI 2012 paper, calibrated for signals
 * normalized to roughly unit range (meters, radians, normalized device
 * coordinates). {@code maxCutoff = 12 Hz} is the survey-synthesis ceiling: at
 * ordinary refresh rates (60–144 Hz) screen-space motion above ~12 Hz is no
 * longer resolvable as discrete jitter, so there is nothing to gain — and
 * everything to lose — by letting the adaptive cutoff run past it.
 * <p>
 * {@code beta} carries the signal's units: it is Hz <em>per</em>
 * (unit/second). <strong>Pixel-unit warning:</strong> with pixel positions,
 * speeds reach hundreds to thousands of px/s and {@code 0.007} opens the gate
 * far too eagerly — recalibrate to roughly {@code 0.001–0.01} per (px/s) for
 * screen-pixel signals (the survey's guidance), or measure as in the paper:
 * raise {@code minCutoff} until noise is unacceptable at rest, then raise
 * {@code beta} until lag is unacceptable while moving.
 * <p>
 * {@code Te} comes from the caller's timestamps, so the filter is safe under
 * variable frame rates: 30, 60 and 144 Hz samples of the same trajectory
 * smooth identically. The first call adopts the sample verbatim (no history to
 * smooth against). A later call with {@code Te <= 0} — a duplicate or rewound
 * timestamp — is ignored: the current estimate is returned and no state moves,
 * so a clock glitch upstream cannot inject a division blow-up here. After a
 * teleport or rebind the caller <em>must</em> call {@link #reset()} (the
 * survey is emphatic on this): otherwise the old position pollutes both the
 * derivative estimate (a phantom speed spike) and the output (a long glide
 * from the pre-teleport position).
 */
public final class OneEuroFilter {

    /** Canonical {@code minCutoff} from the CHI 2012 paper, in Hz. */
    public static final double defaultMinCutoff = 1.0;

    /**
     * Canonical {@code beta} from the CHI 2012 paper — Hz per (unit/second),
     * calibrated for unit-range signals; see the pixel-unit warning above.
     */
    public static final double defaultBeta = 0.007;

    /** Canonical {@code dCutoff} from the CHI 2012 paper, in Hz. */
    public static final double defaultDCutoff = 1.0;

    /** Survey-synthesis cutoff ceiling in Hz; see class javadoc. */
    public static final double defaultMaxCutoff = 12.0;

    private static final double twoPi = 2.0 * Math.PI;

    private final double minCutoff;
    private final double beta;
    private final double dCutoff;
    private final double maxCutoff;

    private boolean started;
    private double tPrev;
    private double xHat;
    private double dxHat;

    /** A filter with the paper's canonical parameters plus the 12 Hz ceiling. */
    public OneEuroFilter() {
        this(defaultMinCutoff, defaultBeta, defaultDCutoff, defaultMaxCutoff);
    }

    /**
     * @param minCutoff the baseline cutoff in Hz — the smoothing strength at
     *        rest; must be finite and positive
     * @param beta the speed-to-cutoff gain in Hz per (unit/second); finite and
     *        non-negative (0 disables adaptation: a plain low-pass)
     * @param dCutoff the derivative low-pass cutoff in Hz; finite and positive
     * @param maxCutoff the ceiling for the adaptive cutoff in Hz; finite,
     *        positive and at least {@code minCutoff}
     */
    public OneEuroFilter(double minCutoff, double beta, double dCutoff, double maxCutoff) {
        requirePositive(minCutoff, "minCutoff");
        if (!Double.isFinite(beta) || beta < 0.0) {
            throw new IllegalArgumentException("beta must be finite and non-negative: " + beta);
        }
        requirePositive(dCutoff, "dCutoff");
        requirePositive(maxCutoff, "maxCutoff");
        if (maxCutoff < minCutoff) {
            throw new IllegalArgumentException(
                    "maxCutoff must be at least minCutoff: " + maxCutoff + " < " + minCutoff);
        }
        this.minCutoff = minCutoff;
        this.beta = beta;
        this.dCutoff = dCutoff;
        this.maxCutoff = maxCutoff;
    }

    /**
     * Filters one sample taken at absolute time {@code tSeconds}.
     *
     * @return the filtered estimate after consuming the sample
     */
    public double filter(double tSeconds, double x) {
        requireFinite(tSeconds, "tSeconds");
        requireFinite(x, "x");
        if (!started) {
            started = true;
            tPrev = tSeconds;
            xHat = x;
            dxHat = 0.0;
            return xHat;
        }
        double te = tSeconds - tPrev;
        if (te <= 0.0) {
            return xHat;
        }
        tPrev = tSeconds;
        double aD = alpha(dCutoff, te);
        double dx = (x - xHat) / te;
        dxHat = aD * dx + (1.0 - aD) * dxHat;
        double cutoff = Math.min(minCutoff + beta * Math.abs(dxHat), maxCutoff);
        double a = alpha(cutoff, te);
        xHat = a * x + (1.0 - a) * xHat;
        return xHat;
    }

    /**
     * The current filtered estimate — {@code 0.0} before the first sample;
     * after {@link #reset()} the stale value until the next sample arrives.
     */
    public double value() {
        return xHat;
    }

    /** Whether at least one sample has been consumed since construction or reset. */
    public boolean hasSample() {
        return started;
    }

    /**
     * Forgets all history — mandatory after a teleport or rebind (see class
     * javadoc). The next sample is adopted verbatim, with a zero derivative
     * estimate, so no pre-reset position or speed leaks into the new regime.
     */
    public void reset() {
        started = false;
        tPrev = 0.0;
        xHat = 0.0;
        dxHat = 0.0;
    }

    /** {@code (2π·cutoff·te) / (2π·cutoff·te + 1)} — the exponential factor. */
    private static double alpha(double cutoff, double te) {
        double tau = twoPi * cutoff * te;
        return tau / (tau + 1.0);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
