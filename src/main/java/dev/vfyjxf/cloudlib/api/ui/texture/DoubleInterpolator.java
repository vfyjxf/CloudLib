package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * Primitive double interpolator to avoid boxing overhead.
 */
@FunctionalInterface
public interface DoubleInterpolator {

    /**
     * Interpolates between start and end.
     *
     * @param start the start value
     * @param end   the end value
     * @param t     interpolation factor (0.0 to 1.0)
     * @return the interpolated result
     */
    double interpolate(double start, double end, float t);

    /**
     * Linear interpolation.
     */
    DoubleInterpolator LINEAR = (start, end, t) -> start + (end - start) * t;

    /**
     * Converts to a boxed Interpolator.
     */
    default Interpolator<Double> boxed() {
        return this::interpolate;
    }
}
