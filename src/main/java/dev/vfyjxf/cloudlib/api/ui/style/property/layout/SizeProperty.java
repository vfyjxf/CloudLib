package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Built-in size layout property.
 * <p>
 * Size is applied to the taffy {@link TaffyStyle#size} for layout calculation.
 * <p>
 * The dimension types supported by taffy include:
 * <ul>
 *   <li>{@link TaffyDimension#AUTO} - automatic sizing</li>
 *   <li>{@link TaffyDimension#length(float)} - fixed pixel length</li>
 *   <li>{@link TaffyDimension#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 *   <li>{@link TaffyDimension#minContent()} - minimum content size</li>
 *   <li>{@link TaffyDimension#maxContent()} - maximum content size</li>
 *   <li>{@link TaffyDimension#fitContent()} - fit content size</li>
 *   <li>{@link TaffyDimension#stretch()} - stretch to fill available space</li>
 * </ul>
 *
 * @see TaffyStyle#size
 * @see TaffyDimension
 * @see UIStyles#sizeOf(float, float)
 */
public record SizeProperty(TaffyDimension width, TaffyDimension height) implements LayoutProperty {

    //region types

    public static final StyleType<TaffySize<TaffyDimension>> type = StyleType.of(
            "size",
            () -> TaffySize.of(TaffyDimension.AUTO, TaffyDimension.AUTO),
            (context, size) -> {
                context.layoutStyle().size.width = size.width;
                context.layoutStyle().size.height = size.height;
            }
    );

    //endregion

    //region constructors

    public SizeProperty(float width, float height) {
        this(width < 0 ? TaffyDimension.AUTO : TaffyDimension.length(width), height < 0 ? TaffyDimension.AUTO : TaffyDimension.length(height));
    }

    public SizeProperty(float size) {
        this(size, size);
    }

    //endregion

    //region factory - single dimension

    /**
     * Creates a size property with only the width set.
     */
    public static SizeProperty width(float value) {
        return width(TaffyDimension.length(value));
    }

    /**
     * Creates a size property with only the width set.
     */
    public static SizeProperty width(TaffyDimension value) {
        Objects.requireNonNull(value, "value");
        return new SizeProperty(value, null);
    }

    /**
     * Creates a size property with only the height set.
     */
    public static SizeProperty height(float value) {
        return height(TaffyDimension.length(value));
    }

    /**
     * Creates a size property with only the height set.
     */
    public static SizeProperty height(TaffyDimension value) {
        Objects.requireNonNull(value, "value");
        return new SizeProperty(null, value);
    }

    //endregion

    //region factory - percentage

    /**
     * Creates a size property with percentage values for width and height.
     */
    public static SizeProperty percent(float all) {
        return new SizeProperty(TaffyDimension.percent(all), TaffyDimension.percent(all));
    }

    /**
     * Creates a size property with percentage values for width and height.
     */
    public static SizeProperty percent(float width, float height) {
        return new SizeProperty(TaffyDimension.percent(width), TaffyDimension.percent(height));
    }

    /**
     * Creates a size property with only width set to percentage.
     */
    public static SizeProperty widthPercent(float percent) {
        return width(TaffyDimension.percent(percent));
    }

    /**
     * Creates a size property with only height set to percentage.
     */
    public static SizeProperty heightPercent(float percent) {
        return height(TaffyDimension.percent(percent));
    }

    //endregion

    //region factory - special values

    /**
     * Creates a size property with auto sizing for both dimensions.
     */
    public static SizeProperty auto() {
        return new SizeProperty(TaffyDimension.AUTO, TaffyDimension.AUTO);
    }

    /**
     * Creates a size property with only width set to auto.
     */
    public static SizeProperty widthAuto() {
        return width(TaffyDimension.AUTO);
    }

    /**
     * Creates a size property with only height set to auto.
     */
    public static SizeProperty heightAuto() {
        return height(TaffyDimension.AUTO);
    }

    /**
     * Creates a size property with 100% width and 100% height (full parent size).
     */
    public static SizeProperty full() {
        return percent(1.0f);
    }

    /**
     * Creates a size property with 100% width.
     */
    public static SizeProperty fullWidth() {
        return widthPercent(1.0f);
    }

    /**
     * Creates a size property with 100% height.
     */
    public static SizeProperty fullHeight() {
        return heightPercent(1.0f);
    }

    /**
     * Creates a size property with stretch for both dimensions.
     */
    public static SizeProperty stretch() {
        return new SizeProperty(TaffyDimension.stretch(), TaffyDimension.stretch());
    }

    /**
     * Creates a size property with fit-content for both dimensions.
     */
    public static SizeProperty fitContent() {
        return new SizeProperty(TaffyDimension.fitContent(), TaffyDimension.fitContent());
    }

    /**
     * Creates a size property with min-content for both dimensions.
     */
    public static SizeProperty minContent() {
        return new SizeProperty(TaffyDimension.minContent(), TaffyDimension.minContent());
    }

    /**
     * Creates a size property with max-content for both dimensions.
     */
    public static SizeProperty maxContent() {
        return new SizeProperty(TaffyDimension.maxContent(), TaffyDimension.maxContent());
    }

    //endregion

    //region LayoutProperty implementation

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void apply(StyleContext context) {
        TaffySize<TaffyDimension> current = context.get(type);

        TaffySize<TaffyDimension> merged = TaffySize.of(
                width != null ? width : current.width,
                height != null ? height : current.height
        );

        context.set(type, merged);
        context.applyGeneric(this);
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        if (width != null) {
            style.size.width = width;
        }
        if (height != null) {
            style.size.height = height;
        }
    }

    //endregion

    @Override
    public String toString() {
        if (Objects.equals(width, height)) {
            return String.valueOf(width);
        }
        return width + " x " + height;
    }
}
