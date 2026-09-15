package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;

/**
 * Built-in icon/foreground texture visual property.
 * <p>
 * Icon is rendered on top of the background and is commonly used for
 * widget icons, images, or decorative elements.
 */
public record IconProperty(VisualTexture icon) implements VisualProperty {

    public static final StyleType<VisualTexture> type = StyleType.of("icon", () -> VisualTexture.empty);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setIcon(icon);
    }

    @Override
    public String toString() {
        return icon.toString();
    }
}
