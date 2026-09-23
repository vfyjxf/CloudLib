/**
 * Grouping, clustering and orbiting (§3.7): which elements belong together
 * ({@link dev.vfyjxf.cloudlib.api.ui.inworld.group.InworldGroup}) and how a
 * group's members are arranged relative to each other
 * ({@link dev.vfyjxf.cloudlib.api.ui.inworld.group.GroupStrategy}:
 * orbit rings with sticky-greedy slot assignment, evolutionary clustering
 * with a "+N" representative, a vertical stack, or no grouping). The
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.group.GroupLayoutEngine} carries
 * the per-group cross-epoch state (slot incumbents, cluster history) that
 * keeps arrangement stable.
 * <p>
 * Pure logic: no Minecraft types, no clock — headless-testable.
 */
@org.jspecify.annotations.NullMarked
package dev.vfyjxf.cloudlib.api.ui.inworld.group;
