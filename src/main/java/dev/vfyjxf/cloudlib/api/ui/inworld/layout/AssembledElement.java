package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.AvoidanceClass;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldElement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ProposeContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceBudget;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.VariantLadder;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.AttentionField;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.GaussianAttention;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.PreviousFrameLayout;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCost;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An {@link ElementSpec} assembled into an {@link InworldElement} the
 * {@code InworldCoordinator} drives. The element-side pipeline stages run
 * inside {@link #propose}: ANCHOR (resolve the anchor — camera-tracked
 * anchors resolve purely, world anchors come from the frame environment,
 * a gone anchor retracts), PROJECT (the variant's anchor-centered
 * footprint), CANDIDATES / AVOID / RANK (the profile-bound named
 * strategies). The coordinator-side stages (ARBITRATE / STABILIZE /
 * COMMIT) run when the coordinator frames; dispatch the result back through
 * {@link #observe} so an embedded custom layouter receives its
 * {@code arbitrated} callback.
 * <p>
 * A spec with an embedded custom layouter delegates propose entirely to it
 * (the escape hatch); the facets still supply the ladder and the default
 * posture, which the layouter's {@code reserve} declaration may override.
 */
public final class AssembledElement implements InworldElement {

    private final ElementSpec spec;
    private final StageCatalogs.CandidateStrategy candidates;
    private final StageCatalogs.AvoidStrategy avoid;
    private final StageCatalogs.RankStrategy rank;
    private final VariantLadder ladder;

    private LayoutEnvironment environment;
    private InworldLayouter.@Nullable SpaceReservation reservation;
    private boolean reservationResolved;

    AssembledElement(
        ElementSpec spec,
        StageCatalogs.CandidateStrategy candidates,
        StageCatalogs.AvoidStrategy avoid,
        StageCatalogs.RankStrategy rank
    ) {
        this.spec = spec;
        this.candidates = candidates;
        this.avoid = avoid;
        this.rank = rank;
        this.ladder = spec.degrade().ladder();
    }

    /** The assembled spec. */
    public ElementSpec spec() {
        return spec;
    }

    /**
     * Hands the element this frame's environment. Must be called before the
     * coordinator's frame; the first call also resolves an embedded custom
     * layouter's {@code reserve} declaration.
     */
    public void beginFrame(LayoutEnvironment frameEnvironment) {
        this.environment = Objects.requireNonNull(frameEnvironment, "frameEnvironment");
        resolveReservation();
    }

    /**
     * Dispatches the coordinator's committed result to the embedded custom
     * layouter ({@code arbitrated}); a no-op for facet-only specs. The
     * placement handed over is the frame's grant — null when the element is
     * not presented (retracted/lingering or rejected; the feedback's phase
     * and rejection say which).
     */
    public void observe(CoordinationResult result) {
        Objects.requireNonNull(result, "result");
        if (spec.custom() == null) {
            return;
        }
        CoordinationResult.ElementState state = result.elementState(spec.id());
        if (state == null) {
            return;
        }
        spec.custom().arbitrated(
            result.placementOf(spec.id()),
            InworldLayouter.Feedback.of(state, result.epoch(), result.frame())
        );
    }

    // region InworldElement

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public SpaceKind spaceKind() {
        return reservation().kind();
    }

    @Override
    public VariantLadder ladder() {
        return ladder;
    }

    @Override
    public int priority() {
        return reservation().priority();
    }

    @Override
    public boolean sticky() {
        return reservation().sticky();
    }

    @Override
    public ElementMode mode() {
        return reservation().mode();
    }

    @Override
    public boolean worldOnly() {
        return spec.worldOnly();
    }

    @Override
    public AvoidanceClass avoidanceClass() {
        return spec.avoidanceClass();
    }

    @Override
    public boolean consumesZoneLayout() {
        return spec.zone() != null;
    }

    @Override
    public ElementProposal propose(ProposeContext context) {
        Objects.requireNonNull(context, "context");
        requireEnvironment();
        InworldLayoutContext layoutContext = new InworldLayoutContext(
            context.epoch(),
            context.round(),
            context.budget(),
            context.lastPlacement(),
            context.lastRejection(),
            context.variant(),
            environment,
            spec
        );
        if (spec.custom() != null) {
            return spec.custom().propose(layoutContext);
        }
        return runStages(layoutContext);
    }

    // endregion

    // region stages

    private ElementProposal runStages(InworldLayoutContext context) {
        // ANCHOR: camera-tracked anchors resolve purely from the screen
        // size; world anchors come from the environment; a gone anchor
        // retracts (linger, not rejection).
        FloatPos anchor = resolveAnchor();
        if (anchor == null) {
            return ElementProposal.retract(context.variant());
        }

        // PROJECT + CANDIDATES + AVOID + RANK.
        double inset = spec.anchor() instanceof AnchorFacet.BlockFace face ? face.insetPixels() : 0.0;
        StageCatalogs.CandidateContext candidateContext = new StageCatalogs.CandidateContext(
            anchor,
            context.variant().requestedSize(),
            spec.orientation().mode(),
            inset,
            environment,
            spec.profile().algorithm().params(),
            spec.avoidance().avoids(),
            spec.zone()
        );
        List<PlacementCandidate> generated = candidates.candidates(candidateContext);
        StageCatalogs.AvoidContext avoidContext = new StageCatalogs.AvoidContext(
            spec.avoidance().avoids(),
            spec.avoidance().respectsExclusions(),
            environment
        );
        List<PlacementCandidate> surviving = avoid.filter(generated, avoidContext);
        // A spec without a zone declaration builds no zone context — its rank
        // context is exactly the pre-zone one.
        StageCatalogs.RankContext rankContext = spec.zone() == null
                ? new StageCatalogs.RankContext(
                    anchor,
                    incumbentCenter(context),
                    sticky(),
                    spec.profile().algorithm().params()
                )
                : new StageCatalogs.RankContext(
                    anchor,
                    incumbentCenter(context),
                    sticky(),
                    spec.profile().algorithm().params(),
                    zoneInputs(context, anchor)
                );
        List<PlacementCandidate> ranked = rank.rank(surviving, rankContext);
        return ElementProposal.of(context.variant(), anchor, ranked);
    }

    /**
     * The zone scoring inputs for a zone-declaring spec: the unified cost
     * context assembled from the frame environment (screen as the safe rect,
     * exclusions), the coordinator's previous-frame snapshot (other
     * elements' committed rects, their leaders, the adjacency tables) and
     * the element's own runtime previous rect. Everything temporal is the
     * previous committed frame — this frame's incremental placements never
     * enter element-side scoring.
     */
    private StageCatalogs.RankContext.ZoneInputs zoneInputs(InworldLayoutContext context, FloatPos anchor) {
        ZoneFacet zone = spec.zone();
        LayoutEnvironment env = context.environment();
        PreviousFrameLayout previous = env.previousLayout();
        Map<String, Rect> placed = new LinkedHashMap<>();
        if (previous != null) {
            for (Map.Entry<String, Rect> entry : previous.placements().entrySet()) {
                if (!entry.getKey().equals(spec.id())) {
                    placed.put(entry.getKey(), entry.getValue());
                }
            }
        }
        InworldPlacement last = context.lastPlacement();
        Rect previousRect = last == null ? null : last.screenRect().toRect();
        AttentionField attention = zone.attention() != null ? zone.attention() : gaussianAttention(env);
        ZoneCost.Context costContext = new ZoneCost.Context(
            spec.id(),
            anchor,
            new Rect(0, 0, env.screenWidth(), env.screenHeight()),
            attention,
            placed,
            env.exclusionRects(),
            previousRect,
            previous == null ? List.of() : previous.leadersExcluding(spec.id()),
            previous == null ? Set.of() : previous.leftOf(),
            previous == null ? Set.of() : previous.above()
        );
        return new StageCatalogs.RankContext.ZoneInputs(zone.weightsOrDefault(), costContext);
    }

    private static GaussianAttention gaussianAttention(LayoutEnvironment env) {
        return GaussianAttention
                .atScreenCenter(env.screenWidth(), env.screenHeight(), ZoneFacet.defaultAttentionSigmaPx);
    }

    private @Nullable FloatPos resolveAnchor() {
        if (spec.anchor() instanceof AnchorFacet.CameraTracked tracked) {
            return new FloatPos(tracked.u() * environment.screenWidth(), tracked.v() * environment.screenHeight());
        }
        if (spec.anchor() instanceof AnchorFacet.None) {
            AnchorFrame frame = environment.anchor();
            return frame != null
                    ? frame.screen()
                    : new FloatPos(environment.screenWidth() * 0.5, environment.screenHeight() * 0.5);
        }
        AnchorFrame frame = environment.anchor();
        return frame == null ? null : frame.screen();
    }

    private @Nullable FloatPos incumbentCenter(InworldLayoutContext context) {
        InworldPlacement last = context.lastPlacement();
        return last == null ? null : last.screenRect().center();
    }

    // endregion

    // region reservation

    private InworldLayouter.SpaceReservation reservation() {
        resolveReservation();
        return reservation;
    }

    private void resolveReservation() {
        if (reservationResolved) {
            return;
        }
        if (spec.custom() != null) {
            requireEnvironment();
            Rect area = new Rect(0, 0, environment.screenWidth(), environment.screenHeight());
            InworldLayoutContext context = new InworldLayoutContext(
                0,
                0,
                new SpaceBudget(area, area.width() * (double) area.height(), 1.0),
                null,
                null,
                ladder.strongest(),
                environment,
                spec
            );
            reservation = spec.custom().reserve(context);
            reservationResolved = true;
            return;
        }
        reservation = new InworldLayouter.SpaceReservation(
            spaceKindOf(spec.anchor()),
            spec.spaces().priority(),
            spec.stability().stickySlot(),
            ElementMode.arbitrated
        );
        reservationResolved = true;
    }

    private static SpaceKind spaceKindOf(AnchorFacet anchor) {
        if (anchor.worldAnchored()) {
            return SpaceKind.world;
        }
        return anchor instanceof AnchorFacet.CameraTracked ? SpaceKind.tracked : SpaceKind.panel;
    }

    private void requireEnvironment() {
        if (environment == null) {
            throw new IllegalStateException(
                "beginFrame must be called before the coordinator drives element " + spec.id()
            );
        }
    }

    // endregion
}
