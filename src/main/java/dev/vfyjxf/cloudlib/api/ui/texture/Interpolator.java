package dev.vfyjxf.cloudlib.api.ui.texture;

/**
 * Interpolator for computing intermediate values between two endpoints.
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

    Interpolator<Float> FLOAT = (start, end, t) -> start + (end - start) * t;

    Interpolator<Integer> INT = (start, end, t) -> Math.round(start + (end - start) * t);

    Interpolator<Double> DOUBLE = (start, end, t) -> start + (end - start) * t;

    /**
     * ARGB color interpolation.
     */
    Interpolator<Integer> COLOR = (start, end, t) -> {
        int sa = (start >> 24) & 0xFF;
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;

        int ea = (end >> 24) & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;

        int a = Math.round(sa + (ea - sa) * t);
        int r = Math.round(sr + (er - sr) * t);
        int g = Math.round(sg + (eg - sg) * t);
        int b = Math.round(sb + (eb - sb) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    };

    /**
     * Discrete interpolation (no blending, switches at t=1).
     */
    static <T> Interpolator<T> discrete() {
        return (start, end, t) -> t < 1.0f ? start : end;
    }
}
