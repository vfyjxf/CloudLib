package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for flex wrap.
 * <p>
 * Flex wrap controls whether the flex container is single-line or multi-line,
 * and the direction of the cross-axis. Maps to taffy {@link TaffyStyle#flexWrap}.
 * <p>
 * The flex wrap values supported by taffy include:
 * <ul>
 *   <li>{@link FlexWrap#NO_WRAP} - single line, no wrapping</li>
 *   <li>{@link FlexWrap#WRAP} - multi-line, wrap to next line</li>
 *   <li>{@link FlexWrap#WRAP_REVERSE} - multi-line, wrap in reverse direction</li>
 * </ul>
 *
 * @see TaffyStyle#flexWrap
 * @see FlexWrap
 * @see UIStyles#flexWrap()
 * @see UIStyles#flexNoWrap()
 */
public record FlexWrapProperty(FlexWrap wrap) implements LayoutProperty {

    public static final StyleType<FlexWrap> type = StyleType.of("flex-wrap", () -> FlexWrap.NO_WRAP);

    public FlexWrapProperty(FlexWrap wrap) {
        this.wrap = Objects.requireNonNull(wrap, "wrap");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flexWrap = wrap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexWrapProperty(FlexWrap wrap1))) return false;
        return wrap == wrap1;
    }

    @Override
    public String toString() {
        return wrap.name().toLowerCase().replace("_", "-");
    }

}
