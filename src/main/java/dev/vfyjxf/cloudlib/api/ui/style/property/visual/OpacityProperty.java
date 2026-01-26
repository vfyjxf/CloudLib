package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;

/**
 * Built-in opacity visual property.
 * <p>
 * Opacity is applied to the {@link VisualContext} for rendering.
 *
 * @see UIStyles#opacity(float)
 */
public record OpacityProperty(float opacity) implements VisualProperty {

    public static final StyleType<Float> type = StyleType.of("opacity", () -> 1.0f);

    public OpacityProperty(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.opacity(opacity);
    }

    @Override
    public String toString() {
        return String.valueOf(opacity);
    }
}
