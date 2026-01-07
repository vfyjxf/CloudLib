package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * Primitive float interpolator to avoid boxing overhead.
 */
@FunctionalInterface
public interface FloatInterpolator {

    /**
     * Interpolates between start and end.
     *
     * @param start the start value
     * @param end   the end value
     * @param t     interpolation factor (0.0 to 1.0)
     * @return the interpolated result
     */
    float interpolate(float start, float end, float t);

    /**
     * Linear interpolation.
     */
    FloatInterpolator LINEAR = (start, end, t) -> start + (end - start) * t;

    /**
     * Converts to a boxed Interpolator.
     */
    default Interpolator<Float> boxed() {
        return this::interpolate;
    }
}
