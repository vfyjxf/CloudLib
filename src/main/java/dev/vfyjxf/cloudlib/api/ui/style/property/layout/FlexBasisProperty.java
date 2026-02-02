package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#flexBasis}.
 * <p>
 * Flex basis defines the default size of an element before the remaining space is distributed.
 * <p>
 * The dimension types supported by taffy include:
 * <ul>
 *   <li>{@link TaffyDimension#AUTO} - automatic sizing based on content</li>
 *   <li>{@link TaffyDimension#length(float)} - fixed pixel length</li>
 *   <li>{@link TaffyDimension#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 *   <li>{@link TaffyDimension#minContent()} - minimum content size</li>
 *   <li>{@link TaffyDimension#maxContent()} - maximum content size</li>
 *   <li>{@link TaffyDimension#fitContent()} - fit content size</li>
 * </ul>
 *
 * @see TaffyStyle#flexBasis
 * @see TaffyDimension
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
