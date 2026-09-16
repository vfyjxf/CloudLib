package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.client.Camera;

/**
 * Everything a {@link Layout} needs to resolve one frame — nothing more.
 * <p>
 * Gui-scaled screen dimensions come from {@link Projection#screenWidth()} /
 * {@link Projection#screenHeight()}; {@code viewportW}/{@code viewportH} are
 * the raw framebuffer pixels, for content that sizes against the physical
 * viewport.
 *
 * @param projection  the frame's world ↔ screen conversion
 * @param camera      the frame's camera
 * @param viewportW   framebuffer width in px
 * @param viewportH   framebuffer height in px
 * @param panelW      the panel's width in gui px
 * @param panelH      the panel's height in gui px
 * @param partialTick the frame's partial tick
 */
public record LayoutContext(
        Projection projection,
        Camera camera,
        int viewportW,
        int viewportH,
        int panelW,
        int panelH,
        float partialTick) {}
