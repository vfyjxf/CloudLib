package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

/**
 * The banded {@link AttentionField}: a piecewise-constant radial field with
 * three levels — {@link #highValue HIGH} inside the inner radius (the
 * crosshair ± r band), {@link #mediumValue MEDIUM} in the mid band out to the
 * outer radius, {@link #lowValue LOW} beyond (the corners). The two boundary
 * radii are configurable; the level values are fixed so both attention fields
 * share one magnitude scale (peak 1, cheapest ≈ 0).
 * <p>
 * The piecewise field is the cheap field: three comparisons per sample, no
 * exp — for coarse-grained sampling or hot paths where the Gaussian's smooth
 * gradient does not buy anything.
 */
public final class PiecewiseAttention implements AttentionField {

    /** The inner-band (crosshair) level: 1.0 — the most expensive screen value. */
    public static final double highValue = 1.0;

    /** The mid-band level: 0.5. */
    public static final double mediumValue = 0.5;

    /** The corner/outer level: 0.1 — the cheapest screen value, never zero. */
    public static final double lowValue = 0.1;

    private final double centerX;
    private final double centerY;
    private final double innerRadius;
    private final double outerRadius;
    private final int samplingStep;

    /**
     * @param innerRadius the HIGH band radius around the origin — distances
     *        at or below it score {@link #highValue}
     * @param outerRadius the MEDIUM band radius — distances at or below it
     *        (and beyond the inner radius) score {@link #mediumValue}
     * @param samplingStep the {@link #cost(Rect)} sampling grid granularity
     *        in pixels, at least 1
     * @throws IllegalArgumentException if the radii are not finite and
     *         positive, if {@code innerRadius >= outerRadius}, or if the step
     *         is below 1
     */
    public PiecewiseAttention(
        double centerX,
        double centerY,
        double innerRadius,
        double outerRadius,
        int samplingStep
    ) {
        if (!Double.isFinite(innerRadius) || innerRadius <= 0.0) {
            throw new IllegalArgumentException("innerRadius must be finite and positive: " + innerRadius);
        }
        if (!Double.isFinite(outerRadius) || outerRadius <= innerRadius) {
            throw new IllegalArgumentException(
                "outerRadius must be finite and greater than innerRadius: " + outerRadius
            );
        }
        if (samplingStep < 1) {
            throw new IllegalArgumentException("samplingStep must be at least 1: " + samplingStep);
        }
        this.centerX = centerX;
        this.centerY = centerY;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.samplingStep = samplingStep;
    }

    /** A banded field with the default 4px sampling grid. */
    public PiecewiseAttention(double centerX, double centerY, double innerRadius, double outerRadius) {
        this(centerX, centerY, innerRadius, outerRadius, 4);
    }

    /** A banded field centered on the screen center, with the default 4px sampling grid. */
    public static PiecewiseAttention atScreenCenter(
        int screenWidth,
        int screenHeight,
        double innerRadius,
        double outerRadius
    ) {
        return new PiecewiseAttention(screenWidth * 0.5, screenHeight * 0.5, innerRadius, outerRadius);
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    /** The HIGH band radius, in pixels. */
    public double innerRadius() {
        return innerRadius;
    }

    /** The MEDIUM band radius, in pixels. */
    public double outerRadius() {
        return outerRadius;
    }

    @Override
    public int samplingStep() {
        return samplingStep;
    }

    @Override
    public double valueAt(double x, double y) {
        double dx = x - centerX;
        double dy = y - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= innerRadius) {
            return highValue;
        }
        return distance <= outerRadius ? mediumValue : lowValue;
    }
}
