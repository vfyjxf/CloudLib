package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.EdgeRect;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Built-in margin layout property.
 * <p>
 * Margin is applied to the taffy {@link TaffyStyle#margin} for layout calculation.
 * <p>
 * This property supports CSS-like individual edge setting. Each edge has its own
 * StyleType, so setting one edge does not affect others:
 * <pre>{@code
 * // Both top and left will be set independently
 * UIStyle.of(marginTop(10), marginLeft(5))
 * }</pre>
 * <p>
 * The margin types supported by taffy include:
 * <ul>
 *   <li>{@link LengthPercentageAuto#auto} - automatic margin</li>
 *   <li>{@link LengthPercentageAuto#length(float)} - fixed pixel length</li>
 *   <li>{@link LengthPercentageAuto#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 *   <li>{@link LengthPercentageAuto#minContent()} - minimum content size</li>
 *   <li>{@link LengthPercentageAuto#maxContent()} - maximum content size</li>
 *   <li>{@link LengthPercentageAuto#fitContent()} - fit content size</li>
 *   <li>{@link LengthPercentageAuto#stretch()} - stretch to fill available space</li>
 * </ul>
 *
 * @see TaffyStyle#margin
 * @see LengthPercentageAuto
 * @see UIStyles#margin(LengthPercentageAuto)
 */
public final class MarginProperty extends EdgeStyleProperty<LengthPercentageAuto> {

    // region types

    /**
     * StyleType for setting all four margin edges at once.
     */
    public static final StyleType<MarginProperty> typeAll = StyleType.of("margin", () -> null);

    /**
     * StyleType for setting only the top margin edge.
     */
    public static final StyleType<MarginProperty> typeTop = StyleType.of("margin-top", () -> null);

    /**
     * StyleType for setting only the right margin edge.
     */
    public static final StyleType<MarginProperty> typeRight = StyleType.of("margin-right", () -> null);

    /**
     * StyleType for setting only the bottom margin edge.
     */
    public static final StyleType<MarginProperty> typeBottom = StyleType.of("margin-bottom", () -> null);

    /**
     * StyleType for setting only the left margin edge.
     */
    public static final StyleType<MarginProperty> typeLeft = StyleType.of("margin-left", () -> null);

    /**
     * StyleType for setting horizontal margin edges (left and right).
     */
    public static final StyleType<MarginProperty> typeHorizontal = StyleType.of("margin-horizontal", () -> null);

    /**
     * StyleType for setting vertical margin edges (top and bottom).
     */
    public static final StyleType<MarginProperty> typeVertical = StyleType.of("margin-vertical", () -> null);

    /**
     * Legacy type alias for backward compatibility.
     */
    public static final StyleType<MarginProperty> type = typeAll;

    // endregion

    // region constructors

    private MarginProperty(EdgeRect<LengthPercentageAuto> edges, EdgeMask mask, StyleType<?> type) {
        super(edges, mask, type);
    }

    /**
     * Creates a margin property with equal margin on all sides.
     */
    public MarginProperty(LengthPercentageAuto all) {
        this(EdgeRect.all(all), EdgeMask.all, typeAll);
    }

    /**
     * Creates a margin property with vertical and horizontal values.
     */
    public MarginProperty(LengthPercentageAuto vertical, LengthPercentageAuto horizontal) {
        this(EdgeRect.symmetric(vertical, horizontal), EdgeMask.all, typeAll);
    }

    /**
     * Creates a margin property with equal pixel margin on all sides.
     */
    public MarginProperty(float all) {
        this(LengthPercentageAuto.length(all));
    }

    /**
     * Creates a margin property with vertical and horizontal pixel values.
     */
    public MarginProperty(float vertical, float horizontal) {
        this(LengthPercentageAuto.length(vertical), LengthPercentageAuto.length(horizontal));
    }

    /**
     * Creates a margin property with individual pixel values for each side.
     */
    public MarginProperty(float top, float right, float bottom, float left) {
        this(
                EdgeRect.of(
                        LengthPercentageAuto.length(top),
                        LengthPercentageAuto.length(right),
                        LengthPercentageAuto.length(bottom),
                        LengthPercentageAuto.length(left)),
                EdgeMask.all,
                typeAll);
    }

    // endregion

    // region factory methods

    /**
     * Creates a margin with auto on all sides.
     */
    public static MarginProperty auto() {
        return new MarginProperty(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates a margin property with auto on horizontal sides (left and right).
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static MarginProperty autoHorizontal() {
        return horizontal(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates a margin property with auto on vertical sides (top and bottom).
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static MarginProperty autoVertical() {
        return vertical(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates a margin with percentage values.
     */
    public static MarginProperty percent(float all) {
        return new MarginProperty(LengthPercentageAuto.percent(all));
    }

    /**
     * Creates a margin with percentage values for vertical and horizontal.
     */
    public static MarginProperty percent(float vertical, float horizontal) {
        return new MarginProperty(LengthPercentageAuto.percent(vertical), LengthPercentageAuto.percent(horizontal));
    }

    // endregion

    // region factory - all edges

    /**
     * Creates a margin property with individual LengthPercentageAuto values for each side.
     *
     * @param top    the top margin
     * @param right  the right margin
     * @param bottom the bottom margin
     * @param left   the left margin
     * @return a margin property affecting all edges
     */
    public static MarginProperty of(
            LengthPercentageAuto top,
            LengthPercentageAuto right,
            LengthPercentageAuto bottom,
            LengthPercentageAuto left) {
        return new MarginProperty(EdgeRect.of(top, right, bottom, left), EdgeMask.all, typeAll);
    }

    // endregion

    // region factory - single edge

    /**
     * Creates a margin property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty top(float value) {
        return top(LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty top(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new MarginProperty(EdgeRect.top(value), EdgeMask.top, typeTop);
    }

    /**
     * Creates a margin property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty right(float value) {
        return right(LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty right(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new MarginProperty(EdgeRect.right(value), EdgeMask.right, typeRight);
    }

    /**
     * Creates a margin property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty bottom(float value) {
        return bottom(LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty bottom(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new MarginProperty(EdgeRect.bottom(value), EdgeMask.bottom, typeBottom);
    }

    /**
     * Creates a margin property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty left(float value) {
        return left(LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty left(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new MarginProperty(EdgeRect.left(value), EdgeMask.left, typeLeft);
    }

    /**
     * Creates a margin property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty edge(Edge edge, float value) {
        return edge(edge, LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static MarginProperty edge(Edge edge, LengthPercentageAuto value) {
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
     * Creates a margin property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static MarginProperty horizontal(float value) {
        return horizontal(LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static MarginProperty horizontal(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new MarginProperty(EdgeRect.horizontal(value), EdgeMask.horizontal, typeHorizontal);
    }

    /**
     * Creates a margin property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static MarginProperty vertical(float value) {
        return vertical(LengthPercentageAuto.length(value));
    }

    /**
     * Creates a margin property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static MarginProperty vertical(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new MarginProperty(EdgeRect.vertical(value), EdgeMask.vertical, typeVertical);
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
    protected String formatValue(LengthPercentageAuto value) {
        return TaffyStyleUtil.formatLengthPercentageAuto(value);
    }

    @Override
    protected String baseName() {
        return "margin";
    }

    // endregion

    // region LayoutProperty implementation

    @Override
    public void applyToStyle(TaffyStyle style) {
        applyEdges(
                edge -> switch (edge) {
                    case top -> style.margin.top;
                    case right -> style.margin.right;
                    case bottom -> style.margin.bottom;
                    case left -> style.margin.left;
                },
                (edge, value) -> {
                    switch (edge) {
                        case top -> style.margin.top = value;
                        case right -> style.margin.right = value;
                        case bottom -> style.margin.bottom = value;
                        case left -> style.margin.left = value;
                    }
                });
    }

    // endregion
}
