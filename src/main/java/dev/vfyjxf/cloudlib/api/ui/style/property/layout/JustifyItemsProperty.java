package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#justifyItems}.
 */
public record JustifyItemsProperty(AlignItems justifyItems) implements LayoutProperty {

    public static final StyleType<AlignItems> type = StyleType.of("justify-items", () -> null);

    public JustifyItemsProperty(AlignItems justifyItems) {
        this.justifyItems = Objects.requireNonNull(justifyItems, "justifyItems");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.justifyItems = justifyItems;
    }

    @Override
    public String toString() {
        return justifyItems.toString();
    }
}
