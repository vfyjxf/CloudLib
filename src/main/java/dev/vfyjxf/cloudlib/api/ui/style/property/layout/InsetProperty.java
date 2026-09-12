package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.EdgeRect;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for absolute positioning insets.
 * <p>
 * Inset controls the offset from the parent's edge when using absolute positioning.
 * Maps to taffy {@link TaffyStyle#inset}.
 * <p>
 * This property supports CSS-like individual edge setting. Each edge has its own
 * StyleType, so setting one edge does not affect others:
 * <pre>{@code
 * // Both top and left will be set independently
 * UIStyle.of(insetTop(10), insetLeft(5))
 * }</pre>
 * <p>
 * The inset value types supported by taffy include:
 * <ul>
 *   <li>{@link LengthPercentageAuto#auto} - automatic positioning</li>
 *   <li>{@link LengthPercentageAuto#length(float)} - fixed pixel offset</li>
 *   <li>{@link LengthPercentageAuto#percent(float)} - percentage of parent (0.0 to 1.0)</li>
 * </ul>
 *
 * @see TaffyStyle#inset
 * @see LengthPercentageAuto
 * @see UIStyles#inset(float)
 */
public final class InsetProperty extends EdgeStyleProperty<LengthPercentageAuto> {

    // region types

    /**
     * StyleType for setting all four inset edges at once.
     */
    public static final StyleType<InsetProperty> typeAll = StyleType.of("inset", () -> null);

    /**
     * StyleType for setting only the top inset edge.
     */
    public static final StyleType<InsetProperty> typeTop = StyleType.of("inset-top", () -> null);

    /**
     * StyleType for setting only the right inset edge.
     */
    public static final StyleType<InsetProperty> typeRight = StyleType.of("inset-right", () -> null);

    /**
     * StyleType for setting only the bottom inset edge.
     */
    public static final StyleType<InsetProperty> typeBottom = StyleType.of("inset-bottom", () -> null);

    /**
     * StyleType for setting only the left inset edge.
     */
    public static final StyleType<InsetProperty> typeLeft = StyleType.of("inset-left", () -> null);

    /**
     * StyleType for setting horizontal inset edges (left and right).
     */
    public static final StyleType<InsetProperty> typeHorizontal = StyleType.of("inset-horizontal", () -> null);

    /**
     * StyleType for setting vertical inset edges (top and bottom).
     */
    public static final StyleType<InsetProperty> typeVertical = StyleType.of("inset-vertical", () -> null);

    /**
     * Legacy type alias for backward compatibility.
     */
    public static final StyleType<InsetProperty> type = typeAll;

    // endregion

    // region constructors

    private InsetProperty(EdgeRect<LengthPercentageAuto> edges, EdgeMask mask, StyleType<?> type) {
        super(edges, mask, type);
    }

    /**
     * Creates an inset property with equal inset on all sides.
     */
    public InsetProperty(LengthPercentageAuto all) {
        this(EdgeRect.all(all), EdgeMask.all, typeAll);
    }

    /**
     * Creates an inset property with vertical and horizontal values.
     */
    public InsetProperty(LengthPercentageAuto vertical, LengthPercentageAuto horizontal) {
        this(EdgeRect.symmetric(vertical, horizontal), EdgeMask.all, typeAll);
    }

    /**
     * Creates an inset property with equal pixel inset on all sides.
     */
    public InsetProperty(float all) {
        this(LengthPercentageAuto.length(all));
    }

    /**
     * Creates an inset property with vertical and horizontal pixel values.
     */
    public InsetProperty(float vertical, float horizontal) {
        this(LengthPercentageAuto.length(vertical), LengthPercentageAuto.length(horizontal));
    }

    /**
     * Creates an inset property with individual pixel values for each side.
     */
    public InsetProperty(float top, float right, float bottom, float left) {
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

    // region factory - all edges

    /**
     * Creates an inset property with individual LengthPercentageAuto values for each side.
     *
     * @param top    the top inset
     * @param right  the right inset
     * @param bottom the bottom inset
     * @param left   the left inset
     * @return an inset property affecting all edges
     */
    public static InsetProperty of(
            LengthPercentageAuto top,
            LengthPercentageAuto right,
            LengthPercentageAuto bottom,
            LengthPercentageAuto left) {
        return new InsetProperty(EdgeRect.of(top, right, bottom, left), EdgeMask.all, typeAll);
    }

    // endregion

    // region factory - single edge

    /**
     * Creates an inset property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty top(float value) {
        return top(LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property with only the top edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty top(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new InsetProperty(EdgeRect.top(value), EdgeMask.top, typeTop);
    }

    /**
     * Creates an inset property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty right(float value) {
        return right(LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property with only the right edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty right(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new InsetProperty(EdgeRect.right(value), EdgeMask.right, typeRight);
    }

    /**
     * Creates an inset property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty bottom(float value) {
        return bottom(LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property with only the bottom edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty bottom(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new InsetProperty(EdgeRect.bottom(value), EdgeMask.bottom, typeBottom);
    }

    /**
     * Creates an inset property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty left(float value) {
        return left(LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property with only the left edge set.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty left(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new InsetProperty(EdgeRect.left(value), EdgeMask.left, typeLeft);
    }

    /**
     * Creates an inset property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty edge(Edge edge, float value) {
        return edge(edge, LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property for a specific edge.
     * This property has its own StyleType, so it won't override other edges.
     */
    public static InsetProperty edge(Edge edge, LengthPercentageAuto value) {
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
     * Creates an inset property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static InsetProperty horizontal(float value) {
        return horizontal(LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property with only horizontal edges (left and right) set.
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static InsetProperty horizontal(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new InsetProperty(EdgeRect.horizontal(value), EdgeMask.horizontal, typeHorizontal);
    }

    /**
     * Creates an inset property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static InsetProperty vertical(float value) {
        return vertical(LengthPercentageAuto.length(value));
    }

    /**
     * Creates an inset property with only vertical edges (top and bottom) set.
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static InsetProperty vertical(LengthPercentageAuto value) {
        Objects.requireNonNull(value, "value");
        return new InsetProperty(EdgeRect.vertical(value), EdgeMask.vertical, typeVertical);
    }

    // endregion

    // region factory - percentage

    /**
     * Creates an inset with percentage values for all sides.
     */
    public static InsetProperty percent(float all) {
        return new InsetProperty(LengthPercentageAuto.percent(all));
    }

    /**
     * Creates an inset with percentage values for vertical and horizontal.
     */
    public static InsetProperty percent(float vertical, float horizontal) {
        return new InsetProperty(LengthPercentageAuto.percent(vertical), LengthPercentageAuto.percent(horizontal));
    }

    /**
     * Creates an inset with percentage values for each side.
     */
    public static InsetProperty percent(float top, float right, float bottom, float left) {
        return new InsetProperty(
                EdgeRect.of(
                        LengthPercentageAuto.percent(top),
                        LengthPercentageAuto.percent(right),
                        LengthPercentageAuto.percent(bottom),
                        LengthPercentageAuto.percent(left)),
                EdgeMask.all,
                typeAll);
    }

    /**
     * Creates an inset property with only top set to percentage.
     */
    public static InsetProperty topPercent(float percent) {
        return top(LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates an inset property with only right set to percentage.
     */
    public static InsetProperty rightPercent(float percent) {
        return right(LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates an inset property with only bottom set to percentage.
     */
    public static InsetProperty bottomPercent(float percent) {
        return bottom(LengthPercentageAuto.percent(percent));
    }

    /**
     * Creates an inset property with only left set to percentage.
     */
    public static InsetProperty leftPercent(float percent) {
        return left(LengthPercentageAuto.percent(percent));
    }

    // endregion

    // region factory - auto

    /**
     * Creates an inset with auto on all sides.
     */
    public static InsetProperty auto() {
        return new InsetProperty(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates an inset property with only top set to auto.
     */
    public static InsetProperty topAuto() {
        return top(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates an inset property with only right set to auto.
     */
    public static InsetProperty rightAuto() {
        return right(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates an inset property with only bottom set to auto.
     */
    public static InsetProperty bottomAuto() {
        return bottom(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates an inset property with only left set to auto.
     */
    public static InsetProperty leftAuto() {
        return left(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates an inset property with auto on horizontal sides (left and right).
     * This property has its own StyleType, so it won't override vertical edges.
     */
    public static InsetProperty autoHorizontal() {
        return horizontal(LengthPercentageAuto.AUTO);
    }

    /**
     * Creates an inset property with auto on vertical sides (top and bottom).
     * This property has its own StyleType, so it won't override horizontal edges.
     */
    public static InsetProperty autoVertical() {
        return vertical(LengthPercentageAuto.AUTO);
    }

    // endregion

    // region factory - common patterns

    /**
     * Creates an inset with zero on all sides.
     */
    public static InsetProperty zero() {
        return new InsetProperty(LengthPercentageAuto.ZERO);
    }

    /**
     * Creates an inset with zero on all sides (alias for zero()).
     */
    public static InsetProperty none() {
        return zero();
    }

    /**
     * Creates an inset from a TaffyRect.
     */
    public static InsetProperty of(TaffyRect<LengthPercentageAuto> rect) {
        Objects.requireNonNull(rect, "rect");
        return new InsetProperty(EdgeRect.of(rect.top, rect.right, rect.bottom, rect.left), EdgeMask.all, typeAll);
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
        return "inset";
    }

    // endregion

    // region LayoutProperty implementation

    @Override
    public void applyToStyle(TaffyStyle style) {
        applyEdges(
                edge -> switch (edge) {
                    case top -> style.inset.top;
                    case right -> style.inset.right;
                    case bottom -> style.inset.bottom;
                    case left -> style.inset.left;
                },
                (edge, value) -> {
                    switch (edge) {
                        case top -> style.inset.top = value;
                        case right -> style.inset.right = value;
                        case bottom -> style.inset.bottom = value;
                        case left -> style.inset.left = value;
                    }
                });
    }

    // endregion

    // region utility

    /**
     * Converts this property to a TaffyRect.
     */
    public TaffyRect<LengthPercentageAuto> toRect() {
        EdgeRect<LengthPercentageAuto> e = edges();
        return new TaffyRect<>(
                e.top() != null ? e.top() : LengthPercentageAuto.AUTO,
                e.right() != null ? e.right() : LengthPercentageAuto.AUTO,
                e.bottom() != null ? e.bottom() : LengthPercentageAuto.AUTO,
                e.left() != null ? e.left() : LengthPercentageAuto.AUTO);
    }

    // endregion
}
