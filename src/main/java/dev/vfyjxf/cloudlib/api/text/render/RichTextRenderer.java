package dev.vfyjxf.cloudlib.api.text.render;

import dev.vfyjxf.cloudlib.api.text.layout.LaidOutText;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;

/**
 * Renders a laid-out rich text document onto a {@link SceneCanvas} at the given
 * local position.
 * <p>
 * The default implementation is
 * {@link dev.vfyjxf.cloudlib.text.DefaultRichTextRenderer}; custom renderers can
 * restyle or replace individual fragment kinds.
 */
public interface RichTextRenderer {

    void render(SceneCanvas canvas, LaidOutText laidOut, float x, float y, RenderOptions options);
}
