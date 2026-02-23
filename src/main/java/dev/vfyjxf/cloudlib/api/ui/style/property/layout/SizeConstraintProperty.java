package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Built-in min/max size constraint layout property.
 * <p>
 * Size constraints are applied to the taffy {@link TaffyStyle#minSize} and
 * {@link TaffyStyle#maxSize} for layout calculation.
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
 * @see TaffyStyle#minSize
 * @see TaffyStyle#maxSize
 * @see TaffyDimension
 * @see UIStyles#minWidth(TaffyDimension)
 * @see UIStyles#maxWidth(TaffyDimension)
 */
public record SizeConstraintProperty(
        @Nullable TaffyDimension minWidth,
        @Nullable TaffyDimension minHeight,
        @Nullable TaffyDimension maxWidth,
        @Nullable TaffyDimension maxHeight
) implements LayoutProperty {

    public static final StyleType<SizeConstraintProperty> type = StyleType.of("size-constraint", () -> null);

    // Static factory methods with TaffyDimension

    public static SizeConstraintProperty minWidth(TaffyDimension value) {
        Objects.requireNonNull(value, "value");
        return new SizeConstraintProperty(value, null, null, null);
    }

    public static SizeConstraintProperty minHeight(TaffyDimension value) {
        Objects.requireNonNull(value, "value");
        return new SizeConstraintProperty(null, value, null, null);
    }

    public static SizeConstraintProperty maxWidth(TaffyDimension value) {
        Objects.requireNonNull(value, "value");
        return new SizeConstraintProperty(null, null, value, null);
    }

    public static SizeConstraintProperty maxHeight(TaffyDimension value) {
        Objects.requireNonNull(value, "value");
        return new SizeConstraintProperty(null, null, null, value);
    }

    public static SizeConstraintProperty minSize(TaffyDimension width, TaffyDimension height) {
        Objects.requireNonNull(width, "width");
        Objects.requireNonNull(height, "height");
        return new SizeConstraintProperty(width, height, null, null);
    }

    public static SizeConstraintProperty minSize(TaffySize<TaffyDimension> size) {
        Objects.requireNonNull(size, "size");
        return new SizeConstraintProperty(size.width, size.height, null, null);
    }

    public static SizeConstraintProperty maxSize(TaffyDimension width, TaffyDimension height) {
        Objects.requireNonNull(width, "width");
        Objects.requireNonNull(height, "height");
        return new SizeConstraintProperty(null, null, width, height);
    }

    public static SizeConstraintProperty maxSize(TaffySize<TaffyDimension> size) {
        Objects.requireNonNull(size, "size");
        return new SizeConstraintProperty(null, null, size.width, size.height);
    }

    // Convenience methods with float (pixel) values

    public static SizeConstraintProperty minWidth(float value) {
        return minWidth(TaffyDimension.length(value));
    }

    public static SizeConstraintProperty minHeight(float value) {
        return minHeight(TaffyDimension.length(value));
    }

    public static SizeConstraintProperty maxWidth(float value) {
        return maxWidth(TaffyDimension.length(value));
    }

    public static SizeConstraintProperty maxHeight(float value) {
        return maxHeight(TaffyDimension.length(value));
    }

    public static SizeConstraintProperty minSize(float width, float height) {
        return minSize(TaffyDimension.length(width), TaffyDimension.length(height));
    }

    public static SizeConstraintProperty maxSize(float width, float height) {
        return maxSize(TaffyDimension.length(width), TaffyDimension.length(height));
    }

    // Percentage convenience methods

    public static SizeConstraintProperty minWidthPercent(float percent) {
        return minWidth(TaffyDimension.percent(percent));
    }

    public static SizeConstraintProperty minHeightPercent(float percent) {
        return minHeight(TaffyDimension.percent(percent));
    }

    public static SizeConstraintProperty maxWidthPercent(float percent) {
        return maxWidth(TaffyDimension.percent(percent));
    }

    public static SizeConstraintProperty maxHeightPercent(float percent) {
        return maxHeight(TaffyDimension.percent(percent));
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        if (minWidth != null) {
            style.minSize.width = minWidth;
        }
        if (minHeight != null) {
            style.minSize.height = minHeight;
        }
        if (maxWidth != null) {
            style.maxSize.width = maxWidth;
        }
        if (maxHeight != null) {
            style.maxSize.height = maxHeight;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (minWidth != null) sb.append("minWidth=").append(minWidth).append(" ");
        if (minHeight != null) sb.append("minHeight=").append(minHeight).append(" ");
        if (maxWidth != null) sb.append("maxWidth=").append(maxWidth).append(" ");
        if (maxHeight != null) sb.append("maxHeight=").append(maxHeight);
        return sb.toString().trim();
    }
}
