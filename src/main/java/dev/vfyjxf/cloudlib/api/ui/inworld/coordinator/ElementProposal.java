package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * What an {@link InworldElement} offers the coordinator this round: the
 * variant (form) it wants plus the candidate placements for that variant, in
 * the element's own preference order — strongest candidate first. Candidates
 * carry the dual representation (world box + projected rect); the anchor
 * screen position is the motion reference the granted rect is stored relative
 * to (§3.0 iron rule 1: positions live as offsets from the anchor projection,
 * never as absolute coordinates).
 * <p>
 * An element whose anchor is gone (entity unloaded) or that simply has
 * nothing to show returns {@link #retract} instead — retraction enters the
 * linger path, it is not a rejection.
 * <p>
 * A {@code world-only} element (see {@link InworldElement#worldOnly()}) may
 * propose without any projection at all: {@link #worldOnly} factories leave
 * the anchor screen position open and the candidates' screen rects optional.
 *
 * @param variant the form being proposed
 * @param anchorScreen the anchor's projected screen position this frame; the
 *        granted rect is stored as an offset from it — required for a
 *        projecting proposal, open for a world-only one
 * @param candidates the candidate placements, strongest first; may be empty
 *        only together with {@code retracted}
 * @param retracted whether the element withdraws this frame (anchor invalid
 *        or nothing to present)
 * @param worldOnly whether this is a world-only proposal: no anchor
 *        projection is required and candidates may carry world geometry
 *        without a screen rect; only a world-only element may propose one
 */
public record ElementProposal(
        InworldVariant variant,
        @Nullable FloatPos anchorScreen,
        List<PlacementCandidate> candidates,
        boolean retracted,
        boolean worldOnly) {

    public ElementProposal {
        Objects.requireNonNull(variant, "variant");
        candidates = List.copyOf(candidates);
        if (retracted && !candidates.isEmpty()) {
            throw new IllegalArgumentException("a retracted proposal carries no candidates");
        }
        if (!retracted && !worldOnly) {
            Objects.requireNonNull(anchorScreen, "anchorScreen");
        }
        if (!worldOnly) {
            for (PlacementCandidate candidate : candidates) {
                if (candidate.screenRect() == null) {
                    throw new IllegalArgumentException("a projecting proposal's candidates must carry screen rects");
                }
            }
        }
    }

    /**
     * An active proposal anchored at {@code anchorScreen}.
     */
    public static ElementProposal of(
            InworldVariant variant, FloatPos anchorScreen, List<PlacementCandidate> candidates) {
        return new ElementProposal(variant, new FloatPos(anchorScreen.x(), anchorScreen.y()), candidates, false, false);
    }

    /**
     * An active proposal anchored at {@code anchorScreen} with the given
     * candidates.
     */
    public static ElementProposal of(InworldVariant variant, FloatPos anchorScreen, PlacementCandidate... candidates) {
        return of(variant, anchorScreen, List.of(candidates));
    }

    /**
     * A world-only proposal with no screen projection: the coordinator grants
     * the first candidate carrying a world box and ignores everything screen
     * space. Candidates without a world box are skipped; a proposal with none
     * retracts.
     */
    public static ElementProposal worldOnly(InworldVariant variant, List<PlacementCandidate> candidates) {
        return new ElementProposal(variant, null, candidates, false, true);
    }

    /** A world-only proposal with the given candidates. */
    public static ElementProposal worldOnly(InworldVariant variant, PlacementCandidate... candidates) {
        return worldOnly(variant, List.of(candidates));
    }

    /**
     * A world-only proposal that still knows its anchor's projected position
     * — carried as metadata only; the coordinator never arbitrates it.
     */
    public static ElementProposal worldOnly(
            InworldVariant variant, @Nullable FloatPos anchorScreen, List<PlacementCandidate> candidates) {
        return new ElementProposal(
                variant,
                anchorScreen == null ? null : new FloatPos(anchorScreen.x(), anchorScreen.y()),
                candidates,
                false,
                true);
    }

    /**
     * A withdrawal: the anchor is gone or the element has nothing to show.
     * The coordinator keeps the element's last placement as sticky memory and
     * enters the linger path.
     */
    public static ElementProposal retract(InworldVariant currentVariant) {
        return new ElementProposal(currentVariant, null, List.of(), true, false);
    }
}
