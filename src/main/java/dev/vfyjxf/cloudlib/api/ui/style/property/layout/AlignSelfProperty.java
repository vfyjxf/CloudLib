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
 * to be overridden for individual flex items. Maps to taffy {@link TaffyStyle#alignSelf}.
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
 * @see TaffyStyle#alignSelf
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
