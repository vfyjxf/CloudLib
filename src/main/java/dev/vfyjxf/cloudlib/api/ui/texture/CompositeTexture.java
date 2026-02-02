package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Composite texture that layers multiple textures together.
 * Renders in order: first added = bottom layer.
 */
public class CompositeTexture implements BatchableTexture {

    private final List<BatchableTexture> layers;

    private CompositeTexture(List<BatchableTexture> layers) {
        this.layers = List.copyOf(layers);
    }

    // region Factory

    public static CompositeTexture of(BatchableTexture... layers) {
        return new CompositeTexture(Arrays.asList(layers));
    }

    public static CompositeTexture of(List<BatchableTexture> layers) {
        return new CompositeTexture(layers);
    }

    public static CompositeTexture empty() {
        return new CompositeTexture(Collections.emptyList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CompositeTexture panel(int backgroundColor, int borderColor, int borderThickness) {
        return builder()
            .add(new ColorTexture(backgroundColor))
            .add(BorderTexture.of(borderColor, borderThickness))
            .build();
    }

    public static CompositeTexture gradientPanel(int colorTop, int colorBottom, int borderColor, int borderThickness) {
        return builder()
            .add(GradientTexture.vertical(colorTop, colorBottom))
            .add(BorderTexture.of(borderColor, borderThickness))
            .build();
    }

    // endregion

    // region Query

    public List<BatchableTexture> layers() {
        return layers;
    }

    public int layerCount() {
        return layers.size();
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    @Nullable
    public BatchableTexture layer(int index) {
        return (index >= 0 && index < layers.size()) ? layers.get(index) : null;
    }

    // endregion

    // region Modification

    public CompositeTexture withLayer(BatchableTexture layer) {
        List<BatchableTexture> newLayers = new ArrayList<>(layers);
        newLayers.add(layer);
        return new CompositeTexture(newLayers);
    }

    public CompositeTexture withoutLayer(int index) {
        if (index < 0 || index >= layers.size()) return this;
        List<BatchableTexture> newLayers = new ArrayList<>(layers);
        newLayers.remove(index);
        return new CompositeTexture(newLayers);
    }

    // endregion

    // region BatchableTexture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        for (BatchableTexture layer : layers) {
            layer.emit(emitter, x, y, width, height, tint);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        for (BatchableTexture layer : layers) {
            layer.render(graphics, x, y, width, height);
        }
    }

    @Override
    public boolean supportsBatching() {
        for (BatchableTexture layer : layers) {
            if (!layer.supportsBatching()) return false;
        }
        return true;
    }

    // endregion

    // region Builder

    public static class Builder {
        private final List<BatchableTexture> layers = new ArrayList<>();

        private Builder() {}

        public Builder add(BatchableTexture layer) {
            layers.add(layer);
            return this;
        }

        public Builder addColor(int color) {
            return add(new ColorTexture(color));
        }

        public Builder addBorder(int color, int thickness) {
            return add(BorderTexture.of(color, thickness));
        }

        public Builder addGradient(int colorTop, int colorBottom) {
            return add(GradientTexture.vertical(colorTop, colorBottom));
        }

        public Builder addIf(boolean condition, BatchableTexture layer) {
            if (condition) layers.add(layer);
            return this;
        }

        public CompositeTexture build() {
            return new CompositeTexture(layers);
        }
    }

    // endregion
}
