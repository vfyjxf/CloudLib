package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The label-side anchor of a leader line: which panel edge the line attaches
 * to, and where on that edge. Fixed mid-ports — one candidate per edge, port
 * position and exit direction locked together — replace the sliding
 * nearest-border-point attachment (the annotation literature's finding: fixed
 * ports read as tidier and space themselves more evenly than anchors that
 * slide along the line/rect intersection).
 * <p>
 * A committed port does not change face easily. Two hysteresis rules gate the
 * switch, both measured from the panel center toward the target:
 * <ul>
 *   <li><strong>The angular band.</strong> The incumbent face is held while
 *       the target direction stays within {@link Config#holdDeg} (55°) of the
 *       face's outward normal; a candidate face is only entered within
 *       {@link Config#enterDeg} (35°) of its normal. Because adjacent normals
 *       sit 90° apart, the two thresholds are the same boundary seen from
 *       either side — the 55°/35° equivalence band: a target wandering around
 *       the 45° diagonal cannot flip the port back and forth.</li>
 *   <li><strong>The exit-side deadzone.</strong> A switch additionally needs
 *       the target's offset <em>along the incumbent edge</em> (perpendicular
 *       to the exit direction) to exceed {@link Config#exitDeadzonePx}
 *       (48 px) — near-panel targets cannot chatter the port between faces
 *       they are barely offset from.</li>
 * </ul>
 * When the face does change — or the panel itself moves/resizes — the port
 * <em>slides along the panel perimeter</em> to its new position over
 * {@link Config#slideSeconds} (160 ms) with an ease-out curve, never
 * teleporting. While sliding, the port's exit direction is the normal of the
 * edge the moving point currently lies on, so the leader's final approach
 * stays perpendicular to the border throughout.
 * <p>
 * At rest every port sits on its face's midpoint — the layout-stable form.
 * The one exception is the hovered panel ({@link #resolve(String, FloatRect,
 * FloatPos, boolean, double)}): there the port tracks the target
 * continuously along the committed edge ({@link #restParam}), so the line
 * answers the pointer without the face ever leaving its hysteresis bands.
 * <p>
 * The class is the state shell around a pure core: {@link #pickFace},
 * {@link #restParam} and the perimeter parameterization are static,
 * deterministic functions of their arguments (no wall clock — the caller
 * hands each call its dt), so the whole resolver is headless-testable and
 * replay-stable.
 */
public final class AttachPointResolver {

    /** One of the four panel edges a port can attach to. Screen space, y down. */
    public enum Face {

        /** The {@code y0} edge; outward normal (0, −1). */
        top,

        /** The {@code y1} edge; outward normal (0, +1). */
        bottom,

        /** The {@code x0} edge; outward normal (−1, 0). */
        left,

        /** The {@code x1} edge; outward normal (+1, 0). */
        right
    }

    /**
     * One resolved attachment: the committed {@link Face}, the (possibly
     * mid-slide) border {@link FloatPos point} and the outward normal of the
     * edge that point currently lies on. At rest the point is exactly the
     * committed face's edge midpoint and the normal is that face's normal.
     */
    public record Port(Face face, FloatPos point, double normalX, double normalY) {}

    /**
     * The hysteresis and transition knobs.
     *
     * @param holdDeg the incumbent face is held while the target direction
     *        stays within this many degrees of the face's outward normal;
     *        must be in (0, 90)
     * @param enterDeg a candidate face is only entered within this many
     *        degrees of its normal; must be in (0, 90) — with four faces 90°
     *        apart, {@code holdDeg + enterDeg = 90} makes the two thresholds
     *        one equivalence band
     * @param exitDeadzonePx the along-edge offset the target must exceed
     *        before any face switch; non-negative
     * @param slideSeconds the perimeter slide duration for a face switch or
     *        panel move; positive
     */
    public record Config(double holdDeg, double enterDeg, double exitDeadzonePx, double slideSeconds) {

        public Config {
            if (!Double.isFinite(holdDeg) || holdDeg <= 0 || holdDeg >= 90) {
                throw new IllegalArgumentException("holdDeg must be in (0, 90): " + holdDeg);
            }
            if (!Double.isFinite(enterDeg) || enterDeg <= 0 || enterDeg >= 90) {
                throw new IllegalArgumentException("enterDeg must be in (0, 90): " + enterDeg);
            }
            if (!Double.isFinite(exitDeadzonePx) || exitDeadzonePx < 0) {
                throw new IllegalArgumentException("exitDeadzonePx must be finite and non-negative: " + exitDeadzonePx);
            }
            if (!Double.isFinite(slideSeconds) || slideSeconds <= 0) {
                throw new IllegalArgumentException("slideSeconds must be finite and positive: " + slideSeconds);
            }
        }

        /** The defaults: {@code 55, 35, 48, 0.16}. */
        public static Config ofDefaults() {
            return new Config(55.0, 35.0, 48.0, 0.16);
        }
    }

    /**
     * The fixed out-stub length every leader leaves its anchor by, in px, on
     * the way into the routing grid. The port end freezes the mirror-image
     * run instead — {@code LeaderGridRouter.Config}'s last segment (20 px)
     * plus its arrival gap (6 px) along the edge normal.
     */
    public static final double stubPx = 16.0;

    /** How far the hover-tracked port stays off a panel corner, in px. */
    public static final double hoverInsetPx = 8.0;

    private static final double epsilon = 1.0e-9;

    /** A rest-position change under this perimeter arc, in px, never re-opens the slide. */
    private static final double settleTolerance = 0.5;

    private final Config config;
    private final Map<String, State> states = new HashMap<>();

    public AttachPointResolver(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Resolves one panel's port for this frame, at rest on the committed
     * face's mid-port.
     *
     * @see #resolve(String, FloatRect, FloatPos, boolean, double)
     */
    public Port resolve(String id, FloatRect rect, FloatPos target, double dtSeconds) {
        return resolve(id, rect, target, false, dtSeconds);
    }

    /**
     * Resolves one panel's port for this frame.
     *
     * @param id the stable owner id of the panel (per-leader state is kept
     *        under it; a first-time id starts with no history)
     * @param rect the panel's border rect in screen px
     * @param target the point the leader comes from (the projected feature
     *        anchor) in the same space
     * @param hovered whether the panel carries the pointer this frame — the
     *        only state in which the port leaves its layout-stable mid-port
     *        and tracks the target continuously along the committed edge
     * @param dtSeconds the frame's dt; a non-advancing dt freezes the slide
     * @return the port to attach at — committed face, animated border point,
     *         local edge normal
     */
    public Port resolve(String id, FloatRect rect, FloatPos target, boolean hovered, double dtSeconds) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rect, "rect");
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(dtSeconds)) {
            throw new IllegalArgumentException("dtSeconds must be finite: " + dtSeconds);
        }
        State state = states.get(id);
        if (state == null) {
            state = new State();
            state.face = pickFace(rect, target.x(), target.y(), null, config);
            state.t = faceParam(rect, state.face);
            state.dest = state.t;
            states.put(id, state);
        } else {
            if (!sameRect(state.rect, rect)) {
                // the panel moved or resized: re-parameterize the carried
                // point onto the new perimeter before it settles again
                state.t = paramOf(rect, pointAt(state.rect, state.t));
                state.dest = state.t;
            }
            state.face = pickFace(rect, target.x(), target.y(), state.face, config);
            double rest = restParam(rect, state.face, target, hovered);
            if (Math.abs(wrapArc(state.dest, rest, perimeter(rect))) > settleTolerance) {
                beginSlide(state, rect, rest);
            }
        }
        state.rect = rect;
        advanceSlide(state, rect, dtSeconds);
        FloatPos point = pointAt(rect, state.t);
        Face edge = edgeAt(rect, state.t);
        return new Port(state.face, point, normalX(edge), normalY(edge));
    }

    /** Drops one owner's state (a closed panel); the next resolve starts fresh. */
    public void forget(String id) {
        states.remove(id);
    }

    /** Drops all state (a new scene, a teleport). */
    public void reset() {
        states.clear();
    }

    // region pure core

    /**
     * The face a target attaches at, with the hysteresis applied: no
     * incumbent picks the nearest normal; an incumbent is held inside its
     * hold band, and left only for a candidate inside its enter band whose
     * along-edge offset clears the exit deadzone. Deterministic.
     */
    public static Face pickFace(FloatRect rect, double targetX, double targetY, @Nullable Face current, Config config) {
        Face nearest = nearestFace(rect, targetX, targetY);
        if (current == null) {
            return nearest;
        }
        if (deviationDeg(rect, targetX, targetY, current) <= config.holdDeg()) {
            return current;
        }
        if (deviationDeg(rect, targetX, targetY, nearest) > config.enterDeg()) {
            // neither band qualifies — stability wins over chasing the nearest
            return current;
        }
        if (config.exitDeadzonePx() > 0) {
            FloatPos mid = faceMid(rect, current);
            double alongEdge = Math.abs(alongEdgeOffset(mid, normalX(current), normalY(current), targetX, targetY));
            if (alongEdge <= config.exitDeadzonePx()) {
                return current;
            }
        }
        return nearest;
    }

    /**
     * Where the port comes to rest for a target: the committed face's
     * mid-port at rest, or — in hover only — the point where the panel
     * center→target ray meets that face's edge, clamped to the edge with a
     * {@link #hoverInsetPx} corner margin. Hover slides the port along the
     * edge it already committed to; the choice of face stays the hysteresis's
     * ({@link #pickFace}). The face's line is parallel to the ray only when
     * the target sits exactly on the face's axis, which falls back to the
     * mid-port.
     */
    public static double restParam(FloatRect rect, Face face, FloatPos target, boolean hovered) {
        if (!hovered) {
            return faceParam(rect, face);
        }
        double cx = rect.centerX();
        double cy = rect.centerY();
        double dx = target.x() - cx;
        double dy = target.y() - cy;
        double x0 = rect.x();
        double y0 = rect.y();
        double x1 = x0 + rect.width();
        double y1 = y0 + rect.height();
        double insetX = Math.min(hoverInsetPx, rect.width() * 0.5);
        double insetY = Math.min(hoverInsetPx, rect.height() * 0.5);
        FloatPos hit =
                switch (face) {
                    case right ->
                        Math.abs(dx) < epsilon
                                ? null
                                : new FloatPos(x1, clamp(cy + (x1 - cx) / dx * dy, y0 + insetY, y1 - insetY));
                    case left ->
                        Math.abs(dx) < epsilon
                                ? null
                                : new FloatPos(x0, clamp(cy + (x0 - cx) / dx * dy, y0 + insetY, y1 - insetY));
                    case bottom ->
                        Math.abs(dy) < epsilon
                                ? null
                                : new FloatPos(clamp(cx + (y1 - cy) / dy * dx, x0 + insetX, x1 - insetX), y1);
                    case top ->
                        Math.abs(dy) < epsilon
                                ? null
                                : new FloatPos(clamp(cx + (y0 - cy) / dy * dx, x0 + insetX, x1 - insetX), y0);
                };
        return hit == null ? faceParam(rect, face) : paramOf(rect, hit);
    }

    private static double clamp(double value, double lo, double hi) {
        return value < lo ? lo : Math.min(value, hi);
    }

    /** The face whose outward normal is nearest the center→target direction; fixed order breaks ties. */
    public static Face nearestFace(FloatRect rect, double targetX, double targetY) {
        double dx = targetX - rect.centerX();
        double dy = targetY - rect.centerY();
        Face best = Face.right;
        double bestDev = Double.POSITIVE_INFINITY;
        for (Face face : Face.values()) {
            double dev = Math.abs(angleDeltaDeg(Math.toDegrees(Math.atan2(dy, dx)), normalDeg(face)));
            if (dev < bestDev - epsilon) {
                bestDev = dev;
                best = face;
            }
        }
        return best;
    }

    /** |Δ| between {@code targetDirDeg} and {@code faceDeg}, wrapped into [0, 180]. */
    private static double angleDeltaDeg(double targetDirDeg, double faceDeg) {
        double delta = (targetDirDeg - faceDeg) % 360.0;
        if (delta > 180.0) delta -= 360.0;
        if (delta < -180.0) delta += 360.0;
        return delta;
    }

    private static double deviationDeg(FloatRect rect, double targetX, double targetY, Face face) {
        double dx = targetX - rect.centerX();
        double dy = targetY - rect.centerY();
        return Math.abs(angleDeltaDeg(Math.toDegrees(Math.atan2(dy, dx)), normalDeg(face)));
    }

    /** The target's offset along the given edge (perpendicular to its normal). */
    private static double alongEdgeOffset(
            FloatPos edgeMid, double normalX, double normalY, double targetX, double targetY) {
        double tangentX = -normalY;
        double tangentY = normalX;
        return (targetX - edgeMid.x()) * tangentX + (targetY - edgeMid.y()) * tangentY;
    }

    /** The compass angle of a face's outward normal, in degrees. */
    private static double normalDeg(Face face) {
        return switch (face) {
            case right -> 0.0;
            case bottom -> 90.0;
            case left -> 180.0;
            case top -> -90.0;
        };
    }

    /** The outward normal x of a face. */
    public static double normalX(Face face) {
        return switch (face) {
            case right -> 1.0;
            case left -> -1.0;
            default -> 0.0;
        };
    }

    /** The outward normal y of a face. */
    public static double normalY(Face face) {
        return switch (face) {
            case bottom -> 1.0;
            case top -> -1.0;
            default -> 0.0;
        };
    }

    /** The face an axis-signed outward normal belongs to (the inverse of the normal accessors). */
    public static Face faceOfNormal(double normalX, double normalY) {
        if (normalX > 0.5) return Face.right;
        if (normalX < -0.5) return Face.left;
        if (normalY > 0.5) return Face.bottom;
        if (normalY < -0.5) return Face.top;
        throw new IllegalArgumentException("not an axis normal: (" + normalX + ", " + normalY + ")");
    }

    // endregion

    // region perimeter parameterization

    /**
     * The perimeter walk, clockwise in screen space from the top-left corner:
     * t ∈ [0, perimeter). Top edge first, then right, bottom, left.
     */
    public static double perimeter(FloatRect rect) {
        return 2.0 * (rect.width() + rect.height());
    }

    /** The perimeter parameter of a face's edge midpoint. */
    public static double faceParam(FloatRect rect, Face face) {
        double w = rect.width();
        double h = rect.height();
        return switch (face) {
            case top -> w * 0.5;
            case right -> w + h * 0.5;
            case bottom -> w + h + w * 0.5;
            case left -> w + h + w + h * 0.5;
        };
    }

    /** The face whose edge segment contains parameter {@code t} (half-open intervals, t wrapped). */
    public static Face edgeAt(FloatRect rect, double t) {
        double p = perimeter(rect);
        double u = ((t % p) + p) % p;
        double w = rect.width();
        double h = rect.height();
        if (u < w) return Face.top;
        if (u < w + h) return Face.right;
        if (u < w + h + w) return Face.bottom;
        return Face.left;
    }

    /** The border point at parameter {@code t} (wrapped). */
    public static FloatPos pointAt(FloatRect rect, double t) {
        double p = perimeter(rect);
        double u = ((t % p) + p) % p;
        double w = rect.width();
        double h = rect.height();
        double x0 = rect.x();
        double y0 = rect.y();
        double x1 = x0 + w;
        double y1 = y0 + h;
        if (u < w) return new FloatPos(x0 + u, y0);
        u -= w;
        if (u < h) return new FloatPos(x1, y0 + u);
        u -= h;
        if (u < w) return new FloatPos(x1 - u, y1);
        u -= w;
        return new FloatPos(x0, y1 - u);
    }

    /** The perimeter parameter of the border point nearest {@code point} (clamped, then snapped onto the facing edge). */
    public static double paramOf(FloatRect rect, FloatPos point) {
        double px = Math.max(rect.x(), Math.min(point.x(), rect.x() + rect.width()));
        double py = Math.max(rect.y(), Math.min(point.y(), rect.y() + rect.height()));
        double cx = rect.centerX();
        double cy = rect.centerY();
        boolean insideX = px > rect.x() + epsilon && px < rect.x() + rect.width() - epsilon;
        boolean insideY = py > rect.y() + epsilon && py < rect.y() + rect.height() - epsilon;
        if (insideX && insideY) {
            // an interior point (the rect grew past it): snap to the nearest edge
            double dLeft = px - rect.x();
            double dRight = rect.x() + rect.width() - px;
            double dTop = py - rect.y();
            double dBottom = rect.y() + rect.height() - py;
            double nearest = Math.min(Math.min(dLeft, dRight), Math.min(dTop, dBottom));
            if (nearest == dLeft) px = rect.x();
            else if (nearest == dRight) px = rect.x() + rect.width();
            else if (nearest == dTop) py = rect.y();
            else py = rect.y() + rect.height();
        } else if (insideX && !insideY) {
            py = point.y() < cy ? rect.y() : rect.y() + rect.height();
        } else if (insideY && !insideX) {
            px = point.x() < cx ? rect.x() : rect.x() + rect.width();
        }
        double w = rect.width();
        double h = rect.height();
        if (Math.abs(py - rect.y()) < epsilon) {
            return clampParam(px - rect.x(), perimeter(rect));
        }
        if (Math.abs(px - (rect.x() + w)) < epsilon) {
            return clampParam(w + (py - rect.y()), perimeter(rect));
        }
        if (Math.abs(py - (rect.y() + h)) < epsilon) {
            return clampParam(w + h + (rect.x() + w - px), perimeter(rect));
        }
        return clampParam(w + h + w + (rect.y() + h - py), perimeter(rect));
    }

    private static double clampParam(double t, double perimeter) {
        double u = ((t % perimeter) + perimeter) % perimeter;
        return u >= perimeter - epsilon ? 0.0 : u;
    }

    /** The shortest signed arc from {@code from} to {@code to} around a perimeter of length {@code perimeter}. */
    public static double wrapArc(double from, double to, double perimeter) {
        double delta = (to - from) % perimeter;
        if (delta > perimeter * 0.5) delta -= perimeter;
        if (delta < -perimeter * 0.5) delta += perimeter;
        return delta;
    }

    // endregion

    // region state shell

    private static final class State {
        Face face;
        FloatRect rect;
        double t;
        /** the parameter the slide is (or has settled) heading for */
        double dest;

        boolean sliding;
        double slideFrom;
        double slideDelta;
        double slideElapsed;
    }

    private void beginSlide(State state, FloatRect rect, double targetParam) {
        state.dest = targetParam;
        double delta = wrapArc(state.t, targetParam, perimeter(rect));
        if (Math.abs(delta) < epsilon) {
            state.t = targetParam;
            state.sliding = false;
            return;
        }
        state.sliding = true;
        state.slideFrom = state.t;
        state.slideDelta = delta;
        state.slideElapsed = 0.0;
    }

    private void advanceSlide(State state, FloatRect rect, double dtSeconds) {
        if (!state.sliding || dtSeconds <= 0.0) {
            return;
        }
        state.slideElapsed += dtSeconds;
        double p = Math.min(1.0, state.slideElapsed / config.slideSeconds());
        // smoothstep: zero velocity at both ends — the position derivative
        // never jumps, at the slide's start or its landing
        double eased = p * p * (3.0 - 2.0 * p);
        state.t = state.slideFrom + state.slideDelta * eased;
        if (p >= 1.0) {
            state.t = state.slideFrom + state.slideDelta;
            state.sliding = false;
        }
    }

    private static boolean sameRect(@Nullable FloatRect a, FloatRect b) {
        if (a == null) return false;
        return Math.abs(a.x() - b.x()) < epsilon
                && Math.abs(a.y() - b.y()) < epsilon
                && Math.abs(a.width() - b.width()) < epsilon
                && Math.abs(a.height() - b.height()) < epsilon;
    }

    private static FloatPos faceMid(FloatRect rect, Face face) {
        return switch (face) {
            case top -> new FloatPos(rect.centerX(), rect.y());
            case bottom -> new FloatPos(rect.centerX(), rect.y() + rect.height());
            case left -> new FloatPos(rect.x(), rect.centerY());
            case right -> new FloatPos(rect.x() + rect.width(), rect.centerY());
        };
    }

    // endregion
}
