package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.group.GroupStrategy;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.InworldGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.NoGrouping;

import java.util.Objects;

/**
 * The group facet: the element's group identity and arrangement strategy
 * (§3.7). Elements sharing an {@link InworldGroup} are arranged relative to
 * each other by that group's {@link GroupStrategy} — orbit rings, clustering
 * with a representative, a column stack, or none.
 *
 * @param group the group identity
 * @param strategy the arrangement strategy for the group's members
 */
public record GroupFacet(InworldGroup group, GroupStrategy strategy) {

    public GroupFacet {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(strategy, "strategy");
    }

    /** Ungrouped: the default posture. */
    public static GroupFacet none() {
        return new GroupFacet(new InworldGroup("ungrouped", "none", "singleton"), NoGrouping.instance);
    }

    /** A grouped facet. */
    public static GroupFacet of(InworldGroup group, GroupStrategy strategy) {
        return new GroupFacet(group, strategy);
    }
}
