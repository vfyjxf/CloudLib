package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for align items.
 * <p>
 * Align items defines the default behavior for how flex items are laid out
 * along the cross axis on the current line. Maps to taffy {@link TaffyStyle#alignItems}.
 * <p>
 * The alignment values supported by taffy include:
 * <ul>
 *   <li>{@link AlignItems#FLEX_START} - align to start of cross axis</li>
 *   <li>{@link AlignItems#FLEX_END} - align to end of cross axis</li>
 *   <li>{@link AlignItems#CENTER} - center along cross axis</li>
 *   <li>{@link AlignItems#BASELINE} - align baselines</li>
 *   <li>{@link AlignItems#STRETCH} - stretch to fill cross axis</li>
 *   <li>{@link AlignItems#START} - align to logical start</li>
 *   <li>{@link AlignItems#END} - align to logical end</li>
 * </ul>
 *
 * @see TaffyStyle#alignItems
 * @see AlignItems
 * @see UIStyles#alignItemsCenter()
 * @see UIStyles#alignItemsFlexStart()
 */
public record AlignItemsProperty(AlignItems align) implements LayoutProperty {

    public static final StyleType<AlignItems> type = StyleType.of("align-items", () -> AlignItems.FLEX_START);

    public AlignItemsProperty(AlignItems align) {
        this.align = Objects.requireNonNull(align, "align");
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.alignItems = align;
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlignItemsProperty(AlignItems align1))) return false;
        return align == align1;
    }

    @Override
    public String toString() {
        return align.name().toLowerCase().replace("_", "-");
    }

}
