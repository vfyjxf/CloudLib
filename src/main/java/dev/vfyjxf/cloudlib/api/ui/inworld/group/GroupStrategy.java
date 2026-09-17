package dev.vfyjxf.cloudlib.api.ui.inworld.group;

/**
 * How the members of an {@link InworldGroup} are arranged relative to each
 * other (§3.7). The closed set of strategies — third parties extend grouping
 * behavior through the layout side's named-strategy catalogs, not by adding
 * implementations of this interface.
 *
 * @see OrbitAroundAnchor ring slots with sticky-greedy assignment
 * @see ClusterToRepresentative evolutionary clustering with a "+N"
 *      representative
 * @see StackInColumn a vertical stack with overflow aggregation
 * @see NoGrouping members placed independently
 */
public sealed interface GroupStrategy permits OrbitAroundAnchor, ClusterToRepresentative, StackInColumn, NoGrouping {}
