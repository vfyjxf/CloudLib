package dev.vfyjxf.cloudlib.api.ui.widgets;

import org.jetbrains.annotations.Range;

/**
 * Represents a layer in the UI.
 */
public class Layer {

    @Range(from = 0, to = 100)
    private final int layer;

    @SuppressWarnings("all")
    public Layer(@Range(from = 0, to = 100) int layer) {
        if (layer < 0 || layer > 100) {
            throw new IllegalArgumentException("Layer must be between 0 and 100");
        }
        this.layer = layer;
    }


    @Range(from = 0, to = 100)
    public int getLayer() {
        return layer;
    }


}
