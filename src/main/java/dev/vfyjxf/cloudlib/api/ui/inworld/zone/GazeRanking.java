package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The crosshair ranking keys (Z1): the total order that decides which
 * elements the gaze (the crosshair neighborhood) attends to first. The
 * comparator resolves, in order:
 * <ol>
 *   <li>screen distance — crosshair to the anchor's screen projection,
 *       ascending</li>
 *   <li>world distance — the anchor's distance in world space, ascending</li>
 *   <li>priority — descending (higher priority attends first)</li>
 *   <li>registration index — ascending</li>
 * </ol>
 * The final key makes the order total and stable: entries with equal keys up
 * to registration never swap (their relative order is the registration
 * order), so a sort is reproducible. The screen distance is a derived
 * quantity — {@link #entry(String, FloatPos, FloatPos, double, int, long)}
 * computes it from the crosshair and the anchor's projection.
 */
public final class GazeRanking {

    /**
     * One element's ranking keys.
     *
     * @param id the element id
     * @param screenDistance the crosshair-to-anchor-projection distance, in
     *        gui pixels
     * @param worldDistance the anchor's distance in world space
     * @param priority the element's arbitration priority
     * @param registrationIndex the element's registration order (the
     *        stability tie-break)
     */
    public record GazeEntry(
            String id, double screenDistance, double worldDistance, int priority, long registrationIndex) {

        public GazeEntry {
            Objects.requireNonNull(id, "id");
            requireNonNegative("screenDistance", screenDistance);
            requireNonNegative("worldDistance", worldDistance);
            if (registrationIndex < 0) {
                throw new IllegalArgumentException("registrationIndex must not be negative: " + registrationIndex);
            }
        }
    }

    private GazeRanking() {}

    /**
     * A ranking entry with the screen distance computed as the euclidean
     * distance between the crosshair and the anchor's screen projection.
     */
    public static GazeEntry entry(
            String id,
            FloatPos crosshair,
            FloatPos anchorScreen,
            double worldDistance,
            int priority,
            long registrationIndex) {
        Objects.requireNonNull(crosshair, "crosshair");
        Objects.requireNonNull(anchorScreen, "anchorScreen");
        double dx = anchorScreen.x() - crosshair.x();
        double dy = anchorScreen.y() - crosshair.y();
        return new GazeEntry(id, Math.sqrt(dx * dx + dy * dy), worldDistance, priority, registrationIndex);
    }

    /**
     * The gaze order: screen distance → world distance → priority (desc) →
     * registration index. A total order — equal keys up to registration never
     * swap.
     */
    public static Comparator<GazeEntry> gazeOrder() {
        return Comparator.<GazeEntry>comparingDouble(GazeEntry::screenDistance)
                .thenComparingDouble(GazeEntry::worldDistance)
                .thenComparing(Comparator.comparingInt(GazeEntry::priority).reversed())
                .thenComparingLong(GazeEntry::registrationIndex);
    }

    /**
     * The gaze-radius filter: entries whose anchor projection lies within
     * {@code radius} of the crosshair — their {@code screenDistance} at or
     * below it (boundary inclusive).
     *
     * @throws IllegalArgumentException if the radius is not finite and
     *         non-negative
     */
    public static Predicate<GazeEntry> withinGazeRadius(double radius) {
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("radius must be finite and non-negative: " + radius);
        }
        return entry -> entry.screenDistance() <= radius;
    }

    private static void requireNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
    }
}
