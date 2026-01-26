package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#justifySelf}.
 */
public record JustifySelfProperty(AlignItems justifySelf) implements LayoutProperty {

    public static final StyleType<AlignItems> type = StyleType.of("justify-self", () -> null);

    public JustifySelfProperty(AlignItems justifySelf) {
        this.justifySelf = Objects.requireNonNull(justifySelf, "justifySelf");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.justifySelf = justifySelf;
    }

    @Override
    public String toString() {
        return justifySelf.toString();
    }
}
