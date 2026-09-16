package dev.vfyjxf.cloudlib.ui.debug;

import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.ui.Textures;
import dev.vfyjxf.cloudlib.util.Locations;

/**
 * Pixel-art PNG icons for the DevTools overlay.
 * <p>
 * Backgrounds and scrollbar textures live in the shared {@link Textures}
 * class. Icons are 12x12 white-on-transparent images tinted at runtime via
 * {@link dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas#color(int)}.
 */
final class DevToolsTextures {

    private static final int iconSize = 12;

    static final VisualTexture pick = icon("pick");
    static final VisualTexture highlight = icon("highlight");
    static final VisualTexture refresh = icon("refresh");
    static final VisualTexture close = icon("close");
    static final VisualTexture more = icon("more");

    static final VisualTexture plus = icon("plus");
    static final VisualTexture minus = icon("minus");

    static final VisualTexture dockFloat = icon("dock_float");
    static final VisualTexture dockRight = icon("dock_right");
    static final VisualTexture dockLeft = icon("dock_left");
    static final VisualTexture dockTop = icon("dock_top");
    static final VisualTexture dockBottom = icon("dock_bottom");

    private static VisualTexture icon(String name) {
        return ImageTexture.of(Locations.ofMod("textures/gui/devtools/" + name + ".png"), iconSize, iconSize);
    }

    private DevToolsTextures() {}
}
