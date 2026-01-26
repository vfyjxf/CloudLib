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
 *
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
