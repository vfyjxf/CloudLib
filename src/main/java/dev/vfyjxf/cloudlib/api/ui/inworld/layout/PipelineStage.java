package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The assembled pipeline's named stages. The element-side half
 * (ANCHOR→PROJECT→CANDIDATES→AVOID→RANK) runs inside the assembled
 * element's propose callback against the frame's
 * {@link LayoutEnvironment}; the coordinator-side half
 * (ARBITRATE→STABILIZE→COMMIT) runs when the coordinator drives the frame —
 * the {@code PipelineAssembler} connects the two halves.
 */
public enum PipelineStage {
    /** Resolve the declared anchor to this frame's screen position and world box. */
    anchor,
    /** Derive the variant's arbitration footprint (the dual representation's screen half). */
    project,
    /** Generate candidate placements from the profile's named candidate strategy. */
    candidates,
    /** Filter candidates against exclusions and avoided layers' occupancy. */
    avoid,
    /** Order candidates by the named ranking strategy (incumbent-aware). */
    rank,
    /** The coordinator's negotiation: fit, reject, degrade, grant. */
    arbitrate,
    /** The coordinator's gates, springs, FLIP and visibility budget. */
    stabilize,
    /** The coordinator's atomic result commit. */
    commit
}
