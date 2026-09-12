/**
 * World-space UI primitives — the lower of CloudLib's two in-world layers.
 * <p>
 * This package defines <em>what an in-world UI surface is</em> and how
 * widgets mounted on it may be interacted with; it deliberately knows nothing
 * about panel lifecycle, provisioning cadence or presentation policy — that
 * semantic layer lives in the consuming runtime (the reference implementation
 * is the {@code nimbusprojection} mod).
 *
 * <h3>Geometry</h3>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor} — where a UI
 *       binds: a block position (plus offset), an entity, or a dynamic world
 *       point. Resolved per frame so entity anchors track motion.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement} — where
 *       the surface sits relative to its anchor: flat on a block face,
 *       floating/following in screen space, docked into a screen corner, or
 *       expanded as a world-space hologram.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.Projection} — the
 *       conversion formulas between world space and gui-scaled screen space:
 *       world → screen point, screen → world ray, ray ↔ plane hit testing.</li>
 * </ul>
 *
 * <h3>Surface</h3>
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel} — the live
 *       handle a runtime hands back for a presented surface.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext} — the
 *       context object passed to content factories and interaction
 *       callbacks (level / player / panel / expose sync access).</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldOverlayScreen} —
 *       marker contract for transparent input-capture screens (e.g. a flat
 *       "inspect" projection that holds the cursor without pausing).</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.PanelChannel} — a panel's
 *       keyed client → server message pipe, for panels not backed by a
 *       synced block entity (which would use expose channels instead).</li>
 * </ul>
 *
 * <h3>Interaction contracts (widget-facing)</h3>
 * A widget opts into world interaction by implementing these; the runtime
 * discovers them by walking the hit widget's ancestor chain:
 * <ul>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable} — pointer
 *       strokes captured on the panel surface (Witness-style drawing).</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.WorldDraggable} /
 *       {@link dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag} — pressing a
 *       widget hands the drag off to the world: carried content floats at the
 *       view ray and world objects become drop targets.</li>
 *   <li>{@link dev.vfyjxf.cloudlib.api.ui.inworld.SplitPlan} — deterministic
 *       distribution math shared by client preview and server commit.</li>
 * </ul>
 */
package dev.vfyjxf.cloudlib.api.ui.inworld;
