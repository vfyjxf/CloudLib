package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for align content.
 * <p>
 * Align content aligns a flex container's lines within when there is extra
 * space in the cross-axis. Maps to taffy {@link TaffyStyle#alignContent}.
 * <p>
 * The alignment values supported by taffy include:
 * <ul>
 *   <li>{@link AlignContent#FLEX_START} - pack lines to start of cross axis</li>
 *   <li>{@link AlignContent#FLEX_END} - pack lines to end of cross axis</li>
 *   <li>{@link AlignContent#CENTER} - center lines along cross axis</li>
 *   <li>{@link AlignContent#STRETCH} - stretch lines to fill cross axis</li>
 *   <li>{@link AlignContent#SPACE_BETWEEN} - distribute lines with space between</li>
 *   <li>{@link AlignContent#SPACE_AROUND} - distribute lines with space around</li>
 *   <li>{@link AlignContent#SPACE_EVENLY} - distribute lines with even space</li>
 *   <li>{@link AlignContent#START} - pack lines to logical start</li>
 *   <li>{@link AlignContent#END} - pack lines to logical end</li>
 * </ul>
 *
 * @see TaffyStyle#alignContent
 * @see AlignContent
 * @see UIStyles#alignContentCenter()
 * @see UIStyles#alignContentSpaceBetween()
 */
public record AlignContentProperty(AlignContent align) implements LayoutProperty {

    public static final StyleType<AlignContent> type = StyleType.of("align-content", () -> AlignContent.FLEX_START);

    public AlignContentProperty(AlignContent align) {
        this.align = Objects.requireNonNull(align, "align");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.alignContent = align;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlignContentProperty(AlignContent align1))) return false;
        return align == align1;
    }

    @Override
    public String toString() {
        return align.name().toLowerCase().replace("_", "-");
    }

}
