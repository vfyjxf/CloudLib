package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.ui.texture.SizedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;

/**
 * An inline image with an explicit reserved box, rendered through the texture
 * pipeline (batchable when the texture supports it).
 */
public record ImageNode(VisualTexture texture, int width, int height) implements RichNode {

    public ImageNode {
        if (texture == null) throw new NullPointerException("texture");
    }

    public ImageNode(SizedTexture texture) {
        this(texture, texture.width(), texture.height());
    }
}
