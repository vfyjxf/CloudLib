package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

/**
 * Why the coordinator could not grant a proposal — the structured half of the
 * negotiation fuel. {@link VariantLadder#degrade} reads the reason to decide
 * how far to jump down the ladder (a spatial squeeze skips rungs; a plain
 * overlap usually costs one).
 */
public enum RejectionReason {
    /** Another committed element's rect blocks the candidate. */
    overlap,
    /** A registered exclusion area blocks the candidate. */
    exclusion,
    /** The candidate cannot be brought inside the work area. */
    outOfBounds,
    /** The granted rect would be smaller than the variant's comfortable minimum. */
    insufficientArea
}
