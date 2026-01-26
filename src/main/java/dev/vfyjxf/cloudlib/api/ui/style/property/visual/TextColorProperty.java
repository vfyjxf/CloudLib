package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;

/**
 * Built-in text color visual property.
 * <p>
 * Text color is applied to the {@link VisualContext} for rendering.
 *
 * @see UIStyles#textColor(int)
 */
public record TextColorProperty(int color) implements VisualProperty {

    public static final StyleType<Integer> type = StyleType.of("textColor", () -> 0);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.textColor(color);
    }

    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return String.format("0x%08X", color);
    }
}
