package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.EdgeRect;
import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Abstract base class for edge-based layout properties (padding, margin, border, inset).
 * <p>
 * This class provides the common infrastructure for CSS-like edge property handling:
 * <ul>
 *   <li>Individual edge setting without affecting other edges</li>
 *   <li>axis-based setting (horizontal, vertical)</li>
 *   <li>All edges at once</li>
 *   <li>Edge merging for style cascading</li>
 *   <li>Inspection support with per-edge display</li>
 * </ul>
 * <p>
 * Subclasses only need to:
 * <ol>
 *   <li>Define their StyleTypes (typeAll, typeTop, etc.)</li>
 *   <li>Implement {@link #applyToStyle(TaffyStyle)} to write to the appropriate TaffyStyle field</li>
 *   <li>Implement {@link #formatValue(Object)} for inspection display</li>
 * </ol>
 *
 * @param <T> the type of value for each edge (e.g., LengthPercentage, LengthPercentageAuto)
 */
public abstract class EdgeStyleProperty<T> implements LayoutProperty {

    // region fields

    private final EdgeRect<T> edges;
    private final EdgeMask mask;
    private final StyleType<?> propertyType;

    // endregion

    // region constructor

    /**
     * Creates a new edge style property.
     *
     * @param edges        the edge values
     * @param mask         which edges this property affects
     * @param propertyType the StyleType for this property
     */
    protected EdgeStyleProperty(EdgeRect<T> edges, EdgeMask mask, StyleType<?> propertyType) {
        this.edges = Objects.requireNonNull(edges, "edges");
        this.mask = Objects.requireNonNull(mask, "mask");
        this.propertyType = Objects.requireNonNull(propertyType, "propertyType");
    }

    // endregion

    // region abstract methods

    /**
     * Gets the StyleType for the "all edges" variant.
     */
    protected abstract StyleType<?> typeAll();

    /**
     * Gets the StyleType for the top edge variant.
     */
    protected abstract StyleType<?> typeTop();

    /**
     * Gets the StyleType for the right edge variant.
     */
    protected abstract StyleType<?> typeRight();

    /**
     * Gets the StyleType for the bottom edge variant.
     */
    protected abstract StyleType<?> typeBottom();

    /**
     * Gets the StyleType for the left edge variant.
     */
    protected abstract StyleType<?> typeLeft();

    /**
     * Gets the StyleType for the horizontal axis variant.
     */
    protected abstract StyleType<?> typeHorizontal();

    /**
     * Gets the StyleType for the vertical axis variant.
     */
    protected abstract StyleType<?> typeVertical();

    /**
     * Formats a single edge value for inspection display.
     *
     * @param value the value to format
     * @return the formatted string
     */
    protected abstract String formatValue(T value);

    /**
     * Gets the base name for inspection (e.g., "padding", "margin").
     */
    protected abstract String baseName();

    // endregion

    // region getters

    /**
     * Gets the edge values.
     */
    public EdgeRect<T> edges() {
        return edges;
    }

    /**
     * Gets which edges this property affects.
     */
    public EdgeMask mask() {
        return mask;
    }

    /**
     * Gets the top edge value.
     */
    public @Nullable T top() {
        return edges.top();
    }

    /**
     * Gets the right edge value.
     */
    public @Nullable T right() {
        return edges.right();
    }

    /**
     * Gets the bottom edge value.
     */
    public @Nullable T bottom() {
        return edges.bottom();
    }

    /**
     * Gets the left edge value.
     */
    public @Nullable T left() {
        return edges.left();
    }

    // endregion

    // region StyleProperty implementation

    @Override
    public StyleType<?> type() {
        return propertyType;
    }

    @Override
    public void apply(StyleContext context) {
        context.applyLayout(this);
    }

    @Override
    public @Nullable Object inspectionValue() {
        return edges;
    }

    @Override
    public void collectInspection(InspectionInfoCollector collector) {
        String category = type().category();

        if (mask == EdgeMask.all && edges.hasAll()) {
            T top = edges.top();
            T right = edges.right();
            T bottom = edges.bottom();
            T left = edges.left();

            String formatted;
            if (Objects.equals(top, right) && Objects.equals(right, bottom) && Objects.equals(bottom, left)) {
                formatted = formatValue(top);
            } else if (Objects.equals(top, bottom) && Objects.equals(left, right)) {
                formatted = formatValue(top) + " " + formatValue(right);
            } else {
                formatted = formatValue(top) + " " + formatValue(right) + " " + formatValue(bottom) + " "
                        + formatValue(left);
            }
            collector.addFormatted(baseName(), formatted, null, category);
            return;
        }

        if (mask.affectsTop() && edges.top() != null) {
            collector.addFormatted(baseName() + "-top", formatValue(edges.top()), null, category);
        }
        if (mask.affectsRight() && edges.right() != null) {
            collector.addFormatted(baseName() + "-right", formatValue(edges.right()), null, category);
        }
        if (mask.affectsBottom() && edges.bottom() != null) {
            collector.addFormatted(baseName() + "-bottom", formatValue(edges.bottom()), null, category);
        }
        if (mask.affectsLeft() && edges.left() != null) {
            collector.addFormatted(baseName() + "-left", formatValue(edges.left()), null, category);
        }
    }

    // endregion

    // region toString

    @Override
    public String toString() {
        if (mask == EdgeMask.all) {
            return baseName() + "(" + edges.toShortString() + ")";
        }
        return baseName() + mask.suffix() + "(" + edges.format(this::formatValue) + ")";
    }

    // endregion

    // region EdgeMask enum

    /**
     * Specifies which edges a property affects.
     */
    public enum EdgeMask {
        all(true, true, true, true, ""),
        top(true, false, false, false, "Top"),
        right(false, true, false, false, "Right"),
        bottom(false, false, true, false, "Bottom"),
        left(false, false, false, true, "Left"),
        horizontal(false, true, false, true, "Horizontal"),
        vertical(true, false, true, false, "Vertical");

        private final boolean hasTop;
        private final boolean hasRight;
        private final boolean hasBottom;
        private final boolean hasLeft;
        private final String suffix;

        EdgeMask(boolean top, boolean right, boolean bottom, boolean left, String suffix) {
            this.hasTop = top;
            this.hasRight = right;
            this.hasBottom = bottom;
            this.hasLeft = left;
            this.suffix = suffix;
        }

        public boolean affectsTop() {
            return hasTop;
        }

        public boolean affectsRight() {
            return hasRight;
        }

        public boolean affectsBottom() {
            return hasBottom;
        }

        public boolean affectsLeft() {
            return hasLeft;
        }

        public boolean affects(Edge edge) {
            return switch (edge) {
                case top -> hasTop;
                case right -> hasRight;
                case bottom -> hasBottom;
                case left -> hasLeft;
            };
        }

        public String suffix() {
            return suffix;
        }

        /**
         * Gets the EdgeMask for a specific Edge.
         */
        public static EdgeMask forEdge(Edge edge) {
            return switch (edge) {
                case top -> EdgeMask.top;
                case right -> EdgeMask.right;
                case bottom -> EdgeMask.bottom;
                case left -> EdgeMask.left;
            };
        }
    }

    // endregion

    // region helper for applying with existing values

    /**
     * Helper to apply edge values to a TaffyStyle Rect, merging with existing values.
     * <p>
     * Only edges that are affected by the mask AND have non-null values will be applied.
     *
     * @param existing the existing edge values from TaffyStyle
     * @param getter   function to get the current value for an edge
     * @param setter   function to set the new value for an edge
     */
    protected void applyEdges(Function<Edge, T> getter, EdgeSetter<T> setter) {
        if (mask.affectsTop() && edges.top() != null) {
            setter.set(Edge.top, edges.top());
        }
        if (mask.affectsRight() && edges.right() != null) {
            setter.set(Edge.right, edges.right());
        }
        if (mask.affectsBottom() && edges.bottom() != null) {
            setter.set(Edge.bottom, edges.bottom());
        }
        if (mask.affectsLeft() && edges.left() != null) {
            setter.set(Edge.left, edges.left());
        }
    }

    /**
     * Functional interface for setting edge values.
     */
    @FunctionalInterface
    protected interface EdgeSetter<T> {
        void set(Edge edge, T value);
    }

    // endregion
}
