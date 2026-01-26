package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Built-in min/max size constraint layout property.
 * <p>
 * Size constraints are applied to the taffy {@link TaffyStyle} for layout calculation.
 *
 * @see UIStyles#minWidth(float)
 * @see UIStyles#maxWidth(float)
 */
public record SizeConstraintProperty(
    float minWidth, float minHeight,
    float maxWidth, float maxHeight
) implements LayoutProperty {

    public static final StyleType<SizeConstraintProperty> type = StyleType.of("size-constraint", () -> null);

    public static SizeConstraintProperty minWidth(float value) {
        return new SizeConstraintProperty(value, 0.0f, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public static SizeConstraintProperty minHeight(float value) {
        return new SizeConstraintProperty(0.0f, value, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public static SizeConstraintProperty maxWidth(float value) {
        return new SizeConstraintProperty(0.0f, 0.0f, value, Float.MAX_VALUE);
    }

    public static SizeConstraintProperty maxHeight(float value) {
        return new SizeConstraintProperty(0.0f, 0.0f, Float.MAX_VALUE, value);
    }

    public static SizeConstraintProperty minSize(float width, float height) {
        return new SizeConstraintProperty(width, height, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public static SizeConstraintProperty maxSize(float width, float height) {
        return new SizeConstraintProperty(0.0f, 0.0f, width, height);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        if (minWidth > 0) {
            style.minSize.width = TaffyDimension.length(minWidth);
        }
        if (minHeight > 0) {
            style.minSize.height = TaffyDimension.length(minHeight);
        }
        if (maxWidth < Float.MAX_VALUE) {
            style.maxSize.width = TaffyDimension.length(maxWidth);
        }
        if (maxHeight < Float.MAX_VALUE) {
            style.maxSize.height = TaffyDimension.length(maxHeight);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (minWidth > 0) sb.append("minWidth=").append(minWidth).append(" ");
        if (minHeight > 0) sb.append("minHeight=").append(minHeight).append(" ");
        if (maxWidth < Float.MAX_VALUE) sb.append("maxWidth=").append(maxWidth).append(" ");
        if (maxHeight < Float.MAX_VALUE) sb.append("maxHeight=").append(maxHeight);
        return sb.toString().trim();
    }
}
