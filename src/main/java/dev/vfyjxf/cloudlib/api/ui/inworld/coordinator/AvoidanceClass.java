package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

/**
 * How hard an element holds its screen rect against everyone else — the
 * element-level yield declaration (see
 * {@link InworldElement#avoidanceClass()}), orthogonal to the per-variant
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy}: the policy
 * says what an element does <em>inside</em> the arbitrated flow, the class
 * says whether it takes part in the contention at all. Ordered by how little
 * the element yields: {@code rigid} yields less than {@code standard}.
 * <ul>
 *   <li>{@link #rigid} — the element places itself. It never enters the
 *       candidate competition: every frame the coordinator grants its first
 *       screen candidate directly, as {@code anchor + declared offset} with
 *       only the work-area edge clamp applied (the clamp acts on the final
 *       rect alone — the declared offset is never rewritten, so the rect
 *       returns to the declared position the moment the anchor comes back
 *       from the edge; no re-pin, no clamp hysteresis). A rigid element
 *       <ol>
 *       <li>is never displaced by anyone: it is not a push target of the
 *           coincidence release and never moved by the residual-overlap
 *           separation;</li>
 *       <li>blocks nobody: its rect never enters another element's
 *           zone-overlap placed set, never acts as a non-pushable blocker
 *           and never marks the occupancy bitmap (nor the space-budget
 *           feedback);</li>
 *       <li>changes no arbitration order or priority — neither its own
 *           standing nor any other element's arbitration index.</li>
 *       </ol>
 *       These hold structurally: the coordinator routes rigid elements
 *       around the whole negotiation flow (like world-only elements, minus
 *       the world-half-only grant), so they never reach the commit that
 *       would mark the bitmap or enter the round's committed set. The
 *       element still proposes every frame, still receives an
 *       {@code ElementState} and still drives the visibility tracker, and
 *       its grant is readable from the {@code CoordinationResult} like any
 *       other placement — it is shared state for nobody else to consume.</li>
 *   <li>{@link #standard} — exactly the behavior before the class existed:
 *       full participation in arbitration, occupancy and relaxation.</li>
 * </ul>
 */
public enum AvoidanceClass {
    rigid,
    standard;

    /**
     * Whether this element's rect may block or displace others — the single
     * predicate every shared-state producer (occupancy bitmap, round
     * committed set, zone snapshot, budget) consults; {@code false} only for
     * {@link #rigid}.
     */
    public boolean blocksOthers() {
        return this == standard;
    }
}
