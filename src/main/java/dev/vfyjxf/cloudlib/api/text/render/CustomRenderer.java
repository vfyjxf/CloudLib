package dev.vfyjxf.cloudlib.api.text.render;

import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;

/**
 * Draw callback for
 * {@link dev.vfyjxf.cloudlib.api.text.CustomRenderNode}.
 * <p>
 * Coordinates are the fragment rect in the rich text widget's local space; the
 * canvas transform is already positioned at the widget origin.
 */
@FunctionalInterface
public interface CustomRenderer {

    void render(SceneCanvas canvas, float x, float y, float width, float height);
}
