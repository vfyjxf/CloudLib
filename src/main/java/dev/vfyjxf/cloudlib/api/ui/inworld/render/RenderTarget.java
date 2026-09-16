package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The destination a {@link UiRenderer} is bound to for one rendered frame.
 * <p>
 * In-world UI draws in two physical passes — a world pass inside the level
 * render (quads in world space, depth-competing with terrain) and a screen
 * pass with the gui (2D overlay geometry in gui-scaled pixels). The target
 * variant tells the renderer which pass it is in and hands it the pass's
 * drawing context; everything else about what gets drawn is the renderer's
 * business.
 */
public sealed interface RenderTarget {

    /** Viewport width in pixels (framebuffer px for world, gui-scaled px for screen). */
    int viewportW();

    /** Viewport height in pixels (framebuffer px for world, gui-scaled px for screen). */
    int viewportH();

    /** The frame's partial tick — for interpolating animated content. */
    float partialTick();

    /**
     * The world pass: drawing happens inside the level render, geometry is
     * emitted in world space (baked through the camera transform).
     *
     * @param camera     the frame's camera
     * @param projection the frame's world ↔ screen conversion
     * @param viewportW  framebuffer width in px
     * @param viewportH  framebuffer height in px
     * @param partialTick the frame's partial tick
     */
    record WorldTarget(Camera camera, Projection projection, int viewportW, int viewportH, float partialTick)
            implements RenderTarget {}

    /**
     * The screen pass: drawing happens with the gui, geometry is emitted in
     * gui-scaled pixels through the {@link GuiGraphics} pipeline.
     *
     * @param graphics   the gui draw context
     * @param viewportW  gui-scaled viewport width
     * @param viewportH  gui-scaled viewport height
     * @param partialTick the frame's partial tick
     */
    record ScreenTarget(GuiGraphics graphics, int viewportW, int viewportH, float partialTick)
            implements RenderTarget {}
}
