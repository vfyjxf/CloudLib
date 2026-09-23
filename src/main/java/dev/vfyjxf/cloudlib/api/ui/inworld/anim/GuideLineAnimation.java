package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import dev.vfyjxf.cloudlib.api.ui.inworld.anim.ExitAnimation.Easing;

/**
 * One leader stroke's motion envelope: the arc-length reveal that grows the
 * line out of its panel, the retraction that takes it away, the
 * no-replay gap for a line that comes straight back, the hover swell, and
 * the short settle a layout-forced move slides through.
 * <p>
 * A per-leader state machine driven entirely by the caller's frame delta —
 * no wall clock, no GL, no geometry: {@link #advance} is told whether the
 * line exists this frame and how long it is, and answers with the arc
 * window, alpha and hover weights the renderer applies. Two polylines of the
 * same leader across frames are tied together by {@code geometryId}, whose
 * change is what opens the {@link Config#yieldSeconds} settle.
 * <p>
 * The phases and the shape of each envelope:
 * <ul>
 *   <li><strong>entering</strong> — after a {@link Config#staggerSeconds}
 *       delay (the panel leads, the line follows) the drawn arc grows from
 *       the panel end outward: {@code [0, t]} with {@code t} eased toward 1
 *       over {@code clamp(length / }{@link Config#enterSpeedPxPerSec}{@code ,
 *       min, max)}.</li>
 *   <li><strong>shown</strong> — the whole arc.</li>
 *   <li><strong>exiting</strong> — faster than the entry, and directional:
 *       a line that had finished growing retracts into its world end
 *       ({@code [t, 1]}, the anchor end last), while one interrupted mid-
 *       growth unwinds back into its panel ({@code [0, t]} shrinking) —
 *       never a jump to the other half of the arc. Either way alpha eases
 *       1 → 0.</li>
 *   <li><strong>hidden</strong> — nothing drawn; the time since the last
 *       present frame is remembered so a return inside
 *       {@link Config#respawnSeconds} restores the line with an alpha ramp
 *       only — the growth is not replayed.</li>
 * </ul>
 * Hover is a smoothed 0 → 1 weight the renderer scales its alpha and width
 * bonuses by; it eases over {@link Config#hoverSeconds} in both directions
 * and never gates visibility.
 * <p>
 * A tier-form swap (the full routing, the fold segment, the attach marks)
 * opens a cross-fade instead of a pop: the caller passes the leader's form
 * token and draws the outgoing form at {@code 1 - tierFade} beside the
 * incoming one at {@code tierFade} — complementary weights over
 * {@link Config#fadeSeconds}, never two full-ink forms in one frame.
 */
public final class GuideLineAnimation {

    private static final double epsilon = 1.0e-9;

    /**
     * The envelope timings and the dash flow.
     *
     * @param enterSpeedPxPerSec the growth speed the entry duration is
     *        measured at, in px per second; positive
     * @param enterMinSeconds the floor on the entry duration; positive
     * @param enterMaxSeconds the ceiling on the entry duration; at least
     *        {@code enterMinSeconds}
     * @param exitSeconds the retraction duration — the exit is meant to feel
     *        quicker than the entry; positive
     * @param respawnSeconds how long after disappearing a return still counts
     *        as the same gesture (alpha ramp, no replayed growth);
     *        non-negative
     * @param hoverSeconds the hover swell transition; positive
     * @param yieldSeconds the settle a changed route slides through;
     *        non-negative
     * @param staggerSeconds the delay between the panel appearing and the
     *        line starting to grow; non-negative
     * @param dashPeriodPx the dash period in px; 0 leaves the stroke solid
     * @param dashCycleSeconds the time one dash period takes to travel at
     *        {@code dashHoverSpeed} 1; positive
     * @param dashHoverSpeed the flow multiplier while hovered (1.5–2 in the
     *        survey); at least 1
     * @param fadeSeconds the tier-form cross-fade: when the form token
     *        changes, the new form ramps in over this long (and the caller
     *        draws the old one out over the same window); positive
     */
    public record Config(
        double enterSpeedPxPerSec,
        double enterMinSeconds,
        double enterMaxSeconds,
        double exitSeconds,
        double respawnSeconds,
        double hoverSeconds,
        double yieldSeconds,
        double staggerSeconds,
        double dashPeriodPx,
        double dashCycleSeconds,
        double dashHoverSpeed,
        double fadeSeconds
    ) {

        public Config {
            requirePositive(enterSpeedPxPerSec, "enterSpeedPxPerSec");
            requirePositive(enterMinSeconds, "enterMinSeconds");
            requirePositive(enterMaxSeconds, "enterMaxSeconds");
            if (enterMaxSeconds < enterMinSeconds) {
                throw new IllegalArgumentException(
                    "enterMaxSeconds must be at least enterMinSeconds: " + enterMaxSeconds
                );
            }
            requirePositive(exitSeconds, "exitSeconds");
            requireNonNegative(respawnSeconds, "respawnSeconds");
            requirePositive(hoverSeconds, "hoverSeconds");
            requireNonNegative(yieldSeconds, "yieldSeconds");
            requireNonNegative(staggerSeconds, "staggerSeconds");
            if (!Double.isFinite(dashPeriodPx) || dashPeriodPx < 0) {
                throw new IllegalArgumentException("dashPeriodPx must be finite and non-negative: " + dashPeriodPx);
            }
            requirePositive(dashCycleSeconds, "dashCycleSeconds");
            if (!Double.isFinite(dashHoverSpeed) || dashHoverSpeed < 1) {
                throw new IllegalArgumentException("dashHoverSpeed must be finite and at least 1: " + dashHoverSpeed);
            }
            requirePositive(fadeSeconds, "fadeSeconds");
        }

        /**
         * The survey defaults: 900 px/s, 120–320 ms enter, 170 ms exit,
         * 250 ms respawn, 125 ms hover, 90 ms yield, 50 ms stagger, an 8 px
         * dash on a 400 ms cycle flowing 1.75× while hovered, and a 170 ms
         * tier-form cross-fade.
         */
        public static Config ofDefaults() {
            return new Config(900.0, 0.12, 0.32, 0.17, 0.25, 0.125, 0.09, 0.05, 8.0, 0.4, 1.75, 0.17);
        }

        /** The pre-fade constructor — the cross-fade takes the survey default. */
        public Config(
            double enterSpeedPxPerSec,
            double enterMinSeconds,
            double enterMaxSeconds,
            double exitSeconds,
            double respawnSeconds,
            double hoverSeconds,
            double yieldSeconds,
            double staggerSeconds,
            double dashPeriodPx,
            double dashCycleSeconds,
            double dashHoverSpeed
        ) {
            this(
                enterSpeedPxPerSec,
                enterMinSeconds,
                enterMaxSeconds,
                exitSeconds,
                respawnSeconds,
                hoverSeconds,
                yieldSeconds,
                staggerSeconds,
                dashPeriodPx,
                dashCycleSeconds,
                dashHoverSpeed,
                0.17
            );
        }

        /** The entry duration for a stroke {@code lengthPx} long, clamped to the configured window. */
        public double enterSeconds(double lengthPx) {
            double length = Double.isFinite(lengthPx) && lengthPx > 0 ? lengthPx : 0.0;
            return Math.max(enterMinSeconds, Math.min(enterMaxSeconds, length / enterSpeedPxPerSec));
        }

        private static void requirePositive(double value, String name) {
            if (!Double.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException(name + " must be finite and positive: " + value);
            }
        }

        private static void requireNonNegative(double value, String name) {
            if (!Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
            }
        }
    }

    /** Which envelope the leader is in. */
    public enum Phase {
        /** Nothing drawn; the return window is still open. */
        hidden,
        /** Growing out of the panel. */
        entering,
        /** Fully drawn. */
        shown,
        /** Retracting. */
        exiting
    }

    /**
     * One frame's answer.
     *
     * @param visible whether the stroke is drawn at all
     * @param arcStart the drawn arc's start fraction, 0 at the panel end
     * @param arcEnd the drawn arc's end fraction, 1 at the world end
     * @param alpha the stroke's alpha multiplier (the exit's fade, the
     *        respawn's ramp)
     * @param hover the smoothed hover weight in [0, 1]
     * @param morph the settle weight in [0, 1]: 0 holds the previous route,
     *        1 is the current one
     * @param tierFade the tier-form cross-fade weight in [0, 1]: 0 on the
     *        frame the form token changed, 1 once the new form has fully
     *        taken over — the caller draws the outgoing form at
     *        {@code 1 - tierFade} and the incoming one at {@code tierFade},
     *        complementary weights that never stack
     * @param dashPhasePx the dash pattern's phase along the arc, in px,
     *        always inside one period — an uniform, never geometry
     */
    public record Sample(
        boolean visible,
        double arcStart,
        double arcEnd,
        double alpha,
        double hover,
        double morph,
        double tierFade,
        double dashPhasePx
    ) {}

    /** The form-token sentinel: the caller is not tracking tier forms. */
    private static final long noForm = Long.MIN_VALUE;

    private final Config config;

    private Phase phase = Phase.hidden;
    private double enter;
    private double exit;
    private double alpha;
    private double hover;
    private double morph = 1.0;
    private double tierFade = 1.0;
    private double sinceGone;
    private double delayLeft;
    private boolean retractToAnchor;
    private long lastGeometry = Long.MIN_VALUE;
    private long lastForm = noForm;
    private double dashPhase;

    public GuideLineAnimation(Config config) {
        this.config = config == null ? Config.ofDefaults() : config;
    }

    public Config config() {
        return config;
    }

    public Phase phase() {
        return phase;
    }

    /**
     * Drives one frame without tier-form tracking — the cross-fade stays
     * closed ({@code tierFade} 1).
     *
     * @see #advance(boolean, double, long, long, boolean, double)
     */
    public Sample advance(boolean present, double lengthPx, long geometryId, boolean hovered, double dtSeconds) {
        return advance(present, lengthPx, geometryId, noForm, hovered, dtSeconds);
    }

    /**
     * Drives one frame.
     *
     * @param present whether the leader has geometry this frame (an
     *        out-of-tolerance or off-view leader is absent, not hidden)
     * @param lengthPx the routed stroke's length, sizing the entry duration
     * @param geometryId a token that changes when the route's committed
     *        topology changes — an unchanged id keeps the settle closed, so
     *        an endpoint sliding under a stable commit must not move it (the
     *        caller passes the router's per-leader epoch, not a geometry
     *        hash)
     * @param formToken a token that changes when the leader's tier form
     *        changes (the full routing, the fold segment, the attach marks);
     *        a change opens the cross-fade — the outgoing form keeps drawing
     *        at {@code 1 - tierFade} while the incoming one ramps in — so a
     *        form swap never pops. {@link Long#MIN_VALUE} disables tracking
     * @param hovered whether the panel carries the pointer this frame
     * @param dtSeconds the frame's delta; non-negative
     * @return this frame's envelope
     */
    public Sample advance(
        boolean present,
        double lengthPx,
        long geometryId,
        long formToken,
        boolean hovered,
        double dtSeconds
    ) {
        if (!Double.isFinite(dtSeconds) || dtSeconds < 0) {
            throw new IllegalArgumentException("dtSeconds must be finite and non-negative: " + dtSeconds);
        }
        double dt = dtSeconds;
        hover = approach(hover, hovered ? 1.0 : 0.0, dt / config.hoverSeconds());
        // the ants are integrated rather than read off a clock: a hover speed
        // change then accelerates the flow instead of jumping its phase
        double speed = 1.0 + (config.dashHoverSpeed() - 1.0) * hover;
        if (config.dashPeriodPx() > 0) {
            dashPhase = (dashPhase + config.dashPeriodPx() * speed * dt / config.dashCycleSeconds())
                    % config.dashPeriodPx();
        }
        if (phase != Phase.hidden && geometryId != lastGeometry) {
            morph = 0.0; // a re-routed line settles into its new shape
        }
        lastGeometry = geometryId;
        morph = Math.min(1.0, morph + (config.yieldSeconds() <= 0 ? 1.0 : dt / config.yieldSeconds()));
        if (formToken != noForm) {
            if (lastForm != noForm && formToken != lastForm) {
                tierFade = 0.0; // the form swapped: cross-fade the old one out
            }
            lastForm = formToken;
        }
        tierFade = Math.min(1.0, tierFade + (config.fadeSeconds() <= 0 ? 1.0 : dt / config.fadeSeconds()));

        if (present) {
            if (sinceGone > 0 && sinceGone <= config.respawnSeconds()) {
                // it came straight back: restore the line, ramp the alpha —
                // replaying the growth here would read as a flicker
                phase = Phase.shown;
                enter = 1.0;
                exit = 0.0;
                retractToAnchor = false;
            } else if (phase == Phase.hidden || phase == Phase.exiting) {
                phase = Phase.entering;
                enter = 0.0;
                exit = 0.0;
                retractToAnchor = false;
                alpha = 1.0;
                delayLeft = config.staggerSeconds();
            }
            sinceGone = 0;
            if (phase == Phase.entering) {
                if (delayLeft > 0) {
                    delayLeft = Math.max(0.0, delayLeft - dt);
                }
                if (delayLeft <= epsilon) {
                    enter = Math.min(1.0, enter + dt / config.enterSeconds(lengthPx));
                    if (enter >= 1.0) {
                        enter = 1.0;
                        phase = Phase.shown;
                    }
                }
            }
            if (phase != Phase.exiting) {
                alpha = approach(alpha, 1.0, dt / config.exitSeconds());
            }
        } else {
            sinceGone += dt;
            if (phase == Phase.entering || phase == Phase.shown) {
                retractToAnchor = phase == Phase.shown;
                phase = Phase.exiting;
                exit = 0.0;
            }
            if (phase == Phase.exiting) {
                exit = Math.min(1.0, exit + dt / config.exitSeconds());
                alpha = 1.0 - exit;
                if (!retractToAnchor) {
                    enter = Math.max(0.0, enter - dt / config.enterSeconds(lengthPx));
                }
                if (exit >= 1.0) {
                    phase = Phase.hidden;
                    enter = 0.0;
                }
            }
        }

        double arcStart = retractToAnchor ? exit : 0.0;
        double grown = retractToAnchor ? 1.0 : eased(enter) * (1.0 - exit);
        double arcEnd = Math.max(arcStart, grown);
        boolean visible = phase != Phase.hidden && arcEnd - arcStart > epsilon && alpha > epsilon;
        return new Sample(visible, arcStart, arcEnd, alpha, hover, morph, tierFade, dashPhase);
    }

    /** Back to the never-drawn state (a new scene, a teleport). */
    public void reset() {
        phase = Phase.hidden;
        enter = 0.0;
        exit = 0.0;
        alpha = 0.0;
        hover = 0.0;
        morph = 1.0;
        tierFade = 1.0;
        sinceGone = 0.0;
        delayLeft = 0.0;
        retractToAnchor = false;
        lastGeometry = Long.MIN_VALUE;
        lastForm = noForm;
        dashPhase = 0.0;
    }

    /** The eased growth: the arc reaches most of its length early and eases into place. */
    private static double eased(double t) {
        return Easing.easeOut.apply((float) Math.max(0.0, Math.min(1.0, t)));
    }

    /** One linear step of {@code value} toward {@code target}, arriving within {@code span}. */
    private static double approach(double value, double target, double span) {
        if (span <= 0.0) return target;
        double step = Math.min(1.0, span);
        if (value < target) return Math.min(target, value + step);
        return Math.max(target, value - step);
    }
}
