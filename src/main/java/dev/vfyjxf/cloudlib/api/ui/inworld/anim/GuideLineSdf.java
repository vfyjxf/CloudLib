package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import dev.vfyjxf.cloudlib.api.math.FloatPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure arithmetic behind a leader stroke's signed-distance rendering: the
 * dash phase, the arc-length fade toward the world end, and the arc-length
 * resample the layout-yield morph interpolates through.
 * <p>
 * The fragment shader of {@code cloudlib:guide_line_hud} evaluates the same
 * expressions per pixel — this class is the reference the host-side code (dash
 * phase uniform, fade parameter, morph) stays in step with, and the part that
 * can be tested without a GL context.
 * <p>
 * Arc positions are fractions of the stroke's total length, measured from the
 * <em>panel</em> end ({@code 0}) toward the world/anchor end ({@code 1}) —
 * the direction the entry animation grows in.
 */
public final class GuideLineSdf {

    private static final double epsilon = 1.0e-9;

    private GuideLineSdf() {}

    /**
     * The dash pattern's advance along the arc, in px, wrapped into one
     * period. One {@code cycleSeconds} advances the pattern by exactly one
     * period at {@code speedScale} 1; a hovered line runs its ants faster by
     * passing a scale above 1.
     *
     * @param elapsedSeconds the frame clock driving the ants, in seconds
     * @param periodPx the dash period (solid + gap) in px; positive
     * @param cycleSeconds the period travel time in seconds; positive
     * @param speedScale the flow multiplier (1 = the theme's base speed)
     * @return the phase in px, always in {@code [0, periodPx)}
     */
    public static double dashPhasePx(double elapsedSeconds, double periodPx, double cycleSeconds, double speedScale) {
        if (!Double.isFinite(periodPx) || periodPx <= 0) {
            throw new IllegalArgumentException("periodPx must be finite and positive: " + periodPx);
        }
        if (!Double.isFinite(cycleSeconds) || cycleSeconds <= 0) {
            throw new IllegalArgumentException("cycleSeconds must be finite and positive: " + cycleSeconds);
        }
        if (!Double.isFinite(speedScale) || speedScale <= 0) {
            throw new IllegalArgumentException("speedScale must be finite and positive: " + speedScale);
        }
        if (!Double.isFinite(elapsedSeconds)) {
            return 0.0;
        }
        double travel = elapsedSeconds / cycleSeconds * speedScale * periodPx;
        double phase = travel % periodPx;
        return phase < 0 ? phase + periodPx : phase;
    }

    /**
     * Whether the arc position falls inside a solid run of the dash pattern.
     * The fragment shader's {@code fract((arc * length + phase) / period) <
     * duty} is the same test.
     *
     * @param arcPx the position along the stroke in px
     * @param lengthPx the stroke's total length in px; positive
     * @param phasePx the phase from {@link #dashPhasePx}
     * @param periodPx the dash period in px; positive
     * @param duty the solid fraction of the period, in (0, 1]
     */
    public static boolean dashOn(double arcPx, double lengthPx, double phasePx, double periodPx, double duty) {
        if (!Double.isFinite(lengthPx) || lengthPx <= 0) return true;
        if (!Double.isFinite(periodPx) || periodPx <= 0) return true;
        if (!(duty > 0.0) || duty > 1.0) return true;
        double wrapped = (arcPx + phasePx) % periodPx;
        if (wrapped < 0) wrapped += periodPx;
        return wrapped < duty * periodPx;
    }

    /**
     * The alpha multiplier along the arc: full over the panel-side
     * {@code 1 - fadeFraction} of the stroke, easing down to
     * {@code endAlpha} at the world end — the tail that fades into the world
     * instead of ending on a hard cut.
     *
     * @param arcFraction the arc position, 0 at the panel end, 1 at the world end
     * @param fadeFraction the fraction of the arc the fade occupies, in [0, 1]
     * @param endAlpha the multiplier at the very end of the arc, in [0, 1]
     */
    public static double worldEndAlpha(double arcFraction, double fadeFraction, double endAlpha) {
        double t = Math.max(0.0, Math.min(1.0, arcFraction));
        double fade = Math.max(0.0, Math.min(1.0, fadeFraction));
        double floor = Math.max(0.0, Math.min(1.0, endAlpha));
        if (fade <= 0.0) return 1.0;
        double remaining = 1.0 - t;
        if (remaining >= fade) return 1.0;
        double k = remaining / fade; // 1 at the fade's start, 0 at its end
        return floor + (1.0 - floor) * k;
    }

    /** The total length of a polyline in px; 0 for fewer than two points. */
    public static double length(List<FloatPos> points) {
        double total = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            total += distance(points.get(i), points.get(i + 1));
        }
        return total;
    }

    /**
     * The point at arc fraction {@code t ∈ [0, 1]} along the polyline, by arc
     * length — the parameterization both the resample and a caller's own
     * markers use. Degenerate inputs (one point, zero length) answer the
     * first point.
     */
    public static FloatPos pointAt(List<FloatPos> points, double t) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("points must not be empty");
        }
        if (points.size() == 1) return points.get(0);
        double total = length(points);
        if (total < epsilon) return points.get(0);
        double target = Math.max(0.0, Math.min(1.0, t)) * total;
        double walked = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            FloatPos a = points.get(i);
            FloatPos b = points.get(i + 1);
            double segment = distance(a, b);
            if (walked + segment >= target || i + 2 == points.size()) {
                double k = segment < epsilon ? 0.0 : (target - walked) / segment;
                k = Math.max(0.0, Math.min(1.0, k));
                return new FloatPos(a.x() + (b.x() - a.x()) * k, a.y() + (b.y() - a.y()) * k);
            }
            walked += segment;
        }
        return points.get(points.size() - 1);
    }

    /**
     * {@code samples} points even in arc length along the polyline — the
     * common form two polylines of different shapes are compared in.
     */
    public static List<FloatPos> resample(List<FloatPos> points, int samples) {
        if (samples < 2) {
            throw new IllegalArgumentException("samples must be at least 2: " + samples);
        }
        List<FloatPos> out = new ArrayList<>(samples);
        if (points.isEmpty()) {
            return out;
        }
        for (int i = 0; i < samples; i++) {
            out.add(pointAt(points, i / (double) (samples - 1)));
        }
        return out;
    }

    /**
     * The layout-yield morph: both polylines resampled to {@code samples}
     * arc-length-even points and interpolated from {@code from} at {@code t =
     * 0} to {@code to} at {@code t = 1}. Equally shaped ends return the
     * endpoint unchanged; a vertex count mismatch is what the resample is
     * for, so a re-routed leader slides into its new shape instead of
     * snapping.
     */
    public static List<FloatPos> morph(List<FloatPos> from, List<FloatPos> to, double t, int samples) {
        List<FloatPos> a = resample(from, samples);
        List<FloatPos> b = resample(to, samples);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        double k = Math.max(0.0, Math.min(1.0, t));
        List<FloatPos> out = new ArrayList<>(samples);
        for (int i = 0; i < samples; i++) {
            FloatPos p = a.get(i);
            FloatPos q = b.get(i);
            out.add(new FloatPos(p.x() + (q.x() - p.x()) * k, p.y() + (q.y() - p.y()) * k));
        }
        return out;
    }

    /**
     * The polyline cut back {@code gapPx} from its world end, by arc length —
     * the renderer's alignment with the leader's arrival gap, so the stroke
     * stops short of the target instead of touching it. A gap at or beyond
     * the whole length leaves the single start point.
     */
    public static List<FloatPos> trimEnd(List<FloatPos> points, double gapPx) {
        if (points.size() < 2 || !(gapPx > 0)) {
            return List.copyOf(points);
        }
        double total = length(points);
        double keep = total - gapPx;
        if (keep <= epsilon) {
            return List.of(points.get(0));
        }
        List<FloatPos> out = new ArrayList<>(points.size());
        double walked = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            FloatPos a = points.get(i);
            FloatPos b = points.get(i + 1);
            double segment = distance(a, b);
            if (walked + segment >= keep) {
                double k = segment < epsilon ? 0.0 : (keep - walked) / segment;
                out.add(new FloatPos(a.x() + (b.x() - a.x()) * k, a.y() + (b.y() - a.y()) * k));
                return out;
            }
            out.add(a);
            walked += segment;
        }
        out.add(points.get(points.size() - 1));
        return out;
    }

    private static double distance(FloatPos a, FloatPos b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
