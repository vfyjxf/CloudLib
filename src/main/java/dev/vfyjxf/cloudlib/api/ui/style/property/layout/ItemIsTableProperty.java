package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property that maps to taffy {@link TaffyStyle#itemIsTable}.
 * <p>
 * When true, this element is treated as a table for layout purposes.
 * This affects how size and content are calculated.
 *
 * @see TaffyStyle#itemIsTable
 */
public record ItemIsTableProperty(boolean value) implements LayoutProperty {

    public static final StyleType<Boolean> type = StyleType.of("item-is-table", () -> false);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.itemIsTable = value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
