/**
 * World-space UI primitives — the lower of CloudLib's two in-world layers.
 * <p>
 * This package defines <em>what an in-world UI surface is</em> and how
 * widgets mounted on it may be interacted with; it deliberately knows
 * nothing about panel lifecycle, provisioning cadence or presentation
 * policy — that semantic layer lives in the consuming runtime (the
 * reference implementation is the {@code nimbusprojection} mod).
 *
 * <h3>Identity</h3>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey} — the typed
 *       panel identity ({@code namespace:path}); trivially serializable,
 *       so local and network-shared panels share one key model.</li>
 * </ul>
 *
 * <h3>Geometry</h3>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor} — where a
 *       UI binds: a block position, an entity, or a dynamic world point.
 *       Open interface; custom anchors become shareable by registering an
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.AnchorCodec} on
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.AnchorCodecs}.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.Presentation} — where the
 *       surface sits relative to its anchor: face / floating / follow /
 *       dock / expand / inspect-only descriptors. Open interface; custom
 *       descriptors pair with a runtime driver.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.Projection} — the
 *       conversion formulas between world space and gui-scaled screen
 *       space.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.LayoutHint} — how a panel
 *       participates in layout: zoning, folding, off-screen collapse and
 *       occlusion policy ({@link dev.vfyjxf.cloudlib.api.ui.inworld.OcclusionClass}).</li>
 * </ul>
 *
 * <h3>Surface</h3>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel} — the live
 *       handle a runtime hands back for a presented surface.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext} —
 *       the context object passed to content factories and interaction
 *       callbacks (level / player / panel / channel / expose sync).</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldOverlayScreen} —
 *       marker contract for transparent input-capture screens.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.PanelChannel} — a panel's
 *       keyed client → server message pipe.</li>
 * </ul>
 *
 * <h3>Interaction contracts (widget-facing)</h3>
 * A widget opts into world interaction by implementing these; the runtime
 * discovers them by walking the hit widget's ancestor chain:
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable} —
 *       pointer strokes captured on the panel surface.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable} /
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag} — press hands
 *       the drag off to the world: carried content floats at the view ray
 *       and world objects become drop targets.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.SplitPlan} —
 *       deterministic distribution math shared by client preview and
 *       server commit.</li>
 * </ul>
 */
package dev.vfyjxf.cloudlib.api.ui.inworld;
