package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by panel content that can hand a press off into the world as a
 * {@link WorldDrag} — the "world is UI" counterpart of
 * {@link InworldTraceable}.
 * <p>
 * When the player presses a mouse button over an in-world panel, the runtime
 * walks the hit widget's ancestor chain looking for a {@code WorldDraggable}.
 * Returning a drag session converts the press into a world-targeted transfer:
 * while the button is held the runtime raycasts the player's view, collects
 * crossed item containers (anything exposing an item-handler capability) into
 * a trail, renders the carried stack floating at the ray hit point and, on
 * release, sends the commit request to the server. The server is authoritative:
 * it re-reads the source slot, re-validates reach and re-inserts the items.
 * <p>
 * Returning {@code null} keeps the press on the normal click path
 * ({@code scene.mouseClicked}), so a widget can mix regular clicking and
 * world-dragging by region or modifier.
 * <p>
 * Distinction from {@link InworldTraceable}: tracing keeps the pointer locked
 * onto the panel surface (puzzle-style gestures on the UI itself) while
 * world-dragging leaves the UI and treats world objects as the drop targets —
 * no capture screen is involved and the camera stays free.
 */
public interface WorldDraggable {

    /**
     * Called when a mouse button is pressed over this widget (or one of its
     * descendants) in world mode. Return a {@link WorldDrag} to start a
     * world-targeted drag, or {@code null} to let the press behave as a
     * regular click.
     *
     * @param sceneX scene-space x of the press
     * @param sceneY scene-space y of the press
     * @param button 0 = left, 1 = right (vanilla pickup semantics)
     */
    @Nullable
    WorldDrag beginWorldDrag(InworldPanelContext ctx, double sceneX, double sceneY, int button);

    /** Walks a hit widget's ancestor chain for the nearest draggable. */
    static @Nullable WorldDraggable find(@Nullable Widget hit) {
        Widget w = hit;
        while (w != null) {
            if (w instanceof WorldDraggable d) return d;
            w = w.parent();
        }
        return null;
    }
}
