package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

/**
 * The level-of-detail rungs for an inworld element, in degrade order —
 * {@code ordinal()} <em>is</em> the degradation rank: full (0) is the
 * strongest presentation, hidden (4) the weakest. Degrading always moves to
 * a higher ordinal; a degradation loop is therefore impossible by
 * construction.
 * <ul>
 *   <li>{@link #full} — the complete panel</li>
 *   <li>{@link #compact} — the compact variant: trimmed content, smaller
 *       footprint</li>
 *   <li>{@link #icon} — icon only</li>
 *   <li>{@link #clustered} — collapsed into a cluster representative (the
 *       cluster's shared marker stands for the element)</li>
 *   <li>{@link #hidden} — not rendered (but still registered; see the
 *       coordinator's linger semantics)</li>
 * </ul>
 */
public enum LodTier {
    full, compact, icon, clustered, hidden;

    private static final LodTier[] degradeOrder = values();

    /**
     * The next rung down the ladder; {@code hidden} is terminal and degrades
     * to itself.
     */
    public LodTier degrade() {
        int next = ordinal() + 1;
        return next < degradeOrder.length ? degradeOrder[next] : this;
    }

    /** Whether there is a rung below this one. */
    public boolean canDegrade() {
        return this != hidden;
    }
}
