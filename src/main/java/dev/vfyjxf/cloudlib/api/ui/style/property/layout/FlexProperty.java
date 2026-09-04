package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property that maps to taffy {@link TaffyStyle#flex}.
 * <p>
 * This is the Yoga-style flex shorthand. When set (not NaN), this overrides
 * flexGrow/flexShrink/flexBasis:
 * <ul>
 *   <li>{@code flex >= 0}: flexGrow = flex, flexShrink = 1, flexBasis = 0</li>
 *   <li>{@code flex < 0}: flexGrow = 0, flexShrink = -flex, flexBasis = 0</li>
 * </ul>
 * <p>
 * When NaN (default), the individual flexGrow/flexShrink/flexBasis values are used.
 *
 * @see TaffyStyle#flex
 * @see TaffyStyle#flexGrow
 * @see TaffyStyle#flexShrink
 * @see TaffyStyle#flexBasis
 */
public record FlexProperty(float flex) implements LayoutProperty {

    public static final StyleType<Float> type = StyleType.of("flex", () -> Float.NaN);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flex = flex;
    }

    @Override
    public String toString() {
        if (Float.isNaN(flex)) {
            return "none";
        }
        return String.valueOf(flex);
    }

}
