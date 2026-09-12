package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddleware;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * How a {@link InworldPanel} is presented in "world" presentation mode.
 * <p>
 * In "inspect" presentation mode (the hold-key flat projection) every panel is
 * flattened to screen space regardless of its placement; the placement only
 * describes where the flat panel is born relative to its projected anchor.
 */
public sealed interface InworldPlacement {

    //region factories

    /**
     * Panel is attached flat onto a block face, rendered in world space
     * and interacted with via the crosshair ray.
     *
     * @param face           which face of the anchored block the panel sits on
     * @param u              horizontal position of the panel center on the face, 0..1
     * @param v              vertical position of the panel center on the face, 0..1
     * @param pixelsPerBlock how many gui pixels fit into one block (panel scale)
     */
    static Face face(Direction face, double u, double v, double pixelsPerBlock) {
        return new Face(face, u, v, pixelsPerBlock);
    }

    /** A face panel centered on the face at 48 px/block. */
    static Face face(Direction face) {
        return new Face(face, 0.5, 0.5, 48);
    }

    /**
     * Panel is projected to screen space and positioned near the anchor's
     * projected point using the {@link dev.vfyjxf.cloudlib.api.ui.floating}
     * middleware pipeline.
     */
    static Floating floating(FloatingPlacement placement, FloatingMiddleware... middlewares) {
        return new Floating(placement, List.of(middlewares));
    }

    /** Floating panel that auto-picks a side near the projected anchor. */
    static Floating floating() {
        return new Floating(
                FloatingPlacement.rightStart,
                List.of(
                        FloatingMiddlewares.offset(14),
                        FloatingMiddlewares.flip(),
                        FloatingMiddlewares.shift(4),
                        FloatingMiddlewares.hide()
                )
        );
    }

    /**
     * Panel follows the anchor's projected screen point with a fixed offset
     * (nameplate-style).
     */
    static Follow follow(double offsetX, double offsetY) {
        return new Follow(offsetX, offsetY);
    }

    /** Panel pinned directly above the anchor's projected point. */
    static Follow follow() {
        return new Follow(0, 0);
    }

    /**
     * Panel docks to a fixed screen corner/edge — the stable "pinned to the
     * screen" presentation: panels stack from the corner outward and a leader
     * line connects back to the anchor's scan frame. The docked position is
     * independent of camera micro-motion, so it never jitters.
     */
    static Dock dock(DockCorner corner) {
        return new Dock(corner);
    }

    /** Dock that auto-picks the screen quadrant nearest the projected anchor. */
    static Dock dock() {
        return new Dock(DockCorner.AUTO);
    }

    /**
     * World-space expanded panel: floats at a free spot near the anchor,
     * billboarded toward the player — a holographic "big screen" the block's
     * compact face controller opens on demand.
     */
    static Expand expand() {
        return new Expand(72);
    }

    static Expand expand(double pixelsPerBlock) {
        return new Expand(pixelsPerBlock);
    }

    //endregion

    /**
     * A panel drawn in world space onto a block face.
     *
     * @param face           the face the panel is attached to
     * @param u              horizontal position of the panel center on the face (0..1)
     * @param v              vertical position of the panel center on the face (0..1)
     * @param pixelsPerBlock gui pixels per block; panel world size = panelPx / pixelsPerBlock
     */
    record Face(Direction face, double u, double v, double pixelsPerBlock) implements InworldPlacement {
    }

    /**
     * A screen-space panel placed relative to the anchor's projected position
     * through the floating middleware pipeline.
     */
    record Floating(FloatingPlacement placement,
                    List<FloatingMiddleware> middlewares) implements InworldPlacement {
    }

    /**
     * A screen-space panel pinned to the anchor's projected position plus a
     * fixed pixel offset.
     */
    record Follow(double offsetX, double offsetY) implements InworldPlacement {
    }

    /**
     * A screen-space panel docked into a fixed screen corner. {@link DockCorner#AUTO}
     * picks the corner on the same side as the anchor's projection; multiple
     * panels in one corner stack vertically in offer order.
     */
    record Dock(DockCorner corner) implements InworldPlacement {
    }

    /**
     * A world-space panel floating at a free spot near its anchor — the
     * manager scans candidate positions around the anchor for air, then
     * billboards the panel toward the player. Rendered through the same
     * render-to-texture quad path as {@link Face} and clickable via crosshair
     * raycast. {@code pixelsPerBlock} sets the world size.
     */
    record Expand(double pixelsPerBlock) implements InworldPlacement {
    }

    /** Which screen corner a {@link Dock} panel pins to. */
    enum DockCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        /** Pick the quadrant the anchor projects into (falls back when off-screen). */
        AUTO
    }
}
