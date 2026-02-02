package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
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
 * @see UIStyles#size(float, float)
 */
public record SizeProperty(TaffyDimension width, TaffyDimension height) implements LayoutProperty {

    public static final StyleType<SizeProperty> type = StyleType.of("size", () -> null);

    public SizeProperty(float width, float height) {
        this(width < 0 ? TaffyDimension.AUTO : TaffyDimension.length(width), height < 0 ? TaffyDimension.AUTO : TaffyDimension.length(height));
    }

    public SizeProperty(float size) {
        this(size, size);
    }

    @Override
    public StyleType<?> type() {
        return type;
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

    @Override
    public String toString() {
        if (Objects.equals(width, height)) {
            return String.valueOf(width);
        }
        return width + " x " + height;
    }
}
