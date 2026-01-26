package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;

/**
 * Built-in background color visual property.
 * <p>
 * Background is applied to the {@link VisualContext} for rendering.
 */
public record BackgroundProperty(VisualTexture background) implements VisualProperty {

    public static final StyleType<VisualTexture> type = StyleType.of("background", () -> VisualTexture.empty);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setBackground(background);
    }

    @Override
    public String toString() {
        return background.toString();
    }

}
