package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

/**
 * How an element's presentation responds to losing line of sight or leaving
 * the screen — the visual-zone vocabulary for the occlusion/off-screen
 * decisions:
 * <ul>
 *   <li>{@link #hardOcclusion} — binary: occluded means fully invisible; the
 *       element reappears only with a clear line of sight. For elements whose
 *       world identity must never lie (a block face label)</li>
 *   <li>{@link #fade} — alpha fade under partial occlusion, tracking the
 *       occluded fraction. The default for panels</li>
 *   <li>{@link #occludedIndicator} — the panel fades but a marker (silhouette
 *       / x-ray outline) stays at the occluded position. For elements that
 *       must remain findable but not readable</li>
 *   <li>{@link #edgeProxy} — off-screen means an edge indicator proxy: the
 *       element collapses to a docked indicator on the screen edge nearest
 *       its anchor. For trackers and waypoint-style elements</li>
 *   <li>{@link #semanticVisible} — always rendered, occlusion ignored: the
 *       element's semantics (a quest marker, a party member beacon) outrank
 *       geometry</li>
 * </ul>
 * The ladder is advisory vocabulary: since Z2 a {@code ZoneFacet} carries
 * the declared policy, and the coordinator's stabilize phase consumes it in
 * a later stage — this layer assigns no policy itself.
 */
public enum VisibilityPolicy {
    hardOcclusion,
    fade,
    occludedIndicator,
    edgeProxy,
    semanticVisible
}
