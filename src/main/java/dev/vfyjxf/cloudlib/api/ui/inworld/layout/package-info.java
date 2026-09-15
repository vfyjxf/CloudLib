/**
 * In-world UI layout — the half of the pipeline that decides <em>where
 * panels and their leader lines go</em>. Everything here is pure
 * spatial reasoning over a per-frame snapshot; rendering lives in the
 * sibling {@code render} package, which adapts solver output to
 * {@code WorldUiPanel} quads, {@code WorldLines} and
 * {@code SceneCanvas} strokes.
 * <p>
 * The vocabulary:
 * <ul>
 *   <li><b>LayoutFrame</b> — everything the solver sees for one frame:
 *       the clock, the {@code Projector} camera plus its world-space
 *       basis, HUD regions to avoid, {@code WorldObstacles}, samplable
 *       sources and the panel requests.</li>
 *   <li><b>SourceProvider / SourceSnapshot</b> — the thing a panel
 *       describes (an entity head, a block face, a held controller),
 *       sampled per frame. {@code Sources} builds the common shapes:
 *       point, segment, sphere, box, surface, composite.</li>
 *   <li><b>PanelRequest</b> — one UI surface to place: its space
 *       (world or screen), position/orientation policies, material,
 *       metrics, tier allowances and leader mode.</li>
 *   <li><b>Policies</b> — {@code PositionPolicy} offers candidate poses
 *       (nearby ring, mounted on the source surface, fixed, following,
 *       camera-relative); {@code OrientationPolicy} faces the result
 *       (toward the camera, fixed, source-aligned, readable both
 *       sides).</li>
 *   <li><b>InworldLayout</b> — the public entry point: it owns the
 *       engine plus persistent {@code LayoutState} (stability memory,
 *       debounce, route reuse), solves frames and returns a
 *       {@code PreparedLayout} view for hit-testing and rendering.</li>
 *   <li><b>LayoutResult</b> — the solver's output: visible
 *       {@code PanelPlacement}s (each carrying a world pose plus its
 *       projected screen polygon), routed {@code LeaderLine}s, the
 *       overflow dock/drawer, per-request addresses, transitions and
 *       diagnostics.</li>
 * </ul>
 * Nothing here touches GL or the render thread: the whole package runs
 * headless, which is what the offline test-suite exercises.
 */
package dev.vfyjxf.cloudlib.api.ui.inworld.layout;
