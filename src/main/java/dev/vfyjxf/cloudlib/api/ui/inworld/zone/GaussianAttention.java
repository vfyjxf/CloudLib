package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

/**
 * The isotropic {@link AttentionField}: a radial Gaussian centered on the
 * attention origin,
 * <pre>
 *   C(x, y) = exp(−‖(x, y) − (cx, cy)‖² / (2σ²))
 * </pre>
 * with the spread σ configurable. Value 1 at the origin, e^(−0.5) ≈ 0.607 at
 * one σ, e^(−2) ≈ 0.135 at two σ, negligible beyond three σ. The Gaussian is
 * the smooth field: cost degrades continuously with distance from the
 * crosshair, with no band boundaries to tune.
 */
public final class GaussianAttention implements AttentionField {

    private final double centerX;
    private final double centerY;
    private final double sigma;
    private final int samplingStep;

    /**
     * @param samplingStep the {@link #cost(Rect)} sampling grid granularity
     *        in pixels, at least 1
     * @throws IllegalArgumentException if σ or the step is not finite and
     *         positive
     */
    public GaussianAttention(double centerX, double centerY, double sigma, int samplingStep) {
        if (!Double.isFinite(sigma) || sigma <= 0.0) {
            throw new IllegalArgumentException("sigma must be finite and positive: " + sigma);
        }
        if (samplingStep < 1) {
            throw new IllegalArgumentException("samplingStep must be at least 1: " + samplingStep);
        }
        this.centerX = centerX;
        this.centerY = centerY;
        this.sigma = sigma;
        this.samplingStep = samplingStep;
    }

    /** A Gaussian with the default 4px sampling grid. */
    public GaussianAttention(double centerX, double centerY, double sigma) {
        this(centerX, centerY, sigma, 4);
    }

    /** A Gaussian centered on the screen center, with the default 4px sampling grid. */
    public static GaussianAttention atScreenCenter(int screenWidth, int screenHeight, double sigma) {
        return new GaussianAttention(screenWidth * 0.5, screenHeight * 0.5, sigma);
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    /** The configured spread σ, in pixels. */
    public double sigma() {
        return sigma;
    }

    @Override
    public int samplingStep() {
        return samplingStep;
    }

    @Override
    public double valueAt(double x, double y) {
        double dx = x - centerX;
        double dy = y - centerY;
        return Math.exp(-(dx * dx + dy * dy) / (2.0 * sigma * sigma));
    }
}
