package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.render.QuadBasis;

/**
 * Where a {@link Layout} decided a UI surface lands this frame.
 * <p>
 * The variant also settles how the surface renders: a {@link WorldQuad} draws
 * in the world pass, a {@link ScreenRect} in the screen pass, and
 * {@link Coordinated} defers placement to the inscreen coordinator.
 */
public sealed interface LayoutOutput {

    /** World-space placement: the surface's quad in the level. */
    record WorldQuad(QuadBasis quad) implements LayoutOutput {}

    /**
     * Screen-space placement in gui-scaled pixels.
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  panel width in px
     * @param height panel height in px
     */
    record ScreenRect(double x, double y, int width, int height) implements LayoutOutput {}

    /**
     * Placement deferred to the inscreen coordinator — the layout declines to
     * pick a spot and hands the surface over with its
     * {@link dev.vfyjxf.cloudlib.api.ui.inworld.layout.inscreen.ScreenHints}.
     */
    record Coordinated() implements LayoutOutput {}
}
