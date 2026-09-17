package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * One frame's coordination outcome, committed atomically: elements never see
 * a half-resolved layout. Placements are the grants, element states carry the
 * per-element render posture (phase, alpha, the animated visual rect), and
 * the budget is the feedback handed to the next frame's collect phase.
 *
 * @param frame the frame index (counted from 1)
 * @param epoch the decision epoch reached (incremented on every resolve)
 * @param resolved whether the discrete layer re-ran this frame (a frame
 *        without a renegotiation cause keeps its incumbents and only relaxes)
 * @param cause why the frame did (or did not) renegotiate
 * @param placements the grants of this frame, in arbitration order; only
 *        currently presented elements appear here
 * @param elementStates every registered element's state, in arbitration
 *        order — including hidden and lingering ones
 * @param budget the space feedback for the next frame's collect phase
 */
public record CoordinationResult(
        long frame,
        long epoch,
        boolean resolved,
        RenegotiationCause cause,
        List<InworldPlacement> placements,
        List<ElementState> elementStates,
        SpaceBudget budget) {

    public CoordinationResult {
        placements = List.copyOf(placements);
        elementStates = List.copyOf(elementStates);
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(budget, "budget");
    }

    /** The grant for {@code elementId}, if the element is presented this frame. */
    public @Nullable InworldPlacement placementOf(String elementId) {
        for (InworldPlacement placement : placements) {
            if (placement.elementId().equals(elementId)) {
                return placement;
            }
        }
        return null;
    }

    /** The state record for {@code elementId}, or {@code null} if unregistered. */
    public @Nullable ElementState elementState(String elementId) {
        for (ElementState state : elementStates) {
            if (state.elementId().equals(elementId)) {
                return state;
            }
        }
        return null;
    }

    /**
     * Why the coordinator did (or did not) renegotiate this frame, in
     * reporting priority order.
     */
    public enum RenegotiationCause {
        /** No cause: the frame only relaxed toward existing targets. */
        none,
        /** An element registered or unregistered. */
        membershipChanged,
        /** The exclusion rectangles changed since the last resolve. */
        exclusionsChanged,
        /** A retracted element came back (its anchor recovered). */
        presentationChanged,
        /** An anchor's projected screen position moved beyond the threshold. */
        anchorDisplacement,
        /** The epoch timer elapsed — the periodic 4–10 Hz re-resolve. */
        epochElapsed
    }

    /**
     * One element's render posture for this frame.
     *
     * @param elementId the element's id
     * @param phase the visibility lifecycle phase
     * @param alpha the visibility alpha in {@code [0, 1]}
     * @param placement the element's current placement target (retained
     *        through linger); {@code null} if never placed
     * @param visualRect the animated rect to draw this frame — the FLIP/spring
     *        output, or the frozen last rect while lingering;
     *        {@code null} if never placed
     * @param rejection the rejection that left this element unpresented this
     *        frame; {@code null} when presented or retracted
     */
    public record ElementState(
            String elementId,
            VisibilityTracker.Phase phase,
            double alpha,
            @Nullable InworldPlacement placement,
            @Nullable FloatRect visualRect,
            @Nullable ElementRejection rejection) {

        public ElementState {
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(phase, "phase");
            if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) {
                throw new IllegalArgumentException("alpha must be in [0, 1]: " + alpha);
            }
        }
    }
}
