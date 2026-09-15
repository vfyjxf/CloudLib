package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property that maps to taffy {@link TaffyStyle#itemIsReplaced}.
 * <p>
 * When true, this element is treated as a replaced element (like an image)
 * for layout purposes. Replaced elements have intrinsic dimensions.
 *
 * @see TaffyStyle#itemIsReplaced
 */
public record ItemIsReplacedProperty(boolean value) implements LayoutProperty {

    public static final StyleType<Boolean> type = StyleType.of("item-is-replaced", () -> false);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.itemIsReplaced = value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
