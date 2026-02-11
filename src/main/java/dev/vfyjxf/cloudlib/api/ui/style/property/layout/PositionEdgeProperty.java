package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for absolute positioning.
 * <p>
 * Position edges control the offset from the parent's edge when using
 * absolute positioning. Maps to taffy {@link TaffyStyle#inset}.
 * <p>
 * The inset types supported by taffy include:
 * <ul>
 *   <li>{@link LengthPercentageAuto#AUTO} - automatic positioning</li>
 *   <li>{@link LengthPercentageAuto#length(float)} - fixed pixel offset</li>
 *   <li>{@link LengthPercentageAuto#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 * </ul>
 *
 * @see TaffyStyle#inset
 * @see LengthPercentageAuto
 * @see Edge
 * @see UIStyles#top(float)
 * @see UIStyles#left(float)
 * @see UIStyles#right(float)
 * @see UIStyles#bottom(float)
 */
public record PositionEdgeProperty(Edge edge, LengthPercentageAuto length) implements LayoutProperty {

    public static final StyleType<LengthPercentageAuto> top = StyleType.of("inset-top", () -> LengthPercentageAuto.AUTO);
    public static final StyleType<LengthPercentageAuto> right = StyleType.of("inset-right", () -> LengthPercentageAuto.AUTO);
    public static final StyleType<LengthPercentageAuto> bottom = StyleType.of("inset-bottom", () -> LengthPercentageAuto.AUTO);
    public static final StyleType<LengthPercentageAuto> left = StyleType.of("inset-left", () -> LengthPercentageAuto.AUTO);

    public PositionEdgeProperty(Edge edge, LengthPercentageAuto length) {
        this.edge = Objects.requireNonNull(edge, "edge");
        this.length = Objects.requireNonNull(length, "length");
    }

    public PositionEdgeProperty(Edge edge, float value) {
        this(edge, LengthPercentageAuto.length(value));
    }

    // ========== Static factory methods for top edge ==========

    /**
     * Creates a position property for the top edge with a pixel value.
     */
    public static PositionEdgeProperty top(float value) {
        return new PositionEdgeProperty(Edge.TOP, value);
    }

    /**
     * Creates a position property for the top edge.
     */
    public static PositionEdgeProperty top(LengthPercentageAuto value) {
        return new PositionEdgeProperty(Edge.TOP, value);
    }

    /**
     * Creates a position property for the top edge with a percentage value.
     */
    public static PositionEdgeProperty topPercent(float percent) {
        return new PositionEdgeProperty(Edge.TOP, LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates a position property for the top edge with auto value.
     */
    public static PositionEdgeProperty topAuto() {
        return new PositionEdgeProperty(Edge.TOP, LengthPercentageAuto.AUTO);
    }

    // ========== Static factory methods for right edge ==========

    /**
     * Creates a position property for the right edge with a pixel value.
     */
    public static PositionEdgeProperty right(float value) {
        return new PositionEdgeProperty(Edge.RIGHT, value);
    }

    /**
     * Creates a position property for the right edge.
     */
    public static PositionEdgeProperty right(LengthPercentageAuto value) {
        return new PositionEdgeProperty(Edge.RIGHT, value);
    }

    /**
     * Creates a position property for the right edge with a percentage value.
     */
    public static PositionEdgeProperty rightPercent(float percent) {
        return new PositionEdgeProperty(Edge.RIGHT, LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates a position property for the right edge with auto value.
     */
    public static PositionEdgeProperty rightAuto() {
        return new PositionEdgeProperty(Edge.RIGHT, LengthPercentageAuto.AUTO);
    }

    // ========== Static factory methods for bottom edge ==========

    /**
     * Creates a position property for the bottom edge with a pixel value.
     */
    public static PositionEdgeProperty bottom(float value) {
        return new PositionEdgeProperty(Edge.BOTTOM, value);
    }

    /**
     * Creates a position property for the bottom edge.
     */
    public static PositionEdgeProperty bottom(LengthPercentageAuto value) {
        return new PositionEdgeProperty(Edge.BOTTOM, value);
    }

    /**
     * Creates a position property for the bottom edge with a percentage value.
     */
    public static PositionEdgeProperty bottomPercent(float percent) {
        return new PositionEdgeProperty(Edge.BOTTOM, LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates a position property for the bottom edge with auto value.
     */
    public static PositionEdgeProperty bottomAuto() {
        return new PositionEdgeProperty(Edge.BOTTOM, LengthPercentageAuto.AUTO);
    }

    // ========== Static factory methods for left edge ==========

    /**
     * Creates a position property for the left edge with a pixel value.
     */
    public static PositionEdgeProperty left(float value) {
        return new PositionEdgeProperty(Edge.LEFT, value);
    }

    /**
     * Creates a position property for the left edge.
     */
    public static PositionEdgeProperty left(LengthPercentageAuto value) {
        return new PositionEdgeProperty(Edge.LEFT, value);
    }

    /**
     * Creates a position property for the left edge with a percentage value.
     */
    public static PositionEdgeProperty leftPercent(float percent) {
        return new PositionEdgeProperty(Edge.LEFT, LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates a position property for the left edge with auto value.
     */
    public static PositionEdgeProperty leftAuto() {
        return new PositionEdgeProperty(Edge.LEFT, LengthPercentageAuto.AUTO);
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        TaffyStyleUtil.setRectEdge(style.inset, edge, length);
    }

    public LengthPercentageAuto getLength() {
        return length;
    }

    @Override
    public StyleType<?> type() {
        return switch (edge) {
            case TOP -> top;
            case RIGHT -> right;
            case BOTTOM -> bottom;
            case LEFT -> left;
        };
    }

    @Override
    public String toString() {
        return length.toString();
    }
}
