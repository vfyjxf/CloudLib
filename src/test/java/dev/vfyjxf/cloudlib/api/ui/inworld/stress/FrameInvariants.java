package dev.vfyjxf.cloudlib.api.ui.inworld.stress;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.LayoutSpace;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The per-frame invariant oracle for a {@link StressScenario} run. Every
 * frame's {@link CoordinationResult} is checked against the coordinator's
 * documented contracts; the first violation throws an
 * {@link InvariantViolation} carrying the seed, the frame number and enough
 * context to reproduce — no softening, no retries.
 * <p>
 * <strong>Margins.</strong> Between two resolves the only inputs that move
 * are anchor projections — membership, exclusion, presentation and
 * past-threshold anchor changes all force a resolve — and dirty detection
 * guarantees each anchor has moved at most
 * {@code anchorDisplacementThresholdPx} since the last resolve. A committed
 * target may therefore have drifted out of the work area, into an exclusion
 * or into a neighbor by at most that drift; the checker prices exactly that
 * margin per element (zero on resolved frames, where targets were re-fitted
 * against the current frame). A viewport resize is not a renegotiation cause
 * (§3.2 leaves resizes to the next epoch tick), so work-area containment is
 * enforced again only from the first resolved frame after a resize.
 * <p>
 * <strong>Continuity.</strong> Per-frame visual movement is bounded by
 * {@code max(relaxDisplacementClampPx, targetDistance * dt / flip min
 * duration)}: the displacement clamp bounds spring-follow frames, and a FLIP
 * morph — the only sanctioned large move — advances at most
 * {@code travel * dt / minDuration} per frame with travel bounded by the
 * distance from the last visual to the current target. The bound is thus
 * tied to the actual geometry, not to the screen size. The single documented
 * snap exception is an element's first visual ever (it had nothing to move
 * from).
 * <p>
 * <strong>Size morphs.</strong> Per-frame size change is bounded by the
 * fastest possible FLIP interpolation rate (diagonals times dt over the
 * minimum flip duration — a one-frame full-size swap fails it), and any size
 * change must fall within a bounded horizon after a variant level change —
 * sizes move through the ladder, never as free-floating jumps.
 * <strong>Policy exemptions.</strong> Fixed and ghost variants are exempt
 * from containment/exclusion/pairwise checks — that exemption is the
 * documented SpacePolicy contract, not a checker loophole. World-only
 * elements are exempt from every screen check by the same token (they claim
 * no screen rect at all); their own contract — a presented world-only
 * element carries a world box and no screen visual — is enforced instead.
 */
final class FrameInvariants {

    private static final double geometryEpsilon = 1.0e-6;
    /**
     * The collision substrate is integer gui pixels — the occupancy bitmap
     * quantizes to 8px cells and float rects enter it through
     * {@code Math.round} — so a rect the bitmap accepted may really penetrate
     * another by just under one gui pixel. The checker tolerates exactly that
     * resolution and nothing more; overlaps beyond one pixel are violations.
     */
    private static final double penetrationEpsilon = 1.0;

    private static final double offsetEpsilon = 0.5;
    private static final double sizeEpsilon = 0.5;
    private static final double flipMaxDurationSeconds = 0.25;
    private static final double envelopeSlackPx = 2.0;
    private double previousDiagonal;
    private static final int levelChangeMorphHorizonFrames = 30;

    private final StressScenario.Spec spec;
    private final InworldCoordinator.Config config;
    private final Map<String, ElementTrace> traces = new HashMap<>();
    private Map<String, FloatPos> anchorsAtLastResolve = Map.of();

    private long lastFrame;
    private long lastEpoch;
    private boolean hasPrevFrame;
    private boolean resizeGracePending;

    FrameInvariants(StressScenario.Spec spec, InworldCoordinator.Config config) {
        this.spec = spec;
        this.config = config;
    }

    void check(StressScenario.FrameRecord record) {
        CoordinationResult result = record.result();
        double dt = record.input().dtSeconds();

        checkLiveness(record);

        FloatRect workArea = workArea(record);
        boolean resolved = result.resolved();
        // the truthfulness check and the drift margins must compare against
        // the anchors of the PREVIOUS resolve, so capture before swapping in
        // this frame's snapshot
        Map<String, FloatPos> previousResolveAnchors = anchorsAtLastResolve;
        if (record.resized()) {
            resizeGracePending = true;
        }
        boolean skipWorkArea = resizeGracePending && !resolved;
        if (resolved) {
            resizeGracePending = false;
            anchorsAtLastResolve = record.anchors();
        }

        List<Presented> presented = new ArrayList<>(result.elementStates().size());
        for (CoordinationResult.ElementState state : result.elementStates()) {
            String id = state.elementId();
            ElementTrace trace = traces.computeIfAbsent(id, key -> new ElementTrace());
            InworldPlacement placement = result.placementOf(id);
            boolean isPresented = placement != null;
            if (record.worldOnlyIds().contains(id)) {
                // a world-only element holds no screen estate: its grant is
                // the world box only — no containment, exclusion, pairwise
                // or continuity contract applies to a screen half it never
                // claimed. What must hold: the world part is present and no
                // screen visual was invented for it.
                if (isPresented) {
                    if (placement.world() == null) {
                        throw violation(record, "world-only-world-part", id + " presented without a world box");
                    }
                    if (state.visualRect() != null) {
                        throw violation(record, "world-only-no-screen-visual", id + " carries a screen visual rect");
                    }
                }
            } else if (isPresented) {
                SpacePolicy policy = placement.variant().spacePolicy();
                double margin = resolved ? 0.0 : driftSinceResolve(id, previousResolveAnchors, record);
                FloatRect rect = placement.screenRect();
                if (!policy.occlusionExempt()) {
                    if (!skipWorkArea && outsideDepth(rect, workArea) > margin + penetrationEpsilon) {
                        throw violation(
                            record,
                            "workarea-containment",
                            id + " rect " + rect + " sticks " + outsideDepth(rect, workArea) + "px out of " + workArea
                                    + " (drift margin " + margin + ")"
                        );
                    }
                    for (Rect exclusion : record.input().exclusionRects()) {
                        double depth = penetration(rect, toFloat(exclusion));
                        if (depth > margin + penetrationEpsilon) {
                            throw violation(
                                record,
                                "exclusion-disjoint",
                                id + " rect " + rect + " penetrates exclusion " + exclusion + " by " + depth
                                        + "px (drift margin " + margin + ")"
                            );
                        }
                    }
                }
                presented.add(new Presented(id, rect, policy.occlusionExempt(), margin));
                checkContinuity(record, trace, state, dt);
            }
            checkStaticWindow(record, trace, isPresented, placement);
            updateTrace(trace, isPresented, placement, state, dt);
        }

        for (int i = 0; i < presented.size(); i++) {
            for (int j = i + 1; j < presented.size(); j++) {
                Presented a = presented.get(i);
                Presented b = presented.get(j);
                if (a.exempt || b.exempt) {
                    continue;
                }
                double depth = penetration(a.rect, b.rect);
                if (depth > a.margin + b.margin + penetrationEpsilon) {
                    throw violation(
                        record,
                        "pairwise-disjoint",
                        a.id + " " + a.rect + " overlaps " + b.id + " " + b.rect + " by " + depth + "px (drift margins "
                                + a.margin + "+" + b.margin + ")"
                    );
                }
            }
        }

        checkAnchorCauseTruthfulness(record, previousResolveAnchors);

        lastFrame = result.frame();
        lastEpoch = result.epoch();
        hasPrevFrame = true;
        previousDiagonal = Math.hypot(record.input().screenWidth(), record.input().screenHeight());
    }

    // region individual invariants

    private void checkLiveness(StressScenario.FrameRecord record) {
        CoordinationResult result = record.result();
        if (record.frameIndex() + 1 != result.frame()) {
            throw violation(
                record,
                "frame-numbering",
                "expected " + (record.frameIndex() + 1) + " but was " + result.frame()
            );
        }
        if (hasPrevFrame && result.frame() != lastFrame + 1) {
            throw violation(record, "frame-numbering", "frames skipped: " + lastFrame + " -> " + result.frame());
        }
        if (result.epoch() < lastEpoch || (result.epoch() > lastEpoch && !result.resolved())) {
            throw violation(
                record,
                "epoch-monotonic",
                "epoch " + result.epoch() + " after " + lastEpoch + " without a resolve"
            );
        }
        Set<String> registered = new HashSet<>(record.registeredIds());
        Set<String> seen = new HashSet<>();
        for (CoordinationResult.ElementState state : result.elementStates()) {
            if (!registered.contains(state.elementId())) {
                throw violation(record, "states-cover-registration", state.elementId() + " is not registered");
            }
            if (!seen.add(state.elementId())) {
                throw violation(record, "states-cover-registration", state.elementId() + " reported twice");
            }
        }
        if (seen.size() != registered.size()) {
            registered.removeAll(seen);
            throw violation(record, "states-cover-registration", "missing from result: " + registered);
        }
        Set<String> placed = new HashSet<>();
        for (InworldPlacement placement : result.placements()) {
            if (!placed.add(placement.elementId())) {
                throw violation(record, "placement-unique", placement.elementId() + " placed twice");
            }
            if (!seen.contains(placement.elementId())) {
                throw violation(record, "placement-unique", placement.elementId() + " placed without a state");
            }
        }
        double freeFraction = result.budget().freeFraction();
        if (freeFraction < -geometryEpsilon || freeFraction > 1 + geometryEpsilon) {
            throw violation(record, "budget-range", "freeFraction " + freeFraction);
        }
    }

    private void checkContinuity(
        StressScenario.FrameRecord record,
        ElementTrace trace,
        CoordinationResult.ElementState state,
        double dt
    ) {
        FloatRect visual = state.visualRect();
        if (visual == null) {
            throw violation(record, "visual-continuity", state.elementId() + " presented without a visual rect");
        }
        if (trace.lastVisual == null) {
            return; // documented snap-in: the element's first visual ever
        }
        // A legitimate frame either spring-follows (bounded by the
        // displacement clamp) or FLIP-morphs. A morph's duration is
        // clamp(travel/speed, min, max), so its per-frame advance never
        // exceeds travel * dt / maxDuration, with travel bounded by the old
        // plus new screen diagonal (both endpoints lie on screen); the
        // amplitude-based bound also covers the completion spike of linear
        // interpolation.
        double diagonal = Math.hypot(record.input().screenWidth(), record.input().screenHeight());
        double movementEnvelope = Math
                .max(config.relaxDisplacementClampPx(), (previousDiagonal + diagonal) * dt / flipMaxDurationSeconds)
                + envelopeSlackPx;
        double movement = Math
                .hypot(visual.centerX() - trace.lastVisual.centerX(), visual.centerY() - trace.lastVisual.centerY());
        if (movement > movementEnvelope) {
            throw violation(
                record,
                "visual-continuity",
                state.elementId() + " moved " + movement + "px in one frame (envelope " + movementEnvelope + "): "
                        + trace.lastVisual + " -> " + visual
            );
        }
        double widthChange = Math.abs(visual.width() - trace.lastVisual.width());
        double heightChange = Math.abs(visual.height() - trace.lastVisual.height());
        double sizeChange = Math.max(widthChange, heightChange);
        if (sizeChange > sizeEpsilon) {
            // a FLIP interpolates size linearly with the same
            // amplitude-bound advance rate; anything faster is a snap
            double sizeEnvelope = (previousDiagonal + diagonal) * dt / flipMaxDurationSeconds + envelopeSlackPx;
            if (sizeChange > sizeEnvelope) {
                throw violation(
                    record,
                    "size-morph-rate",
                    state.elementId() + " size changed by " + sizeChange + "px in one frame (envelope " + sizeEnvelope
                            + "): " + trace.lastVisual + " -> " + visual
                );
            }
            if (trace.framesSinceLevelChange > levelChangeMorphHorizonFrames) {
                throw violation(
                    record,
                    "size-morph-gated",
                    state.elementId() + " size changed " + trace.framesSinceLevelChange
                            + " frames after the last level change: " + trace.lastVisual + " -> " + visual
                );
            }
        }
        InworldPlacement placement = state.placement();
        if (trace.lastOffset != null && placement != null) {
            if (!rectsAlmostEqual(trace.lastOffset, placement.offsetRect(), offsetEpsilon)
                    && !record.result().resolved()) {
                throw violation(
                    record,
                    "offset-change-requires-resolve",
                    state.elementId() + " target offset changed on a non-resolved frame: " + trace.lastOffset + " -> "
                            + placement.offsetRect()
                );
            }
        }
    }

    private void checkStaticWindow(
        StressScenario.FrameRecord record,
        ElementTrace trace,
        boolean presented,
        InworldPlacement placement
    ) {
        if (!record.strictStatic()) {
            return;
        }
        CoordinationResult.RenegotiationCause cause = record.result().cause();
        if (cause != CoordinationResult.RenegotiationCause.none
                && cause != CoordinationResult.RenegotiationCause.epochElapsed) {
            throw violation(record, "static-window-cause", "cause " + cause + " in the static tail");
        }
        if (trace.everTracked && presented != trace.lastPresented) {
            throw violation(record, "static-window-stability", "presentation flipped to " + presented);
        }
        if (presented && placement != null) {
            if (trace.lastLevel >= 0 && placement.variant().level() != trace.lastLevel) {
                throw violation(
                    record,
                    "static-window-stability",
                    "level " + trace.lastLevel + " -> " + placement.variant().level()
                );
            }
            if (trace.lastOffset != null
                    && !rectsAlmostEqual(trace.lastOffset, placement.offsetRect(), offsetEpsilon)) {
                throw violation(
                    record,
                    "static-window-stability",
                    "offset " + trace.lastOffset + " -> " + placement.offsetRect()
                );
            }
        }
    }

    private void checkAnchorCauseTruthfulness(
        StressScenario.FrameRecord record,
        Map<String, FloatPos> previousResolveAnchors
    ) {
        if (record.result().cause() != CoordinationResult.RenegotiationCause.anchorDisplacement) {
            return;
        }
        double maxDrift = 0;
        for (Map.Entry<String, FloatPos> entry : record.anchors().entrySet()) {
            FloatPos before = previousResolveAnchors.get(entry.getKey());
            if (before != null) {
                maxDrift = Math.max(
                    maxDrift,
                    Math.hypot(entry.getValue().x() - before.x(), entry.getValue().y() - before.y())
                );
            }
        }
        if (maxDrift <= config.anchorDisplacementThresholdPx() - 1.0e-6) {
            throw violation(
                record,
                "anchor-cause-truthfulness",
                "anchorDisplacement claimed with max drift " + maxDrift + "px, threshold is "
                        + config.anchorDisplacementThresholdPx()
            );
        }
    }

    // endregion

    // region bookkeeping

    private double driftSinceResolve(
        String id,
        Map<String, FloatPos> previousResolveAnchors,
        StressScenario.FrameRecord record
    ) {
        FloatPos before = previousResolveAnchors.get(id);
        FloatPos now = record.anchors().get(id);
        if (before == null || now == null) {
            return 0.0;
        }
        return Math.hypot(now.x() - before.x(), now.y() - before.y());
    }

    private void updateTrace(
        ElementTrace trace,
        boolean presented,
        InworldPlacement placement,
        CoordinationResult.ElementState state,
        double dt
    ) {
        trace.everTracked = true;
        trace.lastPresented = presented;
        trace.lastVisual = state.visualRect();
        int level = placement == null ? trace.lastLevel : placement.variant().level();
        if (trace.lastLevel >= 0 && level != trace.lastLevel) {
            trace.framesSinceLevelChange = 0;
        } else {
            trace.framesSinceLevelChange++;
        }
        trace.lastLevel = level;
        if (presented && placement != null) {
            trace.lastOffset = placement.offsetRect();
        }
    }

    private FloatRect workArea(StressScenario.FrameRecord record) {
        Rect workArea = LayoutSpace.of(record.input().screenWidth(), record.input().screenHeight())
                .withStruts(record.input().exclusionRects()).workArea();
        return toFloat(workArea);
    }

    private InvariantViolation violation(StressScenario.FrameRecord record, String invariant, String detail) {
        return new InvariantViolation(
            "invariant [" + invariant + "] failed at frame " + (record.frameIndex() + 1) + "/" + spec.frames() + " — "
                    + detail + "\nreproduce with spec: " + spec,
            spec.seed(),
            record.frameIndex() + 1
        );
    }

    // endregion

    // region helpers

    private static double penetration(FloatRect a, FloatRect b) {
        double px = Math.min(a.right(), b.right()) - Math.max(a.x(), b.x());
        double py = Math.min(a.bottom(), b.bottom()) - Math.max(a.y(), b.y());
        if (px <= 0 || py <= 0) {
            return 0;
        }
        return Math.min(px, py);
    }

    private static double outsideDepth(FloatRect rect, FloatRect bounds) {
        double left = bounds.x() - rect.x();
        double top = bounds.y() - rect.y();
        double right = rect.right() - bounds.right();
        double bottom = rect.bottom() - bounds.bottom();
        return Math.max(0, Math.max(Math.max(left, right), Math.max(top, bottom)));
    }

    private static boolean rectsAlmostEqual(FloatRect a, FloatRect b, double epsilon) {
        return Math.abs(a.x() - b.x()) <= epsilon
                && Math.abs(a.y() - b.y()) <= epsilon
                && Math.abs(a.width() - b.width()) <= epsilon
                && Math.abs(a.height() - b.height()) <= epsilon;
    }

    private static FloatRect toFloat(Rect rect) {
        return new FloatRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    // endregion

    private record Presented(String id, FloatRect rect, boolean exempt, double margin) {}

    private static final class ElementTrace {
        boolean everTracked;
        boolean lastPresented;
        FloatRect lastVisual;
        FloatRect lastOffset;
        int lastLevel = -1;
        int framesSinceLevelChange = Integer.MAX_VALUE / 4;
    }

    /** The failure signal: seed + first violating frame + the broken invariant. */
    static final class InvariantViolation extends AssertionError {

        private final long seed;
        private final int frame;

        InvariantViolation(String message, long seed, int frame) {
            super(message);
            this.seed = seed;
            this.frame = frame;
        }

        long seed() {
            return seed;
        }

        int frame() {
            return frame;
        }
    }
}
