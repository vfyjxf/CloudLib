package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.render.CustomRenderer;

/**
 * An inline custom-rendered box: the layouter reserves the given box, the renderer
 * callback draws into it each frame. This is the extension hook for arbitrary 2D/3D
 * content that the built-in node kinds do not cover.
 */
public record CustomRenderNode(int width, int height, CustomRenderer renderer) implements RichNode {

    public CustomRenderNode {
        if (renderer == null) throw new NullPointerException("renderer");
    }
}
