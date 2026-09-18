package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementRejection;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The escape hatch (§7, plan A core): the three-callback strategy interface
 * for the scenarios the seven facets cannot express. An element embeds one
 * through {@code ElementSpec.custom(InworldLayouter)}; its
 * {@link #propose} then <em>replaces</em> the pipeline's element-side stages
 * (ANCHOR→PROJECT→CANDIDATES→AVOID→RANK) — the coordinator still arbitrates,
 * stabilizes and commits. Custom layouters are expected to compose the
 * named strategies from {@link StageCatalogs} rather than reinvent
 * geometry.
 * <p>
 * Callback order per frame: {@link #reserve} once (lazily, on the first
 * frame, to declare the element's space posture), then
 * {@link #propose} per negotiation round (the coordinator drives these),
 * then {@link #arbitrated} when the frame's result is dispatched back
 * through the assembled element's {@code observe} — including the
 * retracted/hidden cases, where {@code placement} is null and the feedback
 * carries the phase and rejection.
 * <p>
 * A layouter whose element lives purely in world space (a billboard drawn at
 * the entity, no screen placement to project) declares the world-only
 * capability on the spec — {@code ElementSpec.withWorldOnly(true)}; the
 * reservation cannot express it. The coordinator then bypasses screen
 * arbitration entirely for the element: {@link #propose} may return a
 * world-only proposal (no anchor projection, candidates without screen
 * rects), the first candidate carrying a world box is granted
 * unconditionally, and {@link #arbitrated} receives that grant — its screen
 * half is empty, the world box is the placement.
 * <p>
 * Zone consumption (Z2, optional): a custom layouter may compose the zone
 * strategies — {@code candidates.zoneGrid} and {@code rank.zoneCost} from
 * {@link StageCatalogs} — and read the spec's {@code ZoneFacet}, the
 * environment's previous committed layout ({@code LayoutEnvironment}'s zone
 * snapshot, refreshed from {@code InworldCoordinator#previousZoneLayout})
 * and the current LOD tier ({@code CoordinationResult.ElementState#lodTier})
 * like any facet-driven element would. Nothing requires it: a layouter that
 * ignores the zone vocabulary keeps its exact pre-zone behavior.
 */
public interface InworldLayouter {

    /**
     * Declares the element's space posture: which {@link SpaceKind} it lives
     * in, its arbitration priority, whether it arbitrates sticky, and its
     * {@link ElementMode}. Called once, on the element's first frame; the
     * facets supply the posture when no custom layouter is present.
     */
    SpaceReservation reserve(InworldLayoutContext ctx);

    /**
     * Produces this round's proposal — the same contract as the pipeline's
     * element-side stages produce: the context's variant, an anchor screen
     * position, and candidates in the element's own preference order
     * (strongest first).
     */
    ElementProposal propose(InworldLayoutContext ctx);

    /**
     * Receives the arbitration outcome for this element: the granted
     * placement, or null when the element is not presented this frame —
     * retracted (lingering) or rejected (the feedback says which).
     */
    void arbitrated(@Nullable InworldPlacement placement, Feedback feedback);

    /**
     * The declared space posture (see {@link #reserve}).
     *
     * @param kind where the element lives — the first arbitration key
     * @param priority arbitration precedence within the kind
     * @param sticky whether the element prefers its incumbent placement
     * @param mode {@code arbitrated} (coordinator-driven degradation) or
     *        {@code selfManaged} (one authoritative candidate, conflicts
     *        final)
     */
    record SpaceReservation(SpaceKind kind, int priority, boolean sticky, ElementMode mode) {

        public SpaceReservation {
            Objects.requireNonNull(kind, "kind and mode must not be null");
            Objects.requireNonNull(mode, "kind and mode must not be null");
        }

        /** An arbitrated, non-sticky reservation at priority 0 in {@code kind}. */
        public static SpaceReservation arbitrated(SpaceKind kind) {
            return new SpaceReservation(kind, 0, false, ElementMode.arbitrated);
        }
    }

    /**
     * The post-arbitration feedback for one element: its render posture and,
     * when unpresented, the rejection that left it out.
     *
     * @param phase the visibility lifecycle phase this frame
     * @param alpha the visibility alpha in {@code [0, 1]}
     * @param rejection the active rejection, when the element was rejected
     * @param epoch the decision epoch of the reported frame
     * @param frame the frame index of the reported frame
     */
    record Feedback(
            VisibilityTracker.Phase phase, double alpha, @Nullable ElementRejection rejection, long epoch, long frame) {

        public Feedback {
            Objects.requireNonNull(phase, "phase must not be null");
            if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) {
                throw new IllegalArgumentException("alpha must be in [0, 1]: " + alpha);
            }
        }

        static Feedback of(CoordinationResult.ElementState state, long epoch, long frame) {
            return new Feedback(state.phase(), state.alpha(), state.rejection(), epoch, frame);
        }
    }
}
