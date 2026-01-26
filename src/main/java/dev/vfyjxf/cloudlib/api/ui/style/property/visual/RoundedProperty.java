package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;

/**
 * Built-in border radius visual property.
 * <p>
 * Border radius is applied to the {@link VisualContext} for rendering.
 *
 * @see UIStyles#rounded(float)
 */
public record RoundedProperty(float topLeft, float topRight, float bottomRight, float bottomLeft) implements VisualProperty {

    public static final StyleType<Float> type = StyleType.of("borderRadius", () -> 0.0f);

    public RoundedProperty(float all) {
        this(all, all, all, all);
    }

    public RoundedProperty(float topLeftBottomRight, float topRightBottomLeft) {
        this(topLeftBottomRight, topRightBottomLeft, topLeftBottomRight, topRightBottomLeft);
    }

    public RoundedProperty(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.borderRadius(topLeft, topRight, bottomRight, bottomLeft);
    }

    @Override
    public String toString() {
        if (topLeft == topRight && topLeft == bottomRight && topLeft == bottomLeft) {
            return String.valueOf(topLeft);
        }
        return topLeft + ", " + topRight + ", " + bottomRight + ", " + bottomLeft;
    }
}
