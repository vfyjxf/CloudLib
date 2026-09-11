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
}
