/**
 * Reserved — view-tracking HUD projection.
 * <p>
 * The sibling of in-world layout: UI pinned to the observer's view (the
 * "projection" in the mod's name) rather than anchored to world positions —
 * screen-stuck overlays, look-following reticles, HUD-locked panels that
 * render <em>as if</em> in the world.
 * <p>
 * Not implemented. The layout package already exposes what a future
 * projection layer needs — {@code SourceProvider}s for sampling and
 * {@code Space.screen} requests for camera-relative placement — so
 * projection can build on the same frame solver.
 */
package dev.vfyjxf.cloudlib.api.ui.inworld.projection;
