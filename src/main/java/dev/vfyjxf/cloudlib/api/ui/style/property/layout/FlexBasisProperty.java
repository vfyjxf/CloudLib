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

    //region types

    public static final StyleType<TaffyDimension> type = StyleType.of("flex-basis", () -> TaffyDimension.AUTO);

    //endregion

    //region constructors

    public FlexBasisProperty(TaffyDimension flexBasis) {
        this.flexBasis = Objects.requireNonNull(flexBasis, "flexBasis");
    }

    /**
     * Creates a flex basis property with a pixel value.
     */
    public FlexBasisProperty(float value) {
        this(TaffyDimension.length(value));
    }

    //endregion

    //region factory methods

    /**
     * Creates a flex basis property with auto value.
     */
    public static FlexBasisProperty auto() {
        return new FlexBasisProperty(TaffyDimension.AUTO);
    }

    /**
     * Creates a flex basis property with a fixed pixel length.
     */
    public static FlexBasisProperty length(float value) {
        return new FlexBasisProperty(TaffyDimension.length(value));
    }

    /**
     * Creates a flex basis property with a percentage value.
     */
    public static FlexBasisProperty percent(float percent) {
        return new FlexBasisProperty(TaffyDimension.percent(percent));
    }

    /**
     * Creates a flex basis property with min-content.
     */
    public static FlexBasisProperty minContent() {
        return new FlexBasisProperty(TaffyDimension.minContent());
    }

    /**
     * Creates a flex basis property with max-content.
     */
    public static FlexBasisProperty maxContent() {
        return new FlexBasisProperty(TaffyDimension.maxContent());
    }

    /**
     * Creates a flex basis property with fit-content.
     */
    public static FlexBasisProperty fitContent() {
        return new FlexBasisProperty(TaffyDimension.fitContent());
    }

    /**
     * Creates a flex basis property with zero value.
     */
    public static FlexBasisProperty zero() {
        return new FlexBasisProperty(TaffyDimension.length(0));
    }

    //endregion

    //region LayoutProperty implementation

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flexBasis = flexBasis;
    }

    //endregion

    @Override
    public String toString() {
        return flexBasis.toString();
    }

}
