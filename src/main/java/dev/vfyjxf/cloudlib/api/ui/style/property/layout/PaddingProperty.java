package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Built-in padding layout property.
 * <p>
 * Padding is applied to the taffy {@link TaffyStyle#padding} for layout calculation.
 * <p>
 * The padding types supported by taffy include:
 * <ul>
 *   <li>{@link LengthPercentage#length(float)} - fixed pixel length</li>
 *   <li>{@link LengthPercentage#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 * </ul>
 * <p>
 * Note: Unlike margin, padding does not support AUTO values.
 *
 * @see TaffyStyle#padding
 * @see LengthPercentage
 * @see UIStyles#padding(float)
 */
public record PaddingProperty(float top, float right, float bottom, float left) implements LayoutProperty {

    public static final StyleType<LengthPercentage> type = StyleType.of("padding", () -> LengthPercentage.ZERO);

    public PaddingProperty(float all) {
        this(all, all, all, all);
    }

    public PaddingProperty(float vertical, float horizontal) {
        this(vertical, horizontal, vertical, horizontal);
    }

    public PaddingProperty(float top, float right, float bottom, float left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.padding.top = LengthPercentage.length(top);
        style.padding.right = LengthPercentage.length(right);
        style.padding.bottom = LengthPercentage.length(bottom);
        style.padding.left = LengthPercentage.length(left);
    }

    @Override
    public String toString() {
        if (top == right && top == bottom && top == left) {
            return String.valueOf(top);
        }
        if (top == bottom && left == right) {
            return top + ", " + right;
        }
        return top + ", " + right + ", " + bottom + ", " + left;
    }
}
