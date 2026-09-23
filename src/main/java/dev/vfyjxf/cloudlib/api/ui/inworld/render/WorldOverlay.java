package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import net.minecraft.world.phys.Vec3;

/**
 * One frame of host geometry that belongs to the in-world UI's world pass but
 * is not one of its {@link WorldUiPanel} surfaces: selection brackets,
 * connector strokes, source marks — anything a host draws in world space
 * beside the panels.
 * <p>
 * It joins the pass's single far → near sequence (see {@link DepthOrder})
 * instead of drawing in a pass of its own. That is the whole point: the pass
 * writes no depth, so an unsorted side pass would composite its geometry over
 * whatever the sorted sequence had already drawn — a far bracket landing on
 * top of a near translucent panel. Handing the geometry to
 * {@link WorldUiRenderer#overlays()} puts it in the same ordering domain as
 * the panels, so occlusion between the two holds whichever side owns the
 * nearer surface.
 * <p>
 * <b>Registration.</b> Overlays are per frame: a host registers its items
 * from its own level-stage listener, which must run before the renderer's
 * (the renderer listens at {@code EventPriority.LOW}), and the pass clears
 * the list after it draws — re-registering every frame is what keeps a host
 * that stops submitting from leaving stale geometry behind.
 * <p>
 * <b>Drawing.</b> {@link #draw(WorldUiPanel.Frame)} runs at the item's sorted
 * position with the world-pass baseline state held: depth test {@code LEQUAL}
 * against the completed scene, no depth writes, straight-alpha blending, cull
 * on — the same policy the panel quads draw under. An overlay sets whatever
 * it needs beyond that and leaves the state as it found it.
 */
public interface WorldOverlay {

    /**
     * The distance, in blocks, the pass sorts this item by: how far the item's
     * representative point sits from the frame's camera.
     * <p>
     * The pass draws everything farther before it, so this key decides what the item
     * can occlude — whatever the pass drew before it is behind it. A compact item keys
     * by its center; an item whose geometry spans a range — a stroke from a near panel
     * corner to a far anchor — keys by its <em>farthest</em> point, because a key nearer
     * than part of that geometry would have that part drawn after surfaces it is behind.
     */
    double sortDistance(Vec3 cameraPos);

    /**
     * Draws this item at its sorted position. The frame is the pass's own —
     * the same one the panels placed against this frame, so geometry resolved
     * inside the draw stays consistent with the panels' quads.
     */
    void draw(WorldUiPanel.Frame frame);
}
