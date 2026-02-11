package dev.vfyjxf.cloudlib.api.ui.style;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * A generic container for four-edge values (top, right, bottom, left).
 * <p>
 * This class provides a unified way to handle CSS-like edge properties such as
 * padding, margin, border, and inset. It supports:
 * <ul>
 *   <li>All edges at once: {@link #all(Object)}</li>
 *   <li>Vertical/horizontal pairs: {@link #symmetric(Object, Object)}</li>
 *   <li>Individual edges: {@link #top(Object)}, {@link #right(Object)}, etc.</li>
 *   <li>Partial edge specification with null for unset edges</li>
 * </ul>
 * <p>
 * EdgeRect values can be merged, where non-null values override null values:
 * <pre>{@code
 * EdgeRect<Integer> a = EdgeRect.top(10);  // top=10, others=null
 * EdgeRect<Integer> b = EdgeRect.left(5);  // left=5, others=null
 * EdgeRect<Integer> merged = a.merge(b);   // top=10, left=5, others=null
 * }</pre>
 *
 * @param <T> the type of value stored in each edge
 */
public record EdgeRect<T>(
    @Nullable T top,
    @Nullable T right,
    @Nullable T bottom,
    @Nullable T left
) {

    //region constants

    /**
     * An empty EdgeRect with all edges set to null.
     */
    @SuppressWarnings("rawtypes")
    private static final EdgeRect EMPTY = new EdgeRect<>(null, null, null, null);

    /**
     * Returns an empty EdgeRect with all edges set to null.
     *
     * @param <T> the value type
     * @return an empty EdgeRect
     */
    @SuppressWarnings("unchecked")
    public static <T> EdgeRect<T> empty() {
        return (EdgeRect<T>) EMPTY;
    }

    //endregion

    //region factory - all edges

    /**
     * Creates an EdgeRect with the same value on all four edges.
     *
     * @param value the value for all edges
     * @param <T>   the value type
     * @return a new EdgeRect
     */
    public static <T> EdgeRect<T> all(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(value, value, value, value);
    }

    /**
     * Creates an EdgeRect with vertical (top/bottom) and horizontal (left/right) pairs.
     *
     * @param vertical   the value for top and bottom
     * @param horizontal the value for left and right
     * @param <T>        the value type
     * @return a new EdgeRect
     */
    public static <T> EdgeRect<T> symmetric(T vertical, T horizontal) {
        Objects.requireNonNull(vertical, "vertical");
        Objects.requireNonNull(horizontal, "horizontal");
        return new EdgeRect<>(vertical, horizontal, vertical, horizontal);
    }

    /**
     * Creates an EdgeRect with all four edges specified individually.
     *
     * @param top    the top value
     * @param right  the right value
     * @param bottom the bottom value
     * @param left   the left value
     * @param <T>    the value type
     * @return a new EdgeRect
     */
    public static <T> EdgeRect<T> of(T top, T right, T bottom, T left) {
        return new EdgeRect<>(top, right, bottom, left);
    }

    //endregion

    //region factory - single edge

    /**
     * Creates an EdgeRect with only the top edge set.
     *
     * @param value the top value
     * @param <T>   the value type
     * @return a new EdgeRect with only top set
     */
    public static <T> EdgeRect<T> top(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(value, null, null, null);
    }

    /**
     * Creates an EdgeRect with only the right edge set.
     *
     * @param value the right value
     * @param <T>   the value type
     * @return a new EdgeRect with only right set
     */
    public static <T> EdgeRect<T> right(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(null, value, null, null);
    }

    /**
     * Creates an EdgeRect with only the bottom edge set.
     *
     * @param value the bottom value
     * @param <T>   the value type
     * @return a new EdgeRect with only bottom set
     */
    public static <T> EdgeRect<T> bottom(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(null, null, value, null);
    }

    /**
     * Creates an EdgeRect with only the left edge set.
     *
     * @param value the left value
     * @param <T>   the value type
     * @return a new EdgeRect with only left set
     */
    public static <T> EdgeRect<T> left(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(null, null, null, value);
    }

    /**
     * Creates an EdgeRect with only the specified edge set.
     *
     * @param edge  the edge to set
     * @param value the value
     * @param <T>   the value type
     * @return a new EdgeRect with only the specified edge set
     */
    public static <T> EdgeRect<T> edge(Edge edge, T value) {
        Objects.requireNonNull(edge, "edge");
        Objects.requireNonNull(value, "value");
        return switch (edge) {
            case TOP -> top(value);
            case RIGHT -> right(value);
            case BOTTOM -> bottom(value);
            case LEFT -> left(value);
        };
    }

    //endregion

    //region factory - axis pairs

    /**
     * Creates an EdgeRect with only horizontal edges (left and right) set.
     *
     * @param value the value for left and right
     * @param <T>   the value type
     * @return a new EdgeRect with only horizontal edges set
     */
    public static <T> EdgeRect<T> horizontal(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(null, value, null, value);
    }

    /**
     * Creates an EdgeRect with only vertical edges (top and bottom) set.
     *
     * @param value the value for top and bottom
     * @param <T>   the value type
     * @return a new EdgeRect with only vertical edges set
     */
    public static <T> EdgeRect<T> vertical(T value) {
        Objects.requireNonNull(value, "value");
        return new EdgeRect<>(value, null, value, null);
    }

    //endregion

    //region accessors

    /**
     * Gets the value for a specific edge.
     *
     * @param edge the edge
     * @return the value, or null if not set
     */
    public @Nullable T get(Edge edge) {
        Objects.requireNonNull(edge, "edge");
        return switch (edge) {
            case TOP -> top;
            case RIGHT -> right;
            case BOTTOM -> bottom;
            case LEFT -> left;
        };
    }

    /**
     * Checks if any edge has a non-null value.
     *
     * @return true if any edge is set
     */
    public boolean hasAny() {
        return top != null || right != null || bottom != null || left != null;
    }

    /**
     * Checks if all edges have non-null values.
     *
     * @return true if all edges are set
     */
    public boolean hasAll() {
        return top != null && right != null && bottom != null && left != null;
    }

    /**
     * Checks if a specific edge has a non-null value.
     *
     * @param edge the edge to check
     * @return true if the edge is set
     */
    public boolean has(Edge edge) {
        return get(edge) != null;
    }

    //endregion

    //region transformation

    /**
     * Creates a new EdgeRect with a specific edge replaced.
     *
     * @param edge  the edge to replace
     * @param value the new value
     * @return a new EdgeRect with the edge replaced
     */
    public EdgeRect<T> with(Edge edge, @Nullable T value) {
        Objects.requireNonNull(edge, "edge");
        return switch (edge) {
            case TOP -> new EdgeRect<>(value, right, bottom, left);
            case RIGHT -> new EdgeRect<>(top, value, bottom, left);
            case BOTTOM -> new EdgeRect<>(top, right, value, left);
            case LEFT -> new EdgeRect<>(top, right, bottom, value);
        };
    }

    /**
     * Creates a new EdgeRect with the top edge replaced.
     *
     * @param value the new top value
     * @return a new EdgeRect
     */
    public EdgeRect<T> withTop(@Nullable T value) {
        return new EdgeRect<>(value, right, bottom, left);
    }

    /**
     * Creates a new EdgeRect with the right edge replaced.
     *
     * @param value the new right value
     * @return a new EdgeRect
     */
    public EdgeRect<T> withRight(@Nullable T value) {
        return new EdgeRect<>(top, value, bottom, left);
    }

    /**
     * Creates a new EdgeRect with the bottom edge replaced.
     *
     * @param value the new bottom value
     * @return a new EdgeRect
     */
    public EdgeRect<T> withBottom(@Nullable T value) {
        return new EdgeRect<>(top, right, value, left);
    }

    /**
     * Creates a new EdgeRect with the left edge replaced.
     *
     * @param value the new left value
     * @return a new EdgeRect
     */
    public EdgeRect<T> withLeft(@Nullable T value) {
        return new EdgeRect<>(top, right, bottom, value);
    }

    /**
     * Merges this EdgeRect with another, where non-null values from other override null values in this.
     * <p>
     * This allows CSS-like cascading where later properties override earlier ones:
     * <pre>{@code
     * EdgeRect<Integer> base = EdgeRect.all(10);      // all edges = 10
     * EdgeRect<Integer> override = EdgeRect.top(20); // only top = 20
     * EdgeRect<Integer> result = base.merge(override); // top=20, others=10
     * }</pre>
     *
     * @param other the EdgeRect to merge with
     * @return a new merged EdgeRect
     */
    public EdgeRect<T> merge(EdgeRect<T> other) {
        if (other == null) {
            return this;
        }
        return new EdgeRect<>(
            other.top != null ? other.top : this.top,
            other.right != null ? other.right : this.right,
            other.bottom != null ? other.bottom : this.bottom,
            other.left != null ? other.left : this.left
        );
    }

    /**
     * Maps the values of this EdgeRect to a new type.
     *
     * @param mapper the mapping function
     * @param <R>    the result type
     * @return a new EdgeRect with mapped values
     */
    public <R> EdgeRect<R> map(Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new EdgeRect<>(
            top != null ? mapper.apply(top) : null,
            right != null ? mapper.apply(right) : null,
            bottom != null ? mapper.apply(bottom) : null,
            left != null ? mapper.apply(left) : null
        );
    }

    //endregion

    //region formatting

    /**
     * Formats this EdgeRect as a CSS-like string.
     *
     * @return a formatted string representation
     */
    public String toShortString() {
        if (!hasAny()) {
            return "none";
        }
        if (hasAll() && Objects.equals(top, right) && Objects.equals(right, bottom) && Objects.equals(bottom, left)) {
            return String.valueOf(top);
        }
        if (hasAll() && Objects.equals(top, bottom) && Objects.equals(left, right)) {
            return top + " " + right;
        }
        StringBuilder sb = new StringBuilder();
        if (top != null) sb.append("top=").append(top);
        if (right != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("right=").append(right);
        }
        if (bottom != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("bottom=").append(bottom);
        }
        if (left != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("left=").append(left);
        }
        return sb.toString();
    }

    /**
     * Formats this EdgeRect with a custom value formatter.
     *
     * @param formatter the value formatter
     * @return a formatted string representation
     */
    public String format(Function<T, String> formatter) {
        Objects.requireNonNull(formatter, "formatter");
        if (!hasAny()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        if (top != null) sb.append("top=").append(formatter.apply(top));
        if (right != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("right=").append(formatter.apply(right));
        }
        if (bottom != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("bottom=").append(formatter.apply(bottom));
        }
        if (left != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("left=").append(formatter.apply(left));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EdgeRect[" + toShortString() + "]";
    }

    //endregion
}
