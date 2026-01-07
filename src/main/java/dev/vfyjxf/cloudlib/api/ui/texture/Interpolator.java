package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * Interpolator for computing intermediate values between two endpoints.
 * <p>
 * For primitive types, use the specialized interfaces to avoid boxing:
 * <ul>
 *   <li>{@link FloatInterpolator} - for float values</li>
 *   <li>{@link IntInterpolator} - for int values (including colors)</li>
 *   <li>{@link DoubleInterpolator} - for double values</li>
 * </ul>
 *
 * @param <T> the value type
 */
@FunctionalInterface
public interface Interpolator<T> {

    /**
     * Interpolates between start and end.
     *
     * @param start the start value
     * @param end   the end value
     * @param t     interpolation factor (0.0 to 1.0)
     * @return the interpolated result
     */
    T interpolate(T start, T end, float t);

    /**
     * Boxed float interpolation. Prefer {@link FloatInterpolator#LINEAR} to avoid boxing.
     */
    Interpolator<Float> FLOAT = FloatInterpolator.LINEAR.boxed();

    /**
     * Boxed int interpolation. Prefer {@link IntInterpolator#LINEAR} to avoid boxing.
     */
    Interpolator<Integer> INT = IntInterpolator.LINEAR.boxed();

    /**
     * Boxed double interpolation. Prefer {@link DoubleInterpolator#LINEAR} to avoid boxing.
     */
    Interpolator<Double> DOUBLE = DoubleInterpolator.LINEAR.boxed();

    /**
     * Boxed ARGB color interpolation. Prefer {@link IntInterpolator#COLOR} to avoid boxing.
     */
    Interpolator<Integer> COLOR = IntInterpolator.COLOR.boxed();

    /**
     * Discrete interpolation (no blending, switches at t=1).
     */
    static <T> Interpolator<T> discrete() {
        return (start, end, t) -> t < 1.0f ? start : end;
    }

    /**
     * Creates a boxed interpolator from a primitive float interpolator.
     */
    static Interpolator<Float> fromFloat(FloatInterpolator interpolator) {
        return interpolator.boxed();
    }

    /**
     * Creates a boxed interpolator from a primitive int interpolator.
     */
    static Interpolator<Integer> fromInt(IntInterpolator interpolator) {
        return interpolator.boxed();
    }

    /**
     * Creates a boxed interpolator from a primitive double interpolator.
     */
    static Interpolator<Double> fromDouble(DoubleInterpolator interpolator) {
        return interpolator.boxed();
    }
}
