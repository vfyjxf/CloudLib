/**
 * The visual-zone layer of the inworld coordination system (Z1): pure screen
 * geometry that grades <em>where</em> a panel may sit, and the unified cost
 * model that turns candidate rectangles into one comparable score. Everything
 * operates on gui-scaled screen pixels in the standard GUI coordinate system
 * (origin top-left, x growing right, y growing down).
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.AttentionField} and its
 *       two implementations — the scalar screen-value field C(x, y), peaked at
 *       the crosshair: {@code GaussianAttention} (σ configurable) and
 *       {@code PiecewiseAttention} (center/mid/corner bands, boundary radii
 *       configurable)</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneModel} — the screen
 *       partition for one anchor: anchor neighborhood, D_max drift band, edge
 *       band with its safe rectangle, and the center attention field</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCandidates} — the
 *       alignment-semantic candidate lattice: eight directions × three
 *       distance tiers plus the in-place candidate, clamped into the safe
 *       rectangle and deduplicated</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCost} /
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneWeights} — the
 *       unified cost function: nine weighted, per-term normalized terms
 *       (anchor proximity, overlap, HUD exclusion, attention, out-of-bounds,
 *       leader length, temporal stability, leader crossings, topology
 *       preservation)</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisualBalance} — the
 *       whole-layout visual-balance metric: the nine-grid-weighted panel
 *       centroid's lateral-weighted L1 offset from the optical center, as a
 *       normalized score — the composition-level companion to ZoneCost's
 *       per-candidate terms (Zhang 2025 VME calibration)</li>
 *   <li>The presentation vocabulary:
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisibilityPolicy},
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.LodTier} (ordinal is
 *       the degrade order),
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.PlacementMode} (the
 *       world → screen ladder)</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.FocusBubble} — the
 *       radius-hysteresis state machine (expand &lt; collapse) for "is the
 *       anchor inside the focus bubble"</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.GazeRanking} — the
 *       crosshair ranking keys: screen distance → world distance → priority →
 *       registration order, plus the gaze-radius filter predicate</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.zone.PreviousFrameLayout} —
 *       the previous frame's committed layout as the zone vocabulary sees it
 *       (Z2): placement map, leader segments, left-of/above adjacency</li>
 * </ul>
 * <p>
 * Iron rules, mirroring the {@code space}/{@code algorithm} packages: pure
 * logic, no Minecraft types, no GL, no wall clock, no randomness —
 * headless-testable by construction. Since Z2 the layer is wired into the
 * element pipeline <em>opt-in only</em>: a spec's {@code ZoneFacet} binds
 * the {@code candidates.zoneGrid} / {@code rank.zoneCost} strategies and
 * scores against the coordinator's previous-frame snapshot; a spec without
 * the facet builds no zone context and runs no zone code path, so
 * populations without zone declarations behave exactly as before.
 */
@org.jspecify.annotations.NullMarked
package dev.vfyjxf.cloudlib.api.ui.inworld.zone;
