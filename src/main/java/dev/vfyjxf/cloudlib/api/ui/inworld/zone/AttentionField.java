package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.Rect;

/**
 * The screen-value scalar field C(x, y): how expensive it is to cover a
 * screen point with a panel. The field peaks (value 1) at the attention
 * origin — the crosshair / screen center — and decays toward the corners and
 * edges, encoding the screen-value semantics: <em>the center is the most
 * expensive real estate, the corners and edges the cheapest</em>. A higher
 * {@link #cost(Rect)} therefore means a worse place to put a panel.
 * <p>
 * {@link #cost(Rect)} is the discrete approximation of the field integral
 * over the rectangle, normalized to the rectangle's area — the mean field
 * value over the rect:
 * <pre>
 *   cost(r) ≈ (1 / area(r)) · Σ C(samplePointᵢ) · cellAreaᵢ   ∈ [0, 1]
 * </pre>
 * sampled on a {@link #samplingStep()}-pixel grid at cell centers. The
 * area normalization is what puts every rectangle on the same [0, 1] scale
 * regardless of its size: a rect covering the crosshair scores near 1, the
 * same rect pushed into a corner scores near the corner's value. An empty
 * rect (zero width or height) costs 0.
 * <p>
 * Implementations: {@link GaussianAttention} (isotropic decay, σ
 * configurable) and {@link PiecewiseAttention} (three-level bands, boundary
 * radii configurable).
 */
public interface AttentionField {

    /** The field value at {@code (x, y)} — always in {@code [0, 1]}, 1 at the peak. */
    double valueAt(double x, double y);

    /**
     * The sampling grid granularity in pixels — the side length of one sample
     * cell {@link #cost(Rect)} integrates over. At least 1.
     */
    int samplingStep();

    /**
     * The mean field value over {@code rect}, sampled on the
     * {@link #samplingStep()} grid at cell centers — the area-normalized
     * discrete approximation of ∫∫ C dA, in {@code [0, 1]}.
     */
    default double cost(Rect rect) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return 0.0;
        }
        double step = samplingStep();
        double sum = 0.0;
        long count = 0;
        for (double y = rect.y() + step * 0.5; y < rect.y() + rect.height(); y += step) {
            for (double x = rect.x() + step * 0.5; x < rect.x() + rect.width(); x += step) {
                sum += valueAt(x, y);
                count++;
            }
        }
        return count == 0 ? 0.0 : Math.min(1.0, Math.max(0.0, sum / count));
    }
}
