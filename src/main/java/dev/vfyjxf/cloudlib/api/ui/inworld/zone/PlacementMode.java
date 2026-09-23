package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

/**
 * Where an element's placement lives, as a five-rung ladder from
 * world-pinned to screen-pinned — the vocabulary for the
 * world-attachment-to-edge-proxy escalation. Declaration order is the ladder
 * order; {@link #nextTowardsScreen()} walks one rung toward the screen end.
 * <ul>
 *   <li>{@link #worldAttached} — pinned to the world geometry at the anchor
 *       (a block face panel); moves exactly as the world projects</li>
 *   <li>{@link #worldFloating} — anchored in world space but free to float
 *       around the anchor when the attached spot does not fit</li>
 *   <li>{@link #screenAnchored} — screen space, docked to the anchor's
 *       projection by alignment semantics (the candidate lattice)</li>
 *   <li>{@link #screenDisplaced} — screen space, drifted from the projection
 *       within the D_max drift band to escape collisions</li>
 *   <li>{@link #screenEdge} — screen space, collapsed onto the screen edge
 *       (an edge proxy); the last stop before the element hides</li>
 * </ul>
 */
public enum PlacementMode {
    worldAttached, worldFloating, screenAnchored, screenDisplaced, screenEdge;

    private static final PlacementMode[] ladder = values();

    /**
     * The next rung toward the screen end of the ladder;
     * {@code screenEdge} is terminal and returns itself.
     */
    public PlacementMode nextTowardsScreen() {
        int next = ordinal() + 1;
        return next < ladder.length ? ladder[next] : this;
    }

    /** Whether this rung is on the world half of the ladder. */
    public boolean isWorldSpace() {
        return this == worldAttached || this == worldFloating;
    }

    /** Whether this rung is on the screen half of the ladder. */
    public boolean isScreenSpace() {
        return !isWorldSpace();
    }
}
