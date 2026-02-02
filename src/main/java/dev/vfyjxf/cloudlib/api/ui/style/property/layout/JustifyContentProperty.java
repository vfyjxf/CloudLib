package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for justify content.
 * <p>
 * Justify content defines the alignment along the main axis.
 * It helps distribute extra free space leftover when either all the flex items
 * on a line are inflexible, or are flexible but have reached their maximum size.
 * Maps to taffy {@link TaffyStyle#justifyContent}.
 * <p>
 * The justification values supported by taffy include:
 * <ul>
 *   <li>{@link JustifyContent#FLEX_START} - pack items to start of main axis</li>
 *   <li>{@link JustifyContent#FLEX_END} - pack items to end of main axis</li>
 *   <li>{@link JustifyContent#CENTER} - center items along main axis</li>
 *   <li>{@link JustifyContent#SPACE_BETWEEN} - distribute items with space between</li>
 *   <li>{@link JustifyContent#SPACE_AROUND} - distribute items with space around</li>
 *   <li>{@link JustifyContent#SPACE_EVENLY} - distribute items with even space</li>
 *   <li>{@link JustifyContent#START} - pack items to logical start</li>
 *   <li>{@link JustifyContent#END} - pack items to logical end</li>
 *   <li>{@link JustifyContent#STRETCH} - stretch items to fill main axis</li>
 * </ul>
 *
 * @see TaffyStyle#justifyContent
 * @see JustifyContent
 * @see UIStyles#justifyCenter()
 * @see UIStyles#justifySpaceBetween()
 */
public record JustifyContentProperty(JustifyContent justify) implements LayoutProperty {


    public static final StyleType<JustifyContent> type = StyleType.of("justify-content", () -> JustifyContent.FLEX_START);

    public JustifyContentProperty(JustifyContent justify) {
        this.justify = Objects.requireNonNull(justify, "justify");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.justifyContent = TaffyStyleUtil.toAlignContent(justify);
    }

    public JustifyContent getJustify() {
        return justify;
    }

    @Override
    public String toString() {
        return justify.name().toLowerCase().replace("_", "-");
    }
}
