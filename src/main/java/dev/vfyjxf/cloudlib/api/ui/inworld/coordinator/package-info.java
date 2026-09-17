/**
 * The inworld coordination system's entry point package: the
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator}
 * seven-phase arbitration pipeline and the negotiation protocol it drives
 * elements through. Everything here is pure logic — no Minecraft types, no
 * wall clock, no randomness — and headless-testable by construction.
 * <p>
 * The protocol in one breath: elements
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldElement#propose}
 * candidates (each in the dual representation — world
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.WorldAabb} plus
 * projected screen rect); the coordinator arbitrates in a total order
 * ({@code spaceKind → priority → sticky → registration}), grants what fits
 * against the occupancy bitmap and exclusion areas, and answers the rest with
 * structured {@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementRejection}s;
 * rejected elements re-propose degraded
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.VariantLadder} rungs
 * in the renegotiation round; everything still standing at the frame deadline
 * commits atomically, and hidden elements linger instead of blinking out.
 * Placements are stored as offsets from the anchor projection (never
 * absolute screen coordinates), the continuous layer follows with
 * warm-started springs and FLIP morphs, and the space feedback
 * ({@link dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceBudget}) lets
 * elements degrade proactively before ever being rejected.
 */
@dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault
package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;
