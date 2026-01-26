package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property for aspect ratio.
 * <p>
 * Aspect ratio controls the relationship between width and height of an element.
 *
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
