package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.FreeRectIndex;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.LayoutSpace;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.OccupancyBitmap;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.FlipPlanner;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.Smoothing;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.Spring2;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.PreviousFrameLayout;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The coordinator (§3.2): the single entry that registers
 * {@link InworldElement}s, drives one layout decision per frame and answers
 * placement queries. Every frame is the seven-phase pipeline
 * <pre>
 * Collect (propose with SpaceBudget feedback) → SpaceBuild (exclusion/occupancy sampling)
 * → Resolve (discrete layer; dirty or 4–10 Hz epoch only) → Relax (continuous layer:
 * warm-start spring follow, clamped residual-overlap pushes) → Stabilize (discrete
 * decisions through the triple gate, continuous quantities through the visibility
 * tracker) → Commit+Animate (atomic result + FLIP/alpha) → Renegotiate judgment
 * (projected-displacement threshold, membership, exclusion change, epoch tick)
 * </pre>
 * with immutable records handed between phases, and the whole frame's outcome
 * committed atomically — an element never observes a half-resolved layout.
 * <p>
 * The arbitration order is a total order fixed for the frame:
 * {@code spaceKind (world → tracked → panel), then priority (desc), then
 * sticky-first, then registration order}. No hash iteration and no randomness
 * participate anywhere, so equal inputs always produce equal results. The
 * same order releases coinciding targets: a fixed variant granted on top of a
 * non-pushable element committed earlier in the round walks its preference
 * list to its first non-coinciding candidate — the pair could never separate
 * any other way (both are exempt from the bitmap veto and neither is
 * pushable), and the deterministic winner keeps the assignment stable across
 * resolves instead of alternating.
 * <p>
 * Negotiation: round 0, everyone proposes the rung the coordinator hands them
 * (upgraded one step on epoch boundaries when below the strongest), and an
 * element that is already on screen falls back to the placement it holds when
 * the handed rung does not fit — a failed upgrade never costs it the slot, so
 * a settled scene is a fixed point of the epoch re-resolve instead of
 * alternating between two layouts. Round 1, only round-0 rejects re-propose,
 * walking their {@link VariantLadder} down as rejections dictate — accepted
 * elements are frozen. Round 1 is bounded by the ladder length, so the frame
 * always ends with every element either granted a placement or hidden
 * (deadline: forced commit; hidden elements linger instead of blinking out).
 * <p>
 * World-only capability ({@link InworldElement#worldOnly()}): an element that
 * lives purely in world space leaves this whole arbitration flow. It still
 * proposes each frame; the first candidate carrying a world box is granted
 * unconditionally, and a proposal without one (or a retraction) retracts. It
 * never marks the occupancy bitmap, never enters the space budget, never
 * negotiates, upgrades or relaxes, and its presentation changes re-resolve
 * nothing — the arbitration outcome of every projecting element is exactly
 * what it would be without the world-only element registered. It still
 * receives an {@code ElementState} every frame and drives the visibility
 * tracker like any other element; its grant carries only the world half.
 * <p>
 * Zone support (Z2): after every commit, when at least one registered
 * element {@link InworldElement#consumesZoneLayout() consumes zone layout},
 * the coordinator derives the previous frame's committed layout — placement
 * map, leader segments, left-of/above adjacency — and exposes it via
 * {@link #previousZoneLayout()} for the next frame's element-side zone
 * scoring. The snapshot never feeds back into arbitration: it is a read-only
 * view of what was committed, and a population without zone consumers
 * computes nothing, so their frames are byte-identical to a run without the
 * zone layer.
 * <p>
 * Purity: no Minecraft types, no wall clock. Time ({@code nowSeconds},
 * {@code dtSeconds}) is an explicit frame input. Time should be monotonic
 * across frames; non-monotonic input is defensively tolerated (components
 * clamp rewound time to the last seen value and negative dt to zero) so a
 * glitching upstream clock cannot crash the render thread.
 */
public final class InworldCoordinator {

    /**
     * The sticky slot match tolerance, in offset space: candidate and
     * incumbent are both compared as offsets from the anchor (the placement
     * contract's own storage), so an anchor that moved between resolves
     * cannot break the match by itself. The tolerance only absorbs the
     * candidate generator's rounding (integer-pixel lattices drift up to
     * 1 px per axis) — real lattice steps are {@code >=} the tier clearances
     * (8 px near), far above it, so a neighbor slot can never match.
     */
    private static final double stickySlotEpsilonPx = 2.0;

    private static final double discreteRectEpsilonPx = 0.5;
    private static final double outOfBoundsEpsilon = 1.0e-6;
    /**
     * The stacking threshold of the ordered release: an intersection covering
     * this fraction of the smaller rect is a coinciding target the arbitration
     * must break (below it the graze stays tolerated — the exempt contract).
     */
    private static final double coincidenceFraction = 0.5;

    private final Config config;
    private final Map<String, ElementRuntime> runtimes = new LinkedHashMap<>();

    private long registrationCounter;
    private long frameCounter;
    private long epochCounter;
    private double lastResolveTime = Double.NEGATIVE_INFINITY;
    private boolean membershipDirty = true;
    private List<Rect> lastExclusions = List.of();
    private double smoothedFreeFraction = Double.NaN;
    private @Nullable CoordinationResult lastResult;
    private @Nullable PreviousFrameLayout zoneLayout;

    /**
     * @param config every threshold and rate the pipeline uses; see
     *        {@link Config#defaults()}
     */
    public InworldCoordinator(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /** A coordinator with {@link Config#defaults()}. */
    public static InworldCoordinator withDefaults() {
        return new InworldCoordinator(Config.defaults());
    }

    // region lifecycle

    /**
     * Registers an element. Takes effect at the next frame (a membership
     * change is a renegotiation cause). Registering a world-only element is
     * not a renegotiation cause: it never touches the arbitration inputs, so
     * the projecting elements' outcome stays byte-identical to a run without
     * it.
     *
     * @throws IllegalArgumentException if the id is already registered
     */
    public void register(InworldElement element) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(element.id(), "element id");
        if (runtimes.containsKey(element.id())) {
            throw new IllegalArgumentException("element id already registered: " + element.id());
        }
        runtimes.put(element.id(), new ElementRuntime(element, registrationCounter++, config));
        if (!element.worldOnly()) {
            membershipDirty = true;
        }
    }

    /**
     * Unregisters an element; its placement is gone from the next frame on
     * (deregistration does not linger — lingering is for rejections and
     * retracted anchors). Unregistering a world-only element is not a
     * renegotiation cause.
     *
     * @return whether such an element was registered
     */
    public boolean unregister(String elementId) {
        ElementRuntime runtime = runtimes.remove(Objects.requireNonNull(elementId, "elementId"));
        if (runtime == null) {
            return false;
        }
        if (!runtime.element.worldOnly()) {
            membershipDirty = true;
        }
        return true;
    }

    /** The registered elements in registration order. */
    public List<InworldElement> elements() {
        List<InworldElement> elements = new ArrayList<>(runtimes.size());
        for (ElementRuntime runtime : runtimes.values()) {
            elements.add(runtime.element);
        }
        return List.copyOf(elements);
    }

    /**
     * The registered elements in the current arbitration order. World-only
     * elements appear here too — the order is their deterministic place in
     * the result — but they never enter the arbitration flow itself (their
     * arbitration indices continue after the arbitrated elements').
     */
    public List<InworldElement> arbitrationOrderSnapshot() {
        List<InworldElement> order = new ArrayList<>(runtimes.size());
        for (ElementRuntime runtime : orderedRuntimes()) {
            order.add(runtime.element);
        }
        return List.copyOf(order);
    }

    /**
     * The element's placement from the last committed frame, if presented.
     */
    public Optional<InworldPlacement> placementOf(String elementId) {
        Objects.requireNonNull(elementId, "elementId");
        return lastResult == null ? Optional.empty() : Optional.ofNullable(lastResult.placementOf(elementId));
    }

    /** The last committed frame's result, if any frame has run. */
    public Optional<CoordinationResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    /**
     * The last committed frame's layout in zone vocabulary — the placement
     * map, the committed leaders and the adjacency tables — for the driving
     * adapter to hand to the next frame's {@code LayoutEnvironment}. Empty
     * when no frame has run or when no registered element consumes zone
     * layout: the snapshot is computed only for zone-consuming populations,
     * so everyone else's frames are byte-identical to a run without the
     * zone layer.
     */
    public Optional<PreviousFrameLayout> previousZoneLayout() {
        return Optional.ofNullable(zoneLayout);
    }

    /** The current decision epoch (incremented on every resolve). */
    public long epoch() {
        return epochCounter;
    }

    /** How many frames have been driven. */
    public long frameCount() {
        return frameCounter;
    }

    // endregion

    // region frame

    /**
     * Drives one frame of the pipeline.
     *
     * @throws IllegalArgumentException if the input is malformed or an element
     *         breaches the propose contract (invalid proposal records,
     *         out-of-ladder variants)
     */
    public CoordinationResult frame(FrameInput frame) {
        frameCounter++;
        List<ElementRuntime> ordered = orderedRuntimes();
        // The capability split: world-only elements leave the arbitration
        // flow entirely — no bitmap, no budget, no negotiation, no
        // relaxation. They still propose, still get element states and still
        // drive the visibility tracker, so they stay in the total order for
        // the committed result.
        List<ElementRuntime> arbitrated = new ArrayList<>(ordered.size());
        List<ElementRuntime> worldOnly = new ArrayList<>(ordered.size());
        for (ElementRuntime runtime : ordered) {
            if (runtime.element.worldOnly()) {
                worldOnly.add(runtime);
            } else {
                arbitrated.add(runtime);
            }
        }
        // Arbitration indices are dense over the arbitrated flow, so a
        // world-only element can never shift another element's arbitration
        // index; the world-only elements continue after it.
        int arbitrationCounter = 0;
        for (ElementRuntime runtime : arbitrated) {
            runtime.frameArbitrationIndex = arbitrationCounter++;
        }
        for (ElementRuntime runtime : worldOnly) {
            runtime.frameArbitrationIndex = arbitrationCounter++;
        }

        LayoutSpace space =
                LayoutSpace.of(frame.screenWidth(), frame.screenHeight()).withStruts(frame.exclusionRects());
        Rect workArea = space.workArea();

        // Renegotiate judgment, pre-collect half: membership, exclusions, epoch.
        boolean membership = membershipDirty;
        boolean exclusionsChanged = !sameRects(lastExclusions, frame.exclusionRects());
        boolean epochElapsed = frame.nowSeconds() >= lastResolveTime + config.epochSeconds();
        if (epochElapsed) {
            attemptUpgrades(arbitrated);
        }

        // SpaceBudget feedback: this frame's exclusions against last frame's
        // committed rects, smoothed so occupancy jitter never flaps tiers.
        // World-only targets mark nothing — they hold no screen estate.
        SpaceBudget budget = smoothedBudget(frame, workArea, arbitrated);

        // Collect: everyone proposes round 0. A world-only proposal is not a
        // negotiation input — its acceptance (below) is unconditional, so
        // the world-only flow has no rounds.
        for (ElementRuntime runtime : ordered) {
            ProposeContext context = new ProposeContext(
                    epochCounter, 0, budget, runtime.target, runtime.activeRejection, ladderVariant(runtime));
            runtime.roundZero = runtime.element.propose(context);
            if (runtime.roundZero.worldOnly() && !runtime.element.worldOnly()) {
                throw new IllegalArgumentException(
                        "element " + runtime.element.id() + " is not world-only but proposed a world-only proposal");
            }
        }

        // Renegotiate judgment, post-collect half: presentation recovery,
        // projected anchor displacement (the dirty rule of §3.5: an entity is
        // always moving in world space; only screen-space displacement past
        // the threshold counts). World-only elements arbitrate nothing, so
        // their presentations and anchors re-resolve nothing.
        boolean presentationChanged = false;
        boolean anchorDisplacement = false;
        for (ElementRuntime runtime : arbitrated) {
            boolean retractedNow = runtime.roundZero.retracted();
            if (runtime.retractedLastFrame && !retractedNow) {
                presentationChanged = true;
            }
            FloatPos anchor = runtime.roundZero.anchorScreen();
            if (!retractedNow
                    && anchor != null
                    && runtime.anchorAtLastResolve != null
                    && distance(anchor, runtime.anchorAtLastResolve) > config.anchorDisplacementThresholdPx()) {
                anchorDisplacement = true;
            }
            if (retractedNow) {
                // A retracted element contributes no anchor to the resolve, so
                // the last recorded one must be forgotten with it: a projection
                // from before the retraction is not a displacement the frame can
                // see, and holding on to it would let the recovery frame claim
                // anchorDisplacement for motion that never happened between two
                // resolves. The recovery is a resolve of its own (the
                // presentation change above), and it re-records the anchor.
                runtime.anchorAtLastResolve = null;
            }
            runtime.retractedLastFrame = retractedNow;
        }
        CoordinationResult.RenegotiationCause cause = membership
                ? CoordinationResult.RenegotiationCause.membershipChanged
                : exclusionsChanged
                        ? CoordinationResult.RenegotiationCause.exclusionsChanged
                        : presentationChanged
                                ? CoordinationResult.RenegotiationCause.presentationChanged
                                : anchorDisplacement
                                        ? CoordinationResult.RenegotiationCause.anchorDisplacement
                                        : epochElapsed
                                                ? CoordinationResult.RenegotiationCause.epochElapsed
                                                : CoordinationResult.RenegotiationCause.none;
        boolean resolveNow = cause != CoordinationResult.RenegotiationCause.none;

        // SpaceBuild: the round-local occupancy bitmap seeded with exclusions.
        OccupancyBitmap bitmap =
                new OccupancyBitmap(frame.screenWidth(), frame.screenHeight(), config.occupancyCellSize());
        bitmap.markAll(frame.exclusionRects());

        // Resolve: the discrete layer.
        if (resolveNow) {
            epochCounter++;
            lastResolveTime = frame.nowSeconds();
            membershipDirty = false;
            lastExclusions = List.copyOf(frame.exclusionRects());
            RoundScope scope = new RoundScope(bitmap, toFloat(workArea), frame.exclusionRects(), epochCounter);
            runNegotiation(arbitrated, scope, budget);
            for (ElementRuntime runtime : arbitrated) {
                if (runtime.roundZero.anchorScreen() != null) {
                    runtime.anchorAtLastResolve = new FloatPos(
                            runtime.roundZero.anchorScreen().x(),
                            runtime.roundZero.anchorScreen().y());
                }
            }
        } else {
            for (ElementRuntime runtime : arbitrated) {
                runtime.pendingPresent =
                        !runtime.roundZero.retracted() && runtime.lastPresented && runtime.target != null;
                if (runtime.roundZero.retracted()) {
                    runtime.activeRejection = null;
                }
            }
        }

        // The world-only acceptances: unconditional, every frame, resolve or
        // not. The grant carries only the world half; a proposal without a
        // world candidate retracts (state without placement).
        for (ElementRuntime runtime : worldOnly) {
            acceptWorldOnly(runtime);
        }

        // Relax + Stabilize: the continuous layer (spring follow, FLIP on
        // discrete changes, displacement clamp, residual-overlap pushes) and
        // the visibility tracker. World-only elements have no screen visual
        // to relax or separate.
        Map<ElementRuntime, FloatRect> visuals = new LinkedHashMap<>();
        for (ElementRuntime runtime : arbitrated) {
            if (!runtime.pendingPresent || runtime.target == null) {
                continue;
            }
            FloatRect targetRect = layoutTargetRect(runtime);
            boolean discrete = resolveNow
                    && (runtime.lastTargetRect == null || !rectsAlmostEqual(runtime.lastTargetRect, targetRect));
            if (runtime.flip == null || runtime.spring == null) {
                runtime.flip = new FlipPlanner(config.flipSpeedPixelsPerSecond(), targetRect);
                runtime.spring = new Spring2(config.springOmega(), targetRect.centerX(), targetRect.centerY());
            } else if (discrete) {
                runtime.flip.flipTo(targetRect, frame.nowSeconds());
                runtime.spring.snap(targetRect.centerX(), targetRect.centerY());
            }
            FloatRect visual;
            if (runtime.flip.isAnimating(frame.nowSeconds())) {
                visual = runtime.flip.visual(frame.nowSeconds());
            } else {
                runtime.spring.step(targetRect.centerX(), targetRect.centerY(), frame.dtSeconds());
                visual = FloatRect.around(
                        new FloatPos(runtime.spring.x(), runtime.spring.y()), targetRect.width(), targetRect.height());
            }
            visuals.put(runtime, visual);
        }
        separateOverlaps(visuals, toFloat(workArea));

        // Commit + Animate: one atomic result.
        List<InworldPlacement> placements = new ArrayList<>(ordered.size());
        List<CoordinationResult.ElementState> elementStates = new ArrayList<>(ordered.size());
        for (ElementRuntime runtime : ordered) {
            if (runtime.element.worldOnly()) {
                // The world-only grant has no screen half to animate, clamp
                // or separate — the world box is committed as proposed.
                if (runtime.pendingPresent && runtime.target != null) {
                    placements.add(runtime.target);
                }
            } else {
                boolean wasPresented = runtime.lastPresented;
                FloatRect unclamped = visuals.get(runtime);
                FloatRect visual = unclamped;
                if (runtime.pendingPresent && runtime.target != null && visual != null) {
                    if (runtime.lastVisual != null) {
                        visual = clampDisplacement(visual, runtime.lastVisual, config.relaxDisplacementClampPx());
                    }
                    carryRelaxation(runtime, unclamped, visual, frame.nowSeconds());
                    FloatPos anchor = runtime.roundZero.anchorScreen() != null
                            ? runtime.roundZero.anchorScreen()
                            : runtime.target.anchor();
                    runtime.lastVisual = visual;
                    runtime.lastTargetRect = runtime.target.offsetRect().translate(anchor.x(), anchor.y());
                    runtime.target = new InworldPlacement(
                            runtime.element.id(),
                            runtime.target.variant(),
                            anchor,
                            runtime.target.offsetRect(),
                            runtime.target.world(),
                            runtime.frameArbitrationIndex,
                            runtime.target.epoch());
                    placements.add(runtime.target);
                } else if (wasPresented && runtime.target != null && runtime.lastVisual != null) {
                    // The element just left the presented set. Commit its pending
                    // morph: nothing is animated off screen, and a half-applied
                    // morph held over a linger would resume a whole morph-late
                    // position/size change on the next presentation. The rect the
                    // element lingers at (and is later rescued from) is therefore
                    // the layout rect it was heading for.
                    FloatRect resting = layoutTargetRect(runtime);
                    runtime.flip.snap(resting);
                    runtime.spring.snap(resting.centerX(), resting.centerY());
                    runtime.lastVisual = resting;
                    runtime.lastTargetRect = resting;
                }
            }
            if (runtime.pendingPresent != runtime.lastPresented) {
                runtime.visibility.setPresent(runtime.pendingPresent, frame.nowSeconds());
                runtime.lastPresented = runtime.pendingPresent;
            }
            elementStates.add(new CoordinationResult.ElementState(
                    runtime.element.id(),
                    runtime.visibility.phase(frame.nowSeconds()),
                    runtime.visibility.alpha(frame.nowSeconds()),
                    runtime.target,
                    runtime.lastVisual,
                    runtime.pendingPresent ? null : runtime.activeRejection));
        }

        CoordinationResult result = new CoordinationResult(
                frameCounter, epochCounter, resolveNow, cause, placements, elementStates, budget);
        lastResult = result;
        // The zone snapshot (post-commit): the previous frame the zone ranker
        // scores against. Computed only when a registered element consumes
        // zone layout — a population without one never pays for it and the
        // snapshot stays null.
        zoneLayout = null;
        for (ElementRuntime runtime : ordered) {
            if (runtime.element.consumesZoneLayout()) {
                zoneLayout = PreviousFrameLayout.of(zonePlacements(placements));
                break;
            }
        }
        return result;
    }

    private static List<PreviousFrameLayout.Placement> zonePlacements(List<InworldPlacement> placements) {
        List<PreviousFrameLayout.Placement> snapshot = new ArrayList<>(placements.size());
        for (InworldPlacement placement : placements) {
            Rect rect = placement.screenRect().toRect();
            if (rect.width() <= 0 || rect.height() <= 0) {
                continue; // world-only grants carry no screen half
            }
            snapshot.add(new PreviousFrameLayout.Placement(placement.elementId(), placement.anchor(), rect));
        }
        return snapshot;
    }

    // endregion

    // region world-only flow

    /**
     * The world-only acceptance: the first candidate carrying a world box, in
     * the element's preference order, granted unconditionally — no fitRect,
     * no bitmap, no exclusion, no nudge, no ladder walk. A proposal without a
     * world candidate (or a retraction) retracts: state without placement,
     * the same linger semantics a retracted anchor gets. The grant carries
     * only the world half — the anchor is the origin and the offset rect
     * empty, so nothing downstream may read its screen rect as geometry.
     */
    private void acceptWorldOnly(ElementRuntime runtime) {
        ElementProposal proposal = runtime.roundZero;
        runtime.pendingPresent = false;
        runtime.activeRejection = null;
        if (proposal.retracted()) {
            return;
        }
        requireLadderRung(runtime, proposal.variant());
        runtime.currentLevel = proposal.variant().level();
        for (PlacementCandidate candidate : proposal.candidates()) {
            if (candidate.world() == null) {
                continue;
            }
            runtime.target = new InworldPlacement(
                    runtime.element.id(),
                    proposal.variant(),
                    new FloatPos(0, 0),
                    FloatRect.empty,
                    candidate.world(),
                    runtime.frameArbitrationIndex,
                    epochCounter);
            runtime.pendingPresent = true;
            return;
        }
    }

    // endregion

    // region negotiation

    private void runNegotiation(List<ElementRuntime> ordered, RoundScope scope, SpaceBudget budget) {
        Map<ElementRuntime, ElementRejection> rejected = new LinkedHashMap<>();

        // Round 0: everyone reports their handed (best current) form.
        for (ElementRuntime runtime : ordered) {
            ElementProposal proposal = runtime.roundZero;
            if (proposal.retracted()) {
                runtime.pendingPresent = false;
                runtime.activeRejection = null;
                continue;
            }
            requireLadderRung(runtime, proposal.variant());
            runtime.currentLevel = proposal.variant().level();
            Acceptance acceptance = tryAccept(runtime, proposal, scope);
            if (acceptance.placement == null) {
                acceptance = tryKeepIncumbent(runtime, proposal, scope, acceptance);
            }
            if (acceptance.placement != null) {
                commit(runtime, acceptance.placement, scope);
            } else {
                runtime.pendingPresent = false;
                rejected.put(runtime, acceptance.rejection);
            }
        }

        if (rejected.isEmpty() || config.maxRounds() < 2) {
            for (ElementRuntime runtime : rejected.keySet()) {
                runtime.activeRejection = rejected.get(runtime);
            }
            return;
        }

        // Round 1: only the rejected re-propose, walking their ladders down;
        // accepted elements are frozen. Self-managed elements never
        // renegotiate — a conflict verdict is final.
        for (Map.Entry<ElementRuntime, ElementRejection> entry : rejected.entrySet()) {
            ElementRuntime runtime = entry.getKey();
            if (runtime.element.mode() == ElementMode.selfManaged) {
                runtime.activeRejection = entry.getValue();
                continue;
            }
            ElementRejection rejection = entry.getValue();
            while (true) {
                InworldVariant degraded = runtime.element.ladder().degrade(ladderVariant(runtime), rejection.reason());
                if (degraded == null) {
                    runtime.activeRejection = rejection;
                    break;
                }
                ProposeContext context =
                        new ProposeContext(scope.epoch, 1, budget, runtime.target, rejection, degraded);
                ElementProposal proposal = runtime.element.propose(context);
                if (proposal.retracted()) {
                    runtime.activeRejection = null;
                    break;
                }
                requireLadderRung(runtime, proposal.variant());
                if (proposal.variant().level() <= runtime.currentLevel) {
                    // the protocol demands strict degradation within a round
                    runtime.activeRejection = rejection;
                    break;
                }
                runtime.currentLevel = proposal.variant().level();
                Acceptance acceptance = tryAccept(runtime, proposal, scope);
                if (acceptance.placement != null) {
                    commit(runtime, acceptance.placement, scope);
                    runtime.activeRejection = null;
                    break;
                }
                rejection = acceptance.rejection;
                if (runtime.element.ladder().isWeakest(proposal.variant())) {
                    runtime.activeRejection = rejection;
                    break;
                }
            }
        }
    }

    /**
     * The stability fallback of round 0: an element that is on screen and
     * cannot fit the rung the coordinator handed it keeps the placement it
     * already holds, as long as that still fits. Leaving it to round 1 is not
     * enough — the re-proposal happens after every other element has been
     * considered, so a later element that fits steals the space the incumbent
     * needs and a failed upgrade costs the element its slot. The outcome then
     * alternates with the handed rung: the element wins on one epoch, loses on
     * the next, and a settled scene never stops reshuffling. Keeping the
     * incumbent at the element's turn in the arbitration order removes that
     * dependency. Only elements that are currently presented fall back — an
     * element that is already hidden re-earns its place through the ladder.
     */
    private Acceptance tryKeepIncumbent(
            ElementRuntime runtime, ElementProposal proposal, RoundScope scope, Acceptance rejected) {
        if (!runtime.lastPresented || runtime.target == null) {
            return rejected;
        }
        FloatPos anchor = proposal.anchorScreen();
        if (anchor == null) {
            return rejected;
        }
        if (!incumbentStillFits(runtime.target.offsetRect().translate(anchor.x(), anchor.y()), runtime, scope)) {
            return rejected;
        }
        return Acceptance.accepted(new InworldPlacement(
                runtime.element.id(),
                runtime.target.variant(),
                anchor,
                runtime.target.offsetRect(),
                runtime.target.world(),
                runtime.frameArbitrationIndex,
                scope.epoch));
    }

    private void commit(ElementRuntime runtime, InworldPlacement placement, RoundScope scope) {
        runtime.target = placement;
        runtime.pendingPresent = true;
        runtime.activeRejection = null;
        runtime.currentLevel = placement.variant().level();
        if (placement.variant().spacePolicy() != SpacePolicy.ghost) {
            scope.bitmap.mark(placement.screenRect().toRect());
        }
        scope.committed.add(Committed.of(
                runtime.element.id(),
                placement.screenRect(),
                placement.variant().spacePolicy().pushable()));
        if (runtime.gate == null) {
            runtime.gate =
                    new SwitchGate<>(config.switchGate(), placement.variant().level(), 0.0);
        }
    }

    /**
     * Picks the granted candidate: the sticky incumbent first when it is
     * among the candidates and still fits, else the element's preference
     * order — then, for a fixed variant whose choice lands on top of a
     * non-pushable element committed earlier this round, the ordered release
     * walks the preference list past the coinciding candidates; finally the
     * discrete variant switch is gated (the triple gate keeps small tier
     * oscillations from committing, unless survival forces the switch).
     */
    private Acceptance tryAccept(ElementRuntime runtime, ElementProposal proposal, RoundScope scope) {
        InworldVariant variant = proposal.variant();
        FloatPos anchor = Objects.requireNonNull(proposal.anchorScreen(), "anchorScreen");

        FloatRect chosenRect = null;
        PlacementCandidate chosen = null;
        if (runtime.element.sticky() && runtime.target != null) {
            // the sticky match is offset-from-anchor, not absolute: candidates
            // dock to their anchor (the lattice translates with it), and the
            // incumbent is stored as exactly such an offset — comparing in
            // offset space means the granted slot survives every anchor motion
            // between resolves (a walking entity, a swaying camera), and only
            // a real re-rank cause (the slot stopped fitting, the element
            // re-registered, the preference itself changed) can move the
            // element off it. An absolute-rect match would de-pin on every
            // resolve once the anchor had moved past the epsilon, handing the
            // pick back to the ranker — a moving anchor would re-argue its
            // slot at every resolve and visibly hop.
            for (PlacementCandidate candidate : proposal.candidates()) {
                FloatRect candidateOffset = candidate.screenRect().translate(-anchor.x(), -anchor.y());
                if (rectsAlmostEqual(candidateOffset, runtime.target.offsetRect(), stickySlotEpsilonPx)) {
                    Fit fit = fitRect(candidate, variant, scope);
                    if (fit.ok()) {
                        chosen = candidate;
                        chosenRect = fit.rect();
                    }
                    break;
                }
            }
        }
        Fit firstFailure = null;
        if (chosen == null) {
            for (PlacementCandidate candidate : proposal.candidates()) {
                Fit fit = fitRect(candidate, variant, scope);
                if (fit.ok()) {
                    chosen = candidate;
                    chosenRect = fit.rect();
                    break;
                }
                if (firstFailure == null) {
                    firstFailure = fit;
                }
            }
        }
        if (chosen != null && chosenRect != null) {
            // The ordered release of coinciding targets: a fixed element is
            // granted its preferred candidate without a bitmap veto, so two
            // non-pushable elements whose preference orders agree (co-anchored
            // panels) both take the same slot — the next resolve's overlap
            // scoring pushes them in lockstep onto a mirrored slot and back,
            // alternating every resolve: the targets coincide forever, the
            // pair flickers and never separates. When at least one side is
            // pushable the continuous layer separates them (the exempt
            // contract stands); when both are non-pushable nothing else ever
            // will, so the arbitration order must break the tie the way it
            // breaks every other contention: the element committed earlier in
            // the round keeps the contested slot, and this element walks its
            // own preference list to its first candidate that does not
            // coincide — still its own geometry, no degradation, no
            // coordinator-invented rect. While the inputs hold, every resolve
            // produces the same deterministic assignment (the flicker stops);
            // once the environment allows — the anchors separate — the walk
            // finds nothing to avoid and the element takes its best slot
            // again. Grazes below the coincidence fraction stay tolerated,
            // and an element every one of whose candidates coincides keeps
            // its preferred one (no starvation).
            PlacementCandidate released = releaseCoincidence(proposal, variant, scope, chosen, chosenRect);
            if (released != chosen) {
                chosen = released;
                chosenRect = fitRect(chosen, variant, scope).rect();
            }
        }
        if (chosen == null || chosenRect == null) {
            return Acceptance.rejected(toRejection(firstFailure, variant, scope));
        }

        InworldPlacement placement = new InworldPlacement(
                runtime.element.id(),
                variant,
                anchor,
                new FloatRect(
                        chosenRect.x() - anchor.x(),
                        chosenRect.y() - anchor.y(),
                        chosenRect.width(),
                        chosenRect.height()),
                chosen.world(),
                runtime.frameArbitrationIndex,
                scope.epoch);

        // Stabilize (discrete half): the tier switch goes through the gate.
        if (runtime.gate != null && variant.level() != runtime.gate.current()) {
            double metric = switchMetric(runtime, anchor, placement.screenRect());
            boolean switched = runtime.gate.propose(variant.level(), metric);
            if (!switched) {
                FloatRect incumbent = runtime.target.offsetRect().translate(anchor.x(), anchor.y());
                if (incumbentStillFits(incumbent, runtime, scope)) {
                    InworldPlacement kept = new InworldPlacement(
                            runtime.element.id(),
                            runtime.target.variant(),
                            anchor,
                            runtime.target.offsetRect(),
                            runtime.target.world(),
                            runtime.frameArbitrationIndex,
                            runtime.target.epoch());
                    return Acceptance.accepted(kept);
                }
                // survival overrides stability: reset the gate at the forced level
                runtime.gate = new SwitchGate<>(config.switchGate(), variant.level(), metric);
            }
        }
        return Acceptance.accepted(placement);
    }

    /**
     * The coincidence test of the ordered release: the intersection covers at
     * least {@link #coincidenceFraction} of the smaller rect — stacking, not
     * grazing — against a committed rect whose element nothing can push.
     */
    private static boolean coincidesWithNonPushable(FloatRect rect, RoundScope scope) {
        for (Committed committed : scope.committed) {
            if (committed.pushable()) {
                continue; // the continuous layer separates pushable pairs
            }
            double ix = Math.min(rect.right(), committed.rect().right())
                    - Math.max(rect.x(), committed.rect().x());
            double iy = Math.min(rect.bottom(), committed.rect().bottom())
                    - Math.max(rect.y(), committed.rect().y());
            if (ix <= 0 || iy <= 0) {
                continue;
            }
            double smaller = Math.min(rectArea(rect), rectArea(committed.rect()));
            if (smaller > 0 && ix * iy / smaller >= coincidenceFraction) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the proposal's preference order past candidates that coincide
     * with non-pushable commits made earlier this round. Returns the
     * preferred candidate unchanged when the variant is not fixed (the
     * bitmap veto already rules non-exempt candidates, and ghosts are
     * invisible to arbitration), when the preferred rect coincides with
     * nothing, or when every candidate coincides (no starvation — the
     * preferred one stands).
     */
    private PlacementCandidate releaseCoincidence(
            ElementProposal proposal,
            InworldVariant variant,
            RoundScope scope,
            PlacementCandidate preferred,
            FloatRect preferredRect) {
        if (variant.spacePolicy() != SpacePolicy.fixed) {
            return preferred;
        }
        if (!coincidesWithNonPushable(preferredRect, scope)) {
            return preferred;
        }
        for (PlacementCandidate candidate : proposal.candidates()) {
            if (candidate == preferred) {
                continue;
            }
            Fit fit = fitRect(candidate, variant, scope);
            if (fit.ok() && !coincidesWithNonPushable(fit.rect(), scope)) {
                return candidate;
            }
        }
        return preferred;
    }

    private static double rectArea(FloatRect rect) {
        return rect.width() * rect.height();
    }

    private Fit fitRect(PlacementCandidate candidate, InworldVariant variant, RoundScope scope) {
        FloatRect rect = candidate.screenRect();
        if (variant.allowsClamp()) {
            rect = rect.clampInto(scope.workArea);
        }
        if (rect.width() <= 0 || rect.height() <= 0) {
            return Fit.fail(RejectionReason.insufficientArea);
        }
        if (rect.areaOutside(scope.workArea) > outOfBoundsEpsilon) {
            return Fit.fail(RejectionReason.outOfBounds);
        }
        if (rect.area() < variant.minComfortableArea()) {
            return Fit.fail(RejectionReason.insufficientArea);
        }
        SpacePolicy policy = variant.spacePolicy();
        if (policy.occlusionExempt()) {
            // fixed (and ghost) layouts stay put: exempt from avoidance
            return Fit.ok(rect);
        }
        if (scope.bitmap.isFree(rect.toRect())) {
            return Fit.ok(rect);
        }
        if (variant.allowsNudge()) {
            Blocker blocker = firstBlocker(rect, scope);
            if (blocker != null) {
                FloatRect shifted = nudge(rect, blocker.rect(), config.maxNudgePx(), scope.workArea, variant);
                if (shifted != null && scope.bitmap.isFree(shifted.toRect())) {
                    return Fit.ok(shifted);
                }
            }
        }
        Blocker blocker = firstBlocker(rect, scope);
        if (blocker == null) {
            // conservative bitmap rejection with no intersecting rect (cell touch)
            return Fit.fail(RejectionReason.overlap);
        }
        return blocker.elementId != null
                ? Fit.fail(RejectionReason.overlap, blocker.elementId)
                : Fit.fail(RejectionReason.exclusion);
    }

    private @Nullable FloatRect nudge(
            FloatRect rect, FloatRect blocker, double maxShift, FloatRect bounds, InworldVariant variant) {
        double[][] shifts = {
            {blocker.right() - rect.x(), 0},
            {-(rect.right() - blocker.x()), 0},
            {0, blocker.bottom() - rect.y()},
            {0, -(rect.bottom() - blocker.y())},
        };
        FloatRect best = null;
        double bestLength = Double.POSITIVE_INFINITY;
        for (double[] shift : shifts) {
            double length = Math.abs(shift[0]) + Math.abs(shift[1]);
            if (length <= 0 || length > maxShift) {
                continue;
            }
            FloatRect moved = rect.translate(shift[0], shift[1]);
            if (variant.allowsClamp()) {
                moved = moved.clampInto(bounds);
            }
            if (moved.areaOutside(bounds) > outOfBoundsEpsilon) {
                continue;
            }
            if (length < bestLength) {
                best = moved;
                bestLength = length;
            }
        }
        return best;
    }

    private @Nullable Blocker firstBlocker(FloatRect rect, RoundScope scope) {
        for (Committed committed : scope.committed) {
            if (rect.intersects(committed.rect)) {
                return new Blocker(committed.elementId, committed.rect);
            }
        }
        for (Rect exclusion : scope.exclusions) {
            if (rect.intersects(toFloat(exclusion))) {
                return new Blocker(null, toFloat(exclusion));
            }
        }
        return null;
    }

    private ElementRejection toRejection(@Nullable Fit failure, InworldVariant variant, RoundScope scope) {
        if (failure == null) {
            return ElementRejection.of(RejectionReason.overlap);
        }
        return new ElementRejection(failure.reason, failure.blockerId, suggestedRect(variant, scope));
    }

    private @Nullable Rect suggestedRect(InworldVariant variant, RoundScope scope) {
        Rect workArea = scope.workArea.toRect();
        if (workArea.width() <= 0 || workArea.height() <= 0) {
            return null;
        }
        FreeRectIndex index = new FreeRectIndex(workArea.width(), workArea.height());
        for (Committed committed : scope.committed) {
            index.occupy(committed.rect.translate(-workArea.x(), -workArea.y()).toRect());
        }
        for (Rect exclusion : scope.exclusions) {
            index.occupy(exclusion.move(new Pos(-workArea.x(), -workArea.y())));
        }
        Rect inserted = index.insert(
                (int) Math.max(1, Math.round(variant.requestedSize().width())),
                (int) Math.max(1, Math.round(variant.requestedSize().height())));
        return inserted == null ? null : inserted.move(new Pos(workArea.x(), workArea.y()));
    }

    private boolean incumbentStillFits(FloatRect incumbent, ElementRuntime runtime, RoundScope scope) {
        InworldVariant incumbentVariant = runtime.target.variant();
        if (incumbentVariant.spacePolicy().occlusionExempt()) {
            return true;
        }
        if (incumbent.areaOutside(scope.workArea) > outOfBoundsEpsilon) {
            return false;
        }
        return scope.bitmap.isFree(incumbent.toRect());
    }

    private double switchMetric(ElementRuntime runtime, FloatPos anchor, FloatRect candidateRect) {
        FloatRect incumbent = runtime.target.offsetRect().translate(anchor.x(), anchor.y());
        double centerDistance = Math.hypot(
                candidateRect.centerX() - incumbent.centerX(), candidateRect.centerY() - incumbent.centerY());
        double sizeChange =
                Math.hypot(candidateRect.width() - incumbent.width(), candidateRect.height() - incumbent.height())
                        * 0.5;
        return centerDistance + sizeChange;
    }

    // endregion

    // region continuous layer

    /**
     * The residual-overlap pushes: bounded passes over the visual rects where
     * the later element in arbitration order is shoved out along the minimal
     * axis (fixed elements never move; ghosts are not present at all).
     */
    private void separateOverlaps(Map<ElementRuntime, FloatRect> visuals, FloatRect workArea) {
        List<ElementRuntime> presented = new ArrayList<>(visuals.keySet());
        for (int pass = 0; pass < config.relaxIterations(); pass++) {
            boolean moved = false;
            for (int i = 0; i < presented.size(); i++) {
                for (int j = i + 1; j < presented.size(); j++) {
                    ElementRuntime first = presented.get(i);
                    ElementRuntime second = presented.get(j);
                    FloatRect a = visuals.get(first);
                    FloatRect b = visuals.get(second);
                    if (!a.intersects(b)) {
                        continue;
                    }
                    boolean secondPushable =
                            second.target.variant().spacePolicy().pushable();
                    boolean firstPushable = first.target.variant().spacePolicy().pushable();
                    ElementRuntime mover;
                    FloatRect obstacle;
                    if (secondPushable) {
                        mover = second;
                        obstacle = a;
                    } else if (firstPushable) {
                        mover = first;
                        obstacle = b;
                    } else {
                        continue;
                    }
                    FloatRect moving = visuals.get(mover);
                    double dx = moving.centerX() - obstacle.centerX();
                    double dy = moving.centerY() - obstacle.centerY();
                    double overlapX = Math.min(moving.right(), obstacle.right()) - Math.max(moving.x(), obstacle.x());
                    double overlapY = Math.min(moving.bottom(), obstacle.bottom()) - Math.max(moving.y(), obstacle.y());
                    double shiftX = 0;
                    double shiftY = 0;
                    if (overlapX <= overlapY) {
                        shiftX = dx >= 0 ? overlapX : -overlapX;
                    } else {
                        shiftY = dy >= 0 ? overlapY : -overlapY;
                    }
                    FloatRect shifted = moving.translate(shiftX, shiftY).clampInto(workArea);
                    visuals.put(mover, shifted);
                    moved = true;
                }
            }
            if (!moved) {
                break;
            }
        }
    }

    /**
     * Caps a visual rect's center motion at {@code maxPx} against the last
     * committed visual, keeping the rect's own size: the cap bounds how far
     * the displayed rect may travel between two frames, it must not freeze
     * the rect's size (a FLIP morphs position and size together, and the
     * size interpolation has its own, slower rate).
     */
    private FloatRect clampDisplacement(FloatRect visual, FloatRect lastVisual, double maxPx) {
        double dx = visual.centerX() - lastVisual.centerX();
        double dy = visual.centerY() - lastVisual.centerY();
        double length = Math.hypot(dx, dy);
        if (length <= maxPx || length == 0) {
            return visual;
        }
        double scale = maxPx / length;
        return visual.translate(dx * (scale - 1.0), dy * (scale - 1.0));
    }

    /**
     * Carries the relaxation displacement — the residual-overlap push and the
     * displacement clamp — into the element's animation state, so the state's
     * visual is exactly the committed rect. Without this the animation would
     * keep following its own, unpushed path and the next frame would re-derive
     * the whole push (or, after a clamp, leave the element permanently behind
     * its state). The FLIP keeps its phase, so a retarget re-plans from what
     * is on screen; when it rests, its rest rect <em>is</em> the committed
     * rect, so the next morph starts from the committed visual rather than
     * from a stale internal one. The spring is displaced along with it, which
     * keeps the two states in step across the FLIP→spring handoff.
     */
    /**
     * The layout rect the element is heading for this frame: its stored offset
     * from the anchor it currently proposes (the retained anchor when the
     * element proposes none, e.g. while retracted).
     */
    private FloatRect layoutTargetRect(ElementRuntime runtime) {
        FloatPos anchor =
                runtime.roundZero.anchorScreen() != null ? runtime.roundZero.anchorScreen() : runtime.target.anchor();
        return runtime.target.offsetRect().translate(anchor.x(), anchor.y());
    }

    private void carryRelaxation(ElementRuntime runtime, FloatRect unclamped, FloatRect committed, double nowSeconds) {
        double dx = committed.centerX() - unclamped.centerX();
        double dy = committed.centerY() - unclamped.centerY();
        if (dx != 0 || dy != 0) {
            runtime.spring.translate(dx, dy);
            if (runtime.flip.isAnimating(nowSeconds)) {
                runtime.flip.translate(dx, dy);
            }
        }
        if (!runtime.flip.isAnimating(nowSeconds)) {
            runtime.flip.snap(committed);
        }
    }

    // endregion

    // region bookkeeping

    private List<ElementRuntime> orderedRuntimes() {
        List<ElementRuntime> ordered = new ArrayList<>(runtimes.values());
        ordered.sort(arbitrationComparator());
        return ordered;
    }

    /** The total arbitration order: spaceKind, priority desc, sticky, registration. */
    private Comparator<ElementRuntime> arbitrationComparator() {
        return (a, b) -> {
            int byKind = Integer.compare(
                    a.element.spaceKind().ordinal(), b.element.spaceKind().ordinal());
            if (byKind != 0) {
                return byKind;
            }
            int byPriority = Integer.compare(b.element.priority(), a.element.priority());
            if (byPriority != 0) {
                return byPriority;
            }
            if (a.element.sticky() != b.element.sticky()) {
                return a.element.sticky() ? -1 : 1;
            }
            return Long.compare(a.registrationIndex, b.registrationIndex);
        };
    }

    private void attemptUpgrades(List<ElementRuntime> ordered) {
        for (ElementRuntime runtime : ordered) {
            if (runtime.currentLevel > 0) {
                runtime.currentLevel--;
            }
        }
    }

    private InworldVariant ladderVariant(ElementRuntime runtime) {
        return runtime.element.ladder().variant(runtime.currentLevel);
    }

    private void requireLadderRung(ElementRuntime runtime, InworldVariant variant) {
        VariantLadder ladder = runtime.element.ladder();
        if (variant.level() < 0 || variant.level() >= ladder.size()) {
            throw new IllegalArgumentException("element " + runtime.element.id() + " proposed variant level "
                    + variant.level() + " outside its ladder [0, " + ladder.size() + ")");
        }
    }

    private SpaceBudget smoothedBudget(FrameInput frame, Rect workArea, List<ElementRuntime> ordered) {
        OccupancyBitmap bitmap =
                new OccupancyBitmap(frame.screenWidth(), frame.screenHeight(), config.occupancyCellSize());
        bitmap.markAll(frame.exclusionRects());
        for (ElementRuntime runtime : ordered) {
            if (runtime.target != null && runtime.target.variant().spacePolicy() != SpacePolicy.ghost) {
                bitmap.mark(runtime.target.screenRect().toRect());
            }
        }
        SpaceBudget raw = SpaceBudget.of(workArea, bitmap);
        double fraction = Double.isNaN(smoothedFreeFraction)
                ? raw.freeFraction()
                : Smoothing.dampHalfLife(
                        smoothedFreeFraction, raw.freeFraction(), config.budgetHalfLifeSeconds(), frame.dtSeconds());
        smoothedFreeFraction = fraction;
        return new SpaceBudget(workArea, raw.workAreaArea() * fraction, fraction);
    }

    private static boolean sameRects(List<Rect> a, List<Rect> b) {
        if (a.size() != b.size()) {
            return false;
        }
        List<Rect> left = new ArrayList<>(a);
        List<Rect> right = new ArrayList<>(b);
        Comparator<Rect> order = Comparator.comparingInt(Rect::y)
                .thenComparingInt(Rect::x)
                .thenComparingInt(Rect::width)
                .thenComparingInt(Rect::height);
        left.sort(order);
        right.sort(order);
        return left.equals(right);
    }

    private static double distance(FloatPos a, FloatPos b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    private static boolean rectsAlmostEqual(FloatRect a, FloatRect b) {
        return rectsAlmostEqual(a, b, discreteRectEpsilonPx);
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

    // region internal types

    private record RoundScope(
            OccupancyBitmap bitmap, FloatRect workArea, List<Rect> exclusions, long epoch, List<Committed> committed) {

        RoundScope(OccupancyBitmap bitmap, FloatRect workArea, List<Rect> exclusions, long epoch) {
            this(bitmap, workArea, exclusions, epoch, new ArrayList<>());
        }
    }

    private record Committed(String elementId, FloatRect rect, boolean pushable) {

        static Committed of(String elementId, FloatRect rect, boolean pushable) {
            return new Committed(elementId, rect, pushable);
        }
    }

    private record Blocker(@Nullable String elementId, FloatRect rect) {}

    private record Fit(boolean ok, FloatRect rect, @Nullable RejectionReason reason, @Nullable String blockerId) {

        static Fit ok(FloatRect rect) {
            return new Fit(true, rect, null, null);
        }

        static Fit fail(RejectionReason reason) {
            return new Fit(false, FloatRect.empty, reason, null);
        }

        static Fit fail(RejectionReason reason, @Nullable String blockerId) {
            return new Fit(false, FloatRect.empty, reason, blockerId);
        }
    }

    private record Acceptance(@Nullable InworldPlacement placement, @Nullable ElementRejection rejection) {

        static Acceptance accepted(InworldPlacement placement) {
            return new Acceptance(placement, null);
        }

        static Acceptance rejected(ElementRejection rejection) {
            return new Acceptance(null, rejection);
        }
    }

    /** Per-element mutable state; everything crossing a phase boundary is a record. */
    private static final class ElementRuntime {

        final InworldElement element;
        final long registrationIndex;
        final VisibilityTracker visibility;

        int frameArbitrationIndex;
        int currentLevel;
        InworldPlacement target;
        boolean lastPresented;
        boolean pendingPresent;
        boolean retractedLastFrame;
        ElementRejection activeRejection;
        FloatPos anchorAtLastResolve;
        FloatRect lastVisual;
        FloatRect lastTargetRect;
        FlipPlanner flip;
        Spring2 spring;
        SwitchGate<Integer> gate;
        ElementProposal roundZero;

        ElementRuntime(InworldElement element, long registrationIndex, Config config) {
            this.element = element;
            this.registrationIndex = registrationIndex;
            this.visibility = new VisibilityTracker(config.visibility());
        }
    }

    // endregion

    // region input types

    /**
     * The pipeline's tunables — every threshold and rate, constructor-checked.
     *
     * @param epochSeconds the decision epoch length in seconds (the 4–10 Hz
     *        re-resolve cadence)
     * @param maxRounds the negotiation round bound: 1 rejects straight to
     *        hidden, 2 allows the renegotiation round
     * @param anchorDisplacementThresholdPx projected screen pixels an anchor
     *        must move between resolves before the move counts as dirty
     * @param maxNudgePx the largest corrective shift the coordinator may
     *        apply to an {@code allowsNudge} candidate before rejecting it
     * @param relaxDisplacementClampPx the per-frame cap on a visual rect's
     *        motion in the continuous layer
     * @param relaxIterations the bounded passes of residual-overlap pushing
     * @param occupancyCellSize the occupancy bitmap's cell size in pixels
     * @param budgetHalfLifeSeconds the half-life of the smoothed space-budget
     *        feedback
     * @param flipSpeedPixelsPerSecond the FLIP morph speed for discrete rect
     *        changes
     * @param springOmega the critically damped follow spring's stiffness
     * @param visibility the linger/fade configuration
     * @param switchGate the discrete tier-switch gate configuration
     */
    public record Config(
            double epochSeconds,
            int maxRounds,
            double anchorDisplacementThresholdPx,
            double maxNudgePx,
            double relaxDisplacementClampPx,
            int relaxIterations,
            int occupancyCellSize,
            double budgetHalfLifeSeconds,
            double flipSpeedPixelsPerSecond,
            double springOmega,
            VisibilityTracker.Config visibility,
            SwitchGate.Config switchGate) {

        public Config {
            requirePositive("epochSeconds", epochSeconds);
            if (maxRounds < 1 || maxRounds > 2) {
                throw new IllegalArgumentException("maxRounds must be 1 or 2: " + maxRounds);
            }
            requirePositive("anchorDisplacementThresholdPx", anchorDisplacementThresholdPx);
            requireNonNegative("maxNudgePx", maxNudgePx);
            requirePositive("relaxDisplacementClampPx", relaxDisplacementClampPx);
            if (relaxIterations < 1) {
                throw new IllegalArgumentException("relaxIterations must be at least 1: " + relaxIterations);
            }
            if (occupancyCellSize < 1) {
                throw new IllegalArgumentException("occupancyCellSize must be at least 1: " + occupancyCellSize);
            }
            requirePositive("budgetHalfLifeSeconds", budgetHalfLifeSeconds);
            requirePositive("flipSpeedPixelsPerSecond", flipSpeedPixelsPerSecond);
            requirePositive("springOmega", springOmega);
            Objects.requireNonNull(visibility, "visibility");
            Objects.requireNonNull(switchGate, "switchGate");
        }

        public static Config defaults() {
            return new Config(
                    0.2,
                    2,
                    12.0,
                    48.0,
                    24.0,
                    3,
                    8,
                    0.3,
                    900.0,
                    30.0,
                    VisibilityTracker.Config.of(0.15, 0.25, 0.25),
                    SwitchGate.Config.of(16.0, 1, 2.0, 0));
        }

        private static void requirePositive(String name, double value) {
            if (!Double.isFinite(value) || value <= 0) {
                throw new IllegalArgumentException(name + " must be finite and positive: " + value);
            }
        }

        private static void requireNonNegative(String name, double value) {
            if (!Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
            }
        }
    }

    /**
     * One frame's explicit inputs — the only way time enters the pipeline.
     *
     * @param screenWidth the gui-scaled screen width
     * @param screenHeight the gui-scaled screen height
     * @param exclusionRects the exclusion areas this frame, typically
     *        collected from the exclusion-area registry; edge-hugging ones
     *        become struts, the rest occupy the bitmap
     * @param nowSeconds the frame's absolute time; should be monotonic across
     *        frames — rewound values are clamped by the time-consuming
     *        components, not rejected
     * @param dtSeconds the elapsed time since the previous frame; must be
     *        finite — a negative value is clamped to zero (a rewound clock),
     *        not rejected
     */
    public record FrameInput(
            int screenWidth, int screenHeight, List<Rect> exclusionRects, double nowSeconds, double dtSeconds) {

        public FrameInput {
            if (screenWidth <= 0 || screenHeight <= 0) {
                throw new IllegalArgumentException("screen size must be positive: " + screenWidth + "x" + screenHeight);
            }
            exclusionRects = List.copyOf(exclusionRects);
            if (!Double.isFinite(nowSeconds)) {
                throw new IllegalArgumentException("nowSeconds must be finite: " + nowSeconds);
            }
            if (!Double.isFinite(dtSeconds)) {
                throw new IllegalArgumentException("dtSeconds must be finite: " + dtSeconds);
            }
            dtSeconds = Math.max(0.0, dtSeconds);
        }

        /** A frame input with the given exclusion rectangles. */
        public static FrameInput of(
                int screenWidth, int screenHeight, double nowSeconds, double dtSeconds, Rect... exclusions) {
            return of(screenWidth, screenHeight, nowSeconds, dtSeconds, List.of(exclusions));
        }

        /** A frame input with the given exclusion rectangles. */
        public static FrameInput of(
                int screenWidth, int screenHeight, double nowSeconds, double dtSeconds, List<Rect> exclusions) {
            return new FrameInput(screenWidth, screenHeight, exclusions, nowSeconds, dtSeconds);
        }
    }

    // endregion
}
