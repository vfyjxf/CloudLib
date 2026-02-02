package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;

/**
 * Built-in shadow visual property for drop shadow effects.
 * <p>
 * Shadow is rendered beneath the widget's background.
 *
 * @param offsetX    horizontal offset of the shadow
 * @param offsetY    vertical offset of the shadow
 * @param blurRadius the blur radius (0 = sharp edge)
 * @param color      the shadow color (ARGB format)
 */
public record ShadowProperty(float offsetX, float offsetY, float blurRadius, int color) implements VisualProperty {

    public static final StyleType<ShadowProperty> type = StyleType.of("shadow", () -> null);

    /**
     * Creates a simple shadow with default offsets.
     *
     * @param blurRadius the blur radius
     * @param color      the shadow color
     */
    public ShadowProperty(float blurRadius, int color) {
        this(2.0f, 2.0f, blurRadius, color);
    }

    /**
     * Creates a shadow with specified parameters.
     */
    public ShadowProperty(float offsetX, float offsetY, float blurRadius, int color) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.blurRadius = Math.max(0.0f, blurRadius);
        this.color = color;
    }

    /**
     * Creates a no-shadow (clear shadow).
     */
    public static ShadowProperty none() {
        return new ShadowProperty(0, 0, 0, 0);
    }

    /**
     * Creates a subtle shadow.
     */
    public static ShadowProperty subtle() {
        return new ShadowProperty(1, 1, 2, 0x40000000);
    }

    /**
     * Creates a medium shadow.
     */
    public static ShadowProperty medium() {
        return new ShadowProperty(2, 2, 4, 0x60000000);
    }

    /**
     * Creates a strong shadow.
     */
    public static ShadowProperty strong() {
        return new ShadowProperty(4, 4, 8, 0x80000000);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setShadow(offsetX, offsetY, blurRadius, color);
    }

    public boolean hasShadow() {
        return (color & 0xFF000000) != 0 && (blurRadius > 0 || offsetX != 0 || offsetY != 0);
    }

    @Override
    public String toString() {
        if (!hasShadow()) {
            return "none";
        }
        return String.format("shadow(%.1f, %.1f, %.1f, 0x%08X)", offsetX, offsetY, blurRadius, color);
    }
}
