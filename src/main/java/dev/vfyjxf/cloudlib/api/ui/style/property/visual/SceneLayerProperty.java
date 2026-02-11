package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;

/**
 * Style property for setting the scene layer of a widget.
 * <p>
 * Scene layers control the order in which widgets are rendered.
 * Widgets in higher priority layers render on top of widgets in lower priority layers.
 * <p>
 * Note: SceneLayer is now managed directly by Widget, not through VisualContext.
 * This property applies the layer to the widget directly when applied.
 *
 * @see SceneLayer
 */
public record SceneLayerProperty(SceneLayer layer) implements StyleProperty {

    public static final StyleType<SceneLayer> type = StyleType.of(
        "sceneLayer",
        () -> SceneLayer.content,
        (ctx, value) -> ctx.widget().setSceneLayer(value)
    );

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void apply(StyleContext context) {
        // Use set() to trigger change listeners and apply to widget
        context.set(type, layer);
    }

    @Override
    public String toString() {
        return "sceneLayer(" + layer.name() + ")";
    }
}
