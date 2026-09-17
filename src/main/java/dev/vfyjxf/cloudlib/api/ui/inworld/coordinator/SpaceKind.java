package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

/**
 * Where an {@link InworldElement} lives. The declaration order is the first
 * key of the coordinator's total arbitration order (§3.2): world-anchored
 * elements resolve before camera-tracked ones, which resolve before screen
 * panels — world content is pinned to geometry and cannot move out of the
 * way, so it claims space first.
 */
public enum SpaceKind {
    world,
    tracked,
    panel
}
