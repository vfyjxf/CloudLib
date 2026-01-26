package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Built-in margin layout property.
 * <p>
 * Margin is applied to the taffy {@link TaffyStyle} for layout calculation.
 *
 * @see UIStyles#margin(float)
 */
public record MarginProperty(float top, float right, float bottom, float left) implements LayoutProperty {

    public static final String NAME = "margin";

    public static final StyleType<LengthPercentageAuto> type = StyleType.of("margin", () -> LengthPercentageAuto.ZERO);

    public MarginProperty(float all) {
        this(all, all, all, all);
    }

    public MarginProperty(float vertical, float horizontal) {
        this(vertical, horizontal, vertical, horizontal);
    }

    public MarginProperty(float top, float right, float bottom, float left) {
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
        style.margin.top = LengthPercentageAuto.length(top);
        style.margin.right = LengthPercentageAuto.length(right);
        style.margin.bottom = LengthPercentageAuto.length(bottom);
        style.margin.left = LengthPercentageAuto.length(left);
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
