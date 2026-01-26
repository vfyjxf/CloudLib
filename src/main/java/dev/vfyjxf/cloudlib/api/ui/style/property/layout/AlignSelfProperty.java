package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for align self.
 * <p>
 * Align self allows the default alignment (or the one specified by align-items)
 * to be overridden for individual flex items.
 *
 * @see AlignItems
 * @see UIStyles#alignSelfCenter()
 * @see UIStyles#alignSelfFlexStart()
 */
public record AlignSelfProperty(AlignItems align) implements LayoutProperty {

    public static final StyleType<AlignItems> type = StyleType.of("align-self", () -> AlignItems.FLEX_START);

    public AlignSelfProperty(AlignItems align) {
        this.align = Objects.requireNonNull(align, "align");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.alignSelf = align;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlignSelfProperty(AlignItems align1))) return false;
        return align == align1;
    }

    @Override
    public String toString() {
        return align.name().toLowerCase().replace("_", "-");
    }

}
