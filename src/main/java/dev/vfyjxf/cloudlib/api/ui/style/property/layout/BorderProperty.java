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
 * Note: Unlike margin, border does not support auto values.
 *
 * @see TaffyStyle#border
 * @see LengthPercentage
 * @see UIStyles#border(float)
 */
public final class BorderProperty extends EdgeStyleProperty<LengthPercentage> {

    // region types

    /**
     * StyleType for setting all four border edges at once.
     */
    public static final StyleType<BorderProperty> typeAll = StyleType.of("border", () -> null);

    /**
     * StyleType for setting only the top border edge.
     */
    public static final StyleType<BorderProperty> typeTop = StyleType.of("border-top", () -> null);

    /**
     * StyleType for setting only the right border edge.
     */
    public static final StyleType<BorderProperty> typeRight = StyleType.of("border-right", () -> null);

    /**
     * StyleType for setting only the bottom border edge.
     */
    public static final StyleType<BorderProperty> typeBottom = StyleType.of("border-bottom", () -> null);

    /**
     * StyleType for setting only the left border edge.
     */
    public static final StyleType<BorderProperty> typeLeft = StyleType.of("border-left", () -> null);

    /**
     * StyleType for setting horizontal border edges (left and right).
     */
    public static final StyleType<BorderProperty> typeHorizontal = StyleType.of("border-horizontal", () -> null);

    /**
     * StyleType for setting vertical border edges (top and bottom).
     */
    public static final StyleType<BorderProperty> typeVertical = StyleType.of("border-vertical", () -> null);

    /**
     * Legacy type alias for backward compatibility.
     */
    public static final StyleType<BorderProperty> type = typeAll;

    // endregion

    // region constructors

    private BorderProperty(EdgeRect<LengthPercentage> edges, EdgeMask mask, StyleType<?> type) {
        super(edges, mask, type);
    }

    /**
     * Creates a border property with equal border on all sides.
     */
    public BorderProperty(LengthPercentage all) {
        this(EdgeRect.all(all), EdgeMask.all, typeAll);
    }

    /**
     * Creates a border property with vertical and horizontal values.
     */
    public BorderProperty(LengthPercentage vertical, LengthPercentage horizontal) {
        this(EdgeRect.symmetric(vertical, horizontal), EdgeMask.all, typeAll);
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
                        LengthPercentage.length(left)),
                EdgeMask.all,
                typeAll);
    }

    // endregion

    // region factory - all edges

    /**
     * Creates a border property with individual LengthPercentage values for each side.
     *
     * @param top    the top border
     * @param right  the right border
     * @param bottom the bottom border
     * @param left   the left border
     * @return a border property affecting all edges
     */
    public static BorderProperty of(
            LengthPercentage top, LengthPercentage right, LengthPercentage bottom, LengthPercentage left) {
        return new BorderProperty(EdgeRect.of(top, right, bottom, left), EdgeMask.all, typeAll);
    }

    // endregion

    // region factory - single edge

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
        return new BorderProperty(EdgeRect.top(value), EdgeMask.top, typeTop);
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
        return new BorderProperty(EdgeRect.right(value), EdgeMask.right, typeRight);
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
        return new BorderProperty(EdgeRect.bottom(value), EdgeMask.bottom, typeBottom);
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
        return new BorderProperty(EdgeRect.left(value), EdgeMask.left, typeLeft);
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
            case top -> top(value);
            case right -> right(value);
            case bottom -> bottom(value);
            case left -> left(value);
        };
    }

    // endregion

    // region factory - horizontal/vertical

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
        return new BorderProperty(EdgeRect.horizontal(value), EdgeMask.horizontal, typeHorizontal);
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
        return new BorderProperty(EdgeRect.vertical(value), EdgeMask.vertical, typeVertical);
    }

    // endregion

    // region factory - percentage

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
                        LengthPercentage.percent(left)),
                EdgeMask.all,
                typeAll);
    }

    // endregion

    // region factory - common patterns

    /**
     * Creates a border with zero on all sides.
     */
    public static BorderProperty none() {
        return new BorderProperty(LengthPercentage.ZERO);
    }

    // endregion

    // region EdgeStyleProperty implementation

    @Override
    protected StyleType<?> typeAll() {
        return typeAll;
    }

    @Override
    protected StyleType<?> typeTop() {
        return typeTop;
    }

    @Override
    protected StyleType<?> typeRight() {
        return typeRight;
    }

    @Override
    protected StyleType<?> typeBottom() {
        return typeBottom;
    }

    @Override
    protected StyleType<?> typeLeft() {
        return typeLeft;
    }

    @Override
    protected StyleType<?> typeHorizontal() {
        return typeHorizontal;
    }

    @Override
    protected StyleType<?> typeVertical() {
        return typeVertical;
    }

    @Override
    protected String formatValue(LengthPercentage value) {
        return TaffyStyleUtil.formatLengthPercentage(value);
    }

    @Override
    protected String baseName() {
        return "border";
    }

    // endregion

    // region LayoutProperty implementation

    @Override
    public void applyToStyle(TaffyStyle style) {
        applyEdges(
                edge -> switch (edge) {
                    case top -> style.border.top;
                    case right -> style.border.right;
                    case bottom -> style.border.bottom;
                    case left -> style.border.left;
                },
                (edge, value) -> {
                    switch (edge) {
                        case top -> style.border.top = value;
                        case right -> style.border.right = value;
                        case bottom -> style.border.bottom = value;
                        case left -> style.border.left = value;
                    }
                });
    }

    // endregion
}
