package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.EdgeRect;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Built-in padding layout property.
 * <p>
 * Padding is applied to the taffy {@link TaffyStyle#padding} for layout calculation.
 * <p>
 * This property supports CSS-like individual edge setting. Each edge has its own
 * StyleType, so setting one edge does not affect others:
 * <pre>{@code
 * // Both top and left will be set independently
 * UIStyle.of(paddingTop(10), paddingLeft(5))
 * }</pre>
 * <p>
 * The padding types supported by taffy include:
 * <ul>
 *   <li>{@link LengthPercentage#length(float)} - fixed pixel length</li>
 *   <li>{@link LengthPercentage#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 * </ul>
 * <p>
 * Note: Unlike margin, padding does not support AUTO values.
 *
 * @see TaffyStyle#padding
 * @see LengthPercentage
 * @see UIStyles#padding(LengthPercentage)
 */
public final class PaddingProperty extends EdgeStyleProperty<LengthPercentage> {

    //region types

    /**
     * StyleType for setting all four padding edges at once.
     */
    public static final StyleType<PaddingProperty> TYPE_ALL = StyleType.of("padding", () -> null);

    /**
     * StyleType for setting only the top padding edge.
     */
    public static final StyleType<PaddingProperty> TYPE_TOP = StyleType.of("padding-top", () -> null);

    /**
     * StyleType for setting only the right padding edge.
     */
    public static final StyleType<PaddingProperty> TYPE_RIGHT = StyleType.of("padding-right", () -> null);

    /**
     * StyleType for setting only the bottom padding edge.
     */
    public static final StyleType<PaddingProperty> TYPE_BOTTOM = StyleType.of("padding-bottom", () -> null);

    /**
     * StyleType for setting only the left padding edge.
     */
    public static final StyleType<PaddingProperty> TYPE_LEFT = StyleType.of("padding-left", () -> null);

    /**
     * StyleType for setting horizontal padding edges (left and right).
     */
    public static final StyleType<PaddingProperty> TYPE_HORIZONTAL = StyleType.of("padding-horizontal", () -> null);

    /**
     * StyleType for setting vertical padding edges (top and bottom).
     */
    public static final StyleType<PaddingProperty> TYPE_VERTICAL = StyleType.of("padding-vertical", () -> null);

    /**
     * Legacy type alias for backward compatibility.
     */
    public static final StyleType<PaddingProperty> type = TYPE_ALL;

    //endregion

    //region constructors

    private PaddingProperty(EdgeRect<LengthPercentage> edges, EdgeMask mask, StyleType<?> type) {
        super(edges, mask, type);
    }

    /**
     * Creates a padding property with equal padding on all sides.
     */
    public PaddingProperty(LengthPercentage all) {
        this(EdgeRect.all(all), EdgeMask.ALL, TYPE_ALL);
    }

    /**
     * Creates a padding property with vertical and horizontal values.
     */
    public PaddingProperty(LengthPercentage vertical, LengthPercentage horizontal) {
        this(EdgeRect.symmetric(vertical, horizontal), EdgeMask.ALL, TYPE_ALL);
    }

    /**
     * Creates a padding property with equal pixel padding on all sides.
     */
    public PaddingProperty(float all) {
        this(LengthPercentage.length(all));
    }

    /**
     * Creates a padding property with vertical and horizontal pixel values.
     */
    public PaddingProperty(float vertical, float horizontal) {
        this(LengthPercentage.length(vertical), LengthPercentage.length(horizontal));
    }

    /**
     * Creates a padding property with individual pixel values for each side.
     */
    public PaddingProperty(float top, float right, float bottom, float left) {
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
     * Creates a padding property with individual LengthPercentage values for each side.
     *
     * @param top    the top padding
     * @param right  the right padding
     * @param bottom the bottom padding
     * @param left   the left padding
     * @return a padding property affecting all edges
     */
    public static PaddingProperty of(LengthPercentage top, LengthPercentage right, LengthPercentage bottom, LengthPercentage left) {
        return new PaddingProperty(EdgeRect.of(top, right, bottom, left), EdgeMask.ALL, TYPE_ALL);
    }

    //endregion

    //region factory - single edge

    /**
     * Creates a padding property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty top(float value) {
        return top(LengthPercentage.length(value));
    }

    /**
     * Creates a padding property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty top(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new PaddingProperty(EdgeRect.top(value), EdgeMask.TOP, TYPE_TOP);
    }

    /**
     * Creates a padding property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty right(float value) {
        return right(LengthPercentage.length(value));
    }

    /**
     * Creates a padding property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty right(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new PaddingProperty(EdgeRect.right(value), EdgeMask.RIGHT, TYPE_RIGHT);
    }

    /**
     * Creates a padding property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty bottom(float value) {
        return bottom(LengthPercentage.length(value));
    }

    /**
     * Creates a padding property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty bottom(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new PaddingProperty(EdgeRect.bottom(value), EdgeMask.BOTTOM, TYPE_BOTTOM);
    }

    /**
     * Creates a padding property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty left(float value) {
        return left(LengthPercentage.length(value));
    }

    /**
     * Creates a padding property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty left(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new PaddingProperty(EdgeRect.left(value), EdgeMask.LEFT, TYPE_LEFT);
    }

    /**
     * Creates a padding property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty edge(Edge edge, float value) {
        return edge(edge, LengthPercentage.length(value));
    }

    /**
     * Creates a padding property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static PaddingProperty edge(Edge edge, LengthPercentage value) {
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
     * Creates a padding property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static PaddingProperty horizontal(float value) {
        return horizontal(LengthPercentage.length(value));
    }

    /**
     * Creates a padding property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static PaddingProperty horizontal(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new PaddingProperty(EdgeRect.horizontal(value), EdgeMask.HORIZONTAL, TYPE_HORIZONTAL);
    }

    /**
     * Creates a padding property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static PaddingProperty vertical(float value) {
        return vertical(LengthPercentage.length(value));
    }

    /**
     * Creates a padding property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static PaddingProperty vertical(LengthPercentage value) {
        Objects.requireNonNull(value, "value");
        return new PaddingProperty(EdgeRect.vertical(value), EdgeMask.VERTICAL, TYPE_VERTICAL);
    }

    //endregion

    //region factory - percentage

    /**
     * Creates a padding with percentage values for all sides.
     */
    public static PaddingProperty percent(float all) {
        return new PaddingProperty(LengthPercentage.percent(all));
    }

    /**
     * Creates a padding with percentage values for vertical and horizontal.
     */
    public static PaddingProperty percent(float vertical, float horizontal) {
        return new PaddingProperty(LengthPercentage.percent(vertical), LengthPercentage.percent(horizontal));
    }

    /**
     * Creates a padding with percentage values for each side.
     */
    public static PaddingProperty percent(float top, float right, float bottom, float left) {
        return new PaddingProperty(
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
     * Creates a padding with zero on all sides.
     */
    public static PaddingProperty none() {
        return new PaddingProperty(LengthPercentage.ZERO);
    }

    //endregion

    //region EdgeStyleProperty implementation

    @Override
    protected StyleType<?> typeAll() {
        return TYPE_ALL;
    }

    @Override
    protected StyleType<?> typeTop() {
        return TYPE_TOP;
    }

    @Override
    protected StyleType<?> typeRight() {
        return TYPE_RIGHT;
    }

    @Override
    protected StyleType<?> typeBottom() {
        return TYPE_BOTTOM;
    }

    @Override
    protected StyleType<?> typeLeft() {
        return TYPE_LEFT;
    }

    @Override
    protected StyleType<?> typeHorizontal() {
        return TYPE_HORIZONTAL;
    }

    @Override
    protected StyleType<?> typeVertical() {
        return TYPE_VERTICAL;
    }

    @Override
    protected String formatValue(LengthPercentage value) {
        return TaffyStyleUtil.formatLengthPercentage(value);
    }

    @Override
    protected String baseName() {
        return "padding";
    }

    //endregion

    //region LayoutProperty implementation

    @Override
    public void applyToStyle(TaffyStyle style) {
        applyEdges(
                edge -> switch (edge) {
                    case TOP -> style.padding.top;
                    case RIGHT -> style.padding.right;
                    case BOTTOM -> style.padding.bottom;
                    case LEFT -> style.padding.left;
                },
                (edge, value) -> {
                    switch (edge) {
                        case TOP -> style.padding.top = value;
                        case RIGHT -> style.padding.right = value;
                        case BOTTOM -> style.padding.bottom = value;
                        case LEFT -> style.padding.left = value;
                    }
                }
        );
    }

    //endregion
}
