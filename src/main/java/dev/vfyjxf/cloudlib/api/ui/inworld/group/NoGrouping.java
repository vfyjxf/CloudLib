package dev.vfyjxf.cloudlib.api.ui.inworld.group;

/**
 * {@link GroupStrategy} for elements that explicitly decline grouping: every
 * member is placed at its own position with no aggregation and no shared
 * slots — the identity tuple still exists (so a group-scoped driver can
 * address the members), but arrangement is independent.
 */
public record NoGrouping() implements GroupStrategy {

    /** The singleton instance. */
    public static final NoGrouping instance = new NoGrouping();
}
