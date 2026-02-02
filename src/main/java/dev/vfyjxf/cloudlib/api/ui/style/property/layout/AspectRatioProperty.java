package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property for aspect ratio.
 * <p>
 * Aspect ratio controls the relationship between width and height of an element.
 * Maps to taffy {@link TaffyStyle#aspectRatio}.
 * <p>
 * The ratio value represents width/height. For example:
 * <ul>
 *   <li>1.0 - square (1:1 ratio)</li>
 *   <li>1.5 - 3:2 ratio (width is 1.5x height)</li>
 *   <li>0.5 - 1:2 ratio (width is 0.5x height)</li>
 *   <li>NaN - no aspect ratio constraint</li>
 * </ul>
 *
 * @see TaffyStyle#aspectRatio
 * @see UIStyles#aspectRatio(float)
 */
public record AspectRatioProperty(float ratio) implements LayoutProperty {

    public static final StyleType<Float> type = StyleType.of("aspect-ratio", () -> 1.0f);

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.aspectRatio = ratio;
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AspectRatioProperty(float ratio1))) return false;
        return Float.compare(ratio, ratio1) == 0;
    }

    @Override
    public String toString() {
        return String.valueOf(ratio);
    }

}
