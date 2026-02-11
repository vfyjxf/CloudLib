package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.EdgeRect;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Built-in border layout property.
 * <p>
 * Border is applied to the taffy {@link TaffyStyle#border} for layout calculation.
 * This property only affects the box model layout - for visual border styling (color, etc.),
 * use separate visual properties.
 * <p>
 * This property supports CSS-like individual edge setting. Each edge has its own
 * StyleType, so setting one edge does not affect others:
 * <pre>{@code
 * // Both top and bottom will be set independently
 * UIStyle.of(borderTop(1), borderBottom(1))
 * }</pre>
 * <p>
 * The border types supported by taffy include:
 * <ul>
 *   <li>{@link LengthPercentage#length(float)} - fixed pixel length</li>
 *   <li>{@link LengthPercentage#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 * </ul>
 * <p>
 * Note: Unlike margin, border does not support AUTO values.
 *
 * @see TaffyStyle#border
 * @see LengthPercentage
 * @see UIStyles#border(float)
 */
public final class BorderProperty extends EdgeStyleProperty<LengthPercentage> {

    //region types

    /**
     * StyleType for setting all four border edges at once.
     */
    public static final StyleType<BorderProperty> TYPE_ALL = StyleType.of("border", () -> null);

    /**
     * StyleType for setting only the top border edge.
     */
    public static final StyleType<BorderProperty> TYPE_TOP = StyleType.of("border-top", () -> null);

    /**
     * StyleType for setting only the right border edge.
     */
    public static final StyleType<BorderProperty> TYPE_RIGHT = StyleType.of("border-right", () -> null);

    /**
     * StyleType for setting only the bottom border edge.
     */
    public static final StyleType<BorderProperty> TYPE_BOTTOM = StyleType.of("border-bottom", () -> null);

    /**
     * StyleType for setting only the left border edge.
     */
    public static final StyleType<BorderProperty> TYPE_LEFT = StyleType.of("border-left", () -> null);

    /**
     * StyleType for setting horizontal border edges (left and right).
     */
    public static final StyleType<BorderProperty> TYPE_HORIZONTAL = StyleType.of("border-horizontal", () -> null);

    /**
     * StyleType for setting vertical border edges (top and bottom).
     */
    public static final StyleType<BorderProperty> TYPE_VERTICAL = StyleType.of("border-vertical", () -> null);

    /**
     * Legacy type alias for backward compatibility.
     */
    public static final StyleType<BorderProperty> type = TYPE_ALL;

    //endregion

    //region constructors

    private BorderProperty(EdgeRect<LengthPercentage> edges, EdgeMask mask, StyleType<?> type) {
        super(edges, mask, type);
    }

    /**
     * Creates a border property with equal border on all sides.
     */
    public BorderProperty(LengthPercentage all) {
        this(EdgeRect.all(all), EdgeMask.ALL, TYPE_ALL);
    }

    /**
     * Creates a border property with vertical and horizontal values.
     */
    public BorderProperty(LengthPercentage vertical, LengthPercentage horizontal) {
        this(EdgeRect.symmetric(vertical, horizontal), EdgeMask.ALL, TYPE_ALL);
    }

    /**
     * Creates a border property with equal pixel border on all sides.
     */
    public BorderProperty(float all) {
        this(LengthPercentage.length(all));
    }

    /**
     * Creates a border property with vertical and horizontal pixel values.
     */
    public BorderProperty(float vertical, float horizontal) {
        this(LengthPercentage.length(vertical), LengthPercentage.length(horizontal));
    }

    /**
     * Creates a border property with individual pixel values for each side.
     */
    public BorderProperty(float top, float right, float bottom, float left) {
        this(
            EdgeRect.of(
                LengthPercentage.length(top),
                LengthPercentage.length(right),
                LengthPercentage.length(bottom),
                LengthPercentage.length(left)
            ),
            EdgeMask.ALL,
            TYPE_ALL
        );
    }

    //endregion

    //region factory - all edges

    /**
     * Creates a border property with individual LengthPercentage values for each side.
     *
     * @param top    the top border
     * @param right  the right border
     * @param bottom the bottom border
     * @param left   the left border
     * @return a border property affecting all edges
     */
    public static BorderProperty of(LengthPercentage top, LengthPercentage right, LengthPercentage bottom, LengthPercentage left) {
        return new BorderProperty(EdgeRect.of(top, right, bottom, left), EdgeMask.ALL, TYPE_ALL);
    }

    //endregion

    //region factory - single edge

    /**
     * Creates a border property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty top(float value) {
        return top(LengthPercentage.length(value));
    }

    /**
     * Creates a border property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty top(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new BorderProperty(EdgeRect.top(value), EdgeMask.TOP, TYPE_TOP);
    }

    /**
     * Creates a border property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty right(float value) {
        return right(LengthPercentage.length(value));
    }

    /**
     * Creates a border property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty right(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new BorderProperty(EdgeRect.right(value), EdgeMask.RIGHT, TYPE_RIGHT);
    }

    /**
     * Creates a border property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty bottom(float value) {
        return bottom(LengthPercentage.length(value));
    }

    /**
     * Creates a border property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty bottom(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new BorderProperty(EdgeRect.bottom(value), EdgeMask.BOTTOM, TYPE_BOTTOM);
    }

    /**
     * Creates a border property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty left(float value) {
        return left(LengthPercentage.length(value));
    }

    /**
     * Creates a border property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty left(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new BorderProperty(EdgeRect.left(value), EdgeMask.LEFT, TYPE_LEFT);
    }

    /**
     * Creates a border property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty edge(Edge edge, float value) {
        return edge(edge, LengthPercentage.length(value));
    }

    /**
     * Creates a border property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static BorderProperty edge(Edge edge, LengthPercentage value) {
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

    //region factory - horizontal/vertical

    /**
     * Creates a border property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static BorderProperty horizontal(float value) {
        return horizontal(LengthPercentage.length(value));
    }

    /**
     * Creates a border property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static BorderProperty horizontal(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new BorderProperty(EdgeRect.horizontal(value), EdgeMask.HORIZONTAL, TYPE_HORIZONTAL);
    }

    /**
     * Creates a border property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static BorderProperty vertical(float value) {
        return vertical(LengthPercentage.length(value));
    }

    /**
     * Creates a border property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static BorderProperty vertical(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new BorderProperty(EdgeRect.vertical(value), EdgeMask.VERTICAL, TYPE_VERTICAL);
    }

    //endregion

    //region factory - percentage

    /**
     * Creates a border with percentage values for all sides.
     */
    public static BorderProperty percent(float all) {
        return new BorderProperty(LengthPercentage.percent(all));
    }

    /**
     * Creates a border with percentage values for vertical and horizontal.
     */
    public static BorderProperty percent(float vertical, float horizontal) {
        return new BorderProperty(LengthPercentage.percent(vertical), LengthPercentage.percent(horizontal));
    }

    /**
     * Creates a border with percentage values for each side.
     */
    public static BorderProperty percent(float top, float right, float bottom, float left) {
        return new BorderProperty(
            EdgeRect.of(
                LengthPercentage.percent(top),
                LengthPercentage.percent(right),
                LengthPercentage.percent(bottom),
                LengthPercentage.percent(left)
            ),
            EdgeMask.ALL,
            TYPE_ALL
        );
    }

    //endregion

    //region factory - common patterns

    /**
     * Creates a border with zero on all sides.
     */
    public static BorderProperty none() {
        return new BorderProperty(LengthPercentage.ZERO);
    }

    //endregion

    //region EdgeStyleProperty implementation

    @Override
    protected StyleType<?> typeAll() { return TYPE_ALL; }

    @Override
    protected StyleType<?> typeTop() { return TYPE_TOP; }

    @Override
    protected StyleType<?> typeRight() { return TYPE_RIGHT; }

    @Override
    protected StyleType<?> typeBottom() { return TYPE_BOTTOM; }

    @Override
    protected StyleType<?> typeLeft() { return TYPE_LEFT; }

    @Override
    protected StyleType<?> typeHorizontal() { return TYPE_HORIZONTAL; }

    @Override
    protected StyleType<?> typeVertical() { return TYPE_VERTICAL; }

    @Override
    protected String formatValue(LengthPercentage value) {
        return TaffyStyleUtil.formatLengthPercentage(value);
    }

    @Override
    protected String baseName() {
        return "border";
    }

    //endregion

    //region LayoutProperty implementation

    @Override
    public void applyToStyle(TaffyStyle style) {
        applyEdges(
            edge -> switch (edge) {
                case TOP -> style.border.top;
                case RIGHT -> style.border.right;
                case BOTTOM -> style.border.bottom;
                case LEFT -> style.border.left;
            },
            (edge, value) -> {
                switch (edge) {
                    case TOP -> style.border.top = value;
                    case RIGHT -> style.border.right = value;
                    case BOTTOM -> style.border.bottom = value;
                    case LEFT -> style.border.left = value;
                }
            }
        );
    }

    //endregion
}
