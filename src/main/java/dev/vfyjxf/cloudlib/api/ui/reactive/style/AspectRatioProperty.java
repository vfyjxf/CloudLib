package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for aspect ratio.
 * <p>
 * Aspect ratio controls the relationship between width and height of an element.
 *
 * @see Styles#aspectRatio(float)
 */
@ApiStatus.Experimental
public final class AspectRatioProperty implements LayoutProperty {

    public static final String NAME = "aspect-ratio";

    private final float ratio;

    public AspectRatioProperty(float ratio) {
        this.ratio = ratio;
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setAspectRatio(ratio);
    }

    @Override
    public String name() {
        return NAME;
    }

    public float getRatio() {
        return ratio;
    }

    @Override
    public String valueToString() {
        return String.valueOf(ratio);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AspectRatioProperty that)) return false;
        return Float.compare(ratio, that.ratio) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ratio);
    }
}
