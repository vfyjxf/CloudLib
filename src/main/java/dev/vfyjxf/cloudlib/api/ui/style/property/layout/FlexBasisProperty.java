package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#flexBasis}.
 */
public record FlexBasisProperty(TaffyDimension flexBasis) implements LayoutProperty {

    public static final StyleType<TaffyDimension> type = StyleType.of("flex-basis", () -> TaffyDimension.AUTO);

    public FlexBasisProperty(TaffyDimension flexBasis) {
        this.flexBasis = Objects.requireNonNull(flexBasis, "flexBasis");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flexBasis = flexBasis;
    }

    @Override
    public String toString() {
        return flexBasis.toString();
    }

}
