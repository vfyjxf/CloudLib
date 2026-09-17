package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

/**
 * The coordinator-driven element contract (§3.2). Implementations are
 * registered with {@link InworldCoordinator#register} and then never lay
 * themselves out again — the coordinator drives them through the
 * propose/adjudicate/feedback cycle and hands back
 * {@link CoordinationResult}s; the declarative {@code ElementSpec} surface
 * that builds ordinary implementations on top of this contract is a later
 * milestone.
 * <p>
 * Contract rules:
 * <ul>
 *   <li>{@link #propose} must be deterministic for a given
 *       {@link ProposeContext} (same context in, same proposal out), fast,
 *       and non-throwing; it proposes the context's
 *       {@link ProposeContext#variant()} — round 0 the current rung, round 1
 *       the rung the coordinator already degraded to. The coordinator owns
 *       the ladder walk; an element cannot talk it out of a degradation</li>
 *   <li>candidates are ordered strongest-first; the coordinator walks them in
 *       that order and grants the first that fits</li>
 *   <li>{@link ElementMode#selfManaged} elements propose exactly one
 *       authoritative candidate: the coordinator registers its occupancy and
 *       rejects it on conflict, nothing more</li>
 *   <li>an anchor that is gone (entity unloaded) retracts via
 *       {@link ElementProposal#retract} — linger, not rejection</li>
 *   <li>a {@link #worldOnly} element opts out of projection: it never enters
 *       screen arbitration (no occupancy, no budget, no negotiation, no
 *       relaxation) — the coordinator accepts its first candidate carrying a
 *       world box unconditionally, and retracts it when it proposes none</li>
 * </ul>
 */
public interface InworldElement {

    /** The element's unique id within its coordinator. */
    String id();

    /**
     * Where this element lives — the first key of the arbitration order
     * (world → tracked → panel).
     */
    SpaceKind spaceKind();

    /** The element's degradation ladder. */
    VariantLadder ladder();

    /**
     * Produces this round's proposal.
     */
    ElementProposal propose(ProposeContext context);

    /**
     * Arbitration precedence within the same {@link SpaceKind}: higher wins
     * the contested spot. Default 0.
     */
    default int priority() {
        return 0;
    }

    /**
     * Whether this element prefers its incumbent placement when arbitrating
     * (slot stickiness) and sorts before non-sticky peers at equal priority.
     * Default false.
     */
    default boolean sticky() {
        return false;
    }

    /** {@link ElementMode#arbitrated} by default. */
    default ElementMode mode() {
        return ElementMode.arbitrated;
    }

    /**
     * Whether this element lives purely in world space (a billboard drawn at
     * the entity, with no screen placement to project or coordinate): the
     * coordinator bypasses the whole screen-arbitration flow for it — no
     * occupancy bitmap, no space budget, no ladder negotiation, no
     * relaxation — and grants its world candidate unconditionally. It still
     * proposes, still receives an {@code ElementState} every frame and still
     * drives the visibility tracker. Default false: the element projects and
     * arbitrates like every ordinary element.
     */
    default boolean worldOnly() {
        return false;
    }
}
