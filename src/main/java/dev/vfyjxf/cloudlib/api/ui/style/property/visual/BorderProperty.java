package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Built-in border property.
 * <p>
 * This property intentionally affects both:
 * <ul>
 *   <li><b>Layout</b>: writes to taffy {@link TaffyStyle#border} so border participates in box model.</li>
 *   <li><b>Rendering</b>: writes to {@link VisualContext} so widgets can draw a border.</li>
 * </ul>
 * <p>
 * If you only want a visual outline without affecting layout size, consider creating a separate
 * visual-only property.
 */
public record BorderProperty(float width, int color) implements LayoutProperty, VisualProperty {

    public static final StyleType<BorderProperty> type = StyleType.of("border", () -> null);

    public BorderProperty(float width, int color) {
        this.width = Math.max(0.0f, width);
        this.color = color;
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void apply(StyleContext context) {
        context.applyGeneric(this);
        applyToStyle(context.layoutStyle());
        applyToWidget(context.visualContext());
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        var v = LengthPercentage.length(width);
        style.border.top = v;
        style.border.right = v;
        style.border.bottom = v;
        style.border.left = v;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.border(width, color);
    }

    @Override
    public String toString() {
        return width + " " + String.format("0x%08X", color);
    }

}
