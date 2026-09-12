/**
 * The Nimbus in-world UI API — the semantic layer over CloudLib's
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld} primitives.
 * <p>
 * Positioning: Jade-style "look at a thing, get its UI" — but the UI is a
 * real CloudLib widget tree, presented in world space (face-mounted,
 * floating, following, docked or expanded as a hologram) and interacted
 * with through soft focus, an engage key, pointer, trace strokes and
 * world-drag gestures rather than pixel-perfect crosshair clicks.
 *
 * <h3>Layers</h3>
 * <ul>
 *   <li><b>CloudLib primitives</b> — anchors, placements, projection math,
 *       the live panel handle and the widget-facing interaction contracts
 *       ({@code InworldTraceable}, {@code WorldDraggable},
 *       {@code PanelChannel}).</li>
 *   <li><b>This API</b> — who provides panels and when they live or die
 *       ({@link dev.vfyjxf.nimbusprojection.api.provider}), what a panel
 *       looks like ({@link dev.vfyjxf.nimbusprojection.api.panel.PanelSpec}),
 *       and how panels talk across the network and stay consistent for
 *       every watching player
 *       ({@link dev.vfyjxf.nimbusprojection.api.sync}).</li>
 * </ul>
 *
 * <h3>Entry points</h3>
 * {@link dev.vfyjxf.nimbusprojection.api.Nimbus#client()} is the
 * client-side runtime; {@link dev.vfyjxf.nimbusprojection.api.Nimbus#server()}
 * is the server-side shared-panel registry. See {@code API.md} in the
 * project root for the full design.
 */
package dev.vfyjxf.nimbusprojection.api;
