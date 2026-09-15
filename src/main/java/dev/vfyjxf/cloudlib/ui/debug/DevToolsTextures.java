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

    private static final int ICON_SIZE = 12;

    static final VisualTexture PICK = icon("pick");
    static final VisualTexture HIGHLIGHT = icon("highlight");
    static final VisualTexture REFRESH = icon("refresh");
    static final VisualTexture CLOSE = icon("close");
    static final VisualTexture MORE = icon("more");

    static final VisualTexture PLUS = icon("plus");
    static final VisualTexture MINUS = icon("minus");

    static final VisualTexture DOCK_FLOAT = icon("dock_float");
    static final VisualTexture DOCK_RIGHT = icon("dock_right");
    static final VisualTexture DOCK_LEFT = icon("dock_left");
    static final VisualTexture DOCK_TOP = icon("dock_top");
    static final VisualTexture DOCK_BOTTOM = icon("dock_bottom");

    private static VisualTexture icon(String name) {
        return ImageTexture.of(Locations.ofMod("textures/gui/devtools/" + name + ".png"), ICON_SIZE, ICON_SIZE);
    }

    private DevToolsTextures() {
    }

}
