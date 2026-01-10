package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for absolute positioning.
 * <p>
 * Position edges control the offset from the parent's edge when using
 * absolute positioning.
 *
 * @see YogaEdge
 * @see Styles#top(float)
 * @see Styles#left(float)
 * @see Styles#right(float)
 * @see Styles#bottom(float)
 */
@ApiStatus.Experimental
public final class PositionEdgeProperty implements LayoutProperty {

    private final YogaEdge edge;
    private final StyleLength length;

    public PositionEdgeProperty(YogaEdge edge, float value) {
        this(edge, StyleLength.points(value));
    }

    public PositionEdgeProperty(YogaEdge edge, StyleLength length) {
        this.edge = Objects.requireNonNull(edge, "edge");
        this.length = Objects.requireNonNull(length, "length");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setPosition(edge, length);
    }

    @Override
    public String name() {
        return edge.name().toLowerCase();
    }

    public YogaEdge getEdge() {
        return edge;
    }

    public StyleLength getLength() {
        return length;
    }

    @Override
    public String valueToString() {
        return length.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionEdgeProperty that)) return false;
        return edge == that.edge && Objects.equals(length, that.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edge, length);
    }
}
