package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Static DSL entry points for creating style properties.
 * <p>
 * Import this class statically to use the fluent style DSL:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.style.Styles.*;
 *
 * var cardStyle = Style.of(
 *     padding(12),
 *     background(0xFFFFFFFF),
 *     border(1, 0xFF666666),
 *     rounded(4)
 * );
 *
 * var buttonStyle = Style.of(
 *     padding(8, 16),
 *     background(0xFF0066CC),
 *     rounded(4),
 *     cursor(Cursor.HAND)
 * );
 *
 * // Compose styles
 * var hoveredButton = buttonStyle.with(
 *     background(0xFF0088FF)
 * );
 *
 * // Merge styles
 * var combined = cardStyle.merge(buttonStyle);
 * }</pre>
 *
 * @see Style
 * @see StyleProperty
 */
@ApiStatus.Experimental
public final class Styles {

    private Styles() {}

    // ==================== Padding ====================

    /**
     * Creates a padding property with equal padding on all sides.
     *
     * @param all the padding value for all sides
     * @return a padding property
     */
    public static PaddingProperty padding(double all) {
        return new PaddingProperty(all);
    }

    /**
     * Creates a padding property with vertical and horizontal values.
     *
     * @param vertical   the padding for top and bottom
     * @param horizontal the padding for left and right
     * @return a padding property
     */
    public static PaddingProperty padding(double vertical, double horizontal) {
        return new PaddingProperty(vertical, horizontal);
    }

    /**
     * Creates a padding property with individual values for each side.
     *
     * @param top    the top padding
     * @param right  the right padding
     * @param bottom the bottom padding
     * @param left   the left padding
     * @return a padding property
     */
    public static PaddingProperty padding(double top, double right, double bottom, double left) {
        return new PaddingProperty(top, right, bottom, left);
    }

    // ==================== Margin ====================

    /**
     * Creates a margin property with equal margin on all sides.
     *
     * @param all the margin value for all sides
     * @return a margin property
     */
    public static MarginProperty margin(double all) {
        return new MarginProperty(all);
    }

    /**
     * Creates a margin property with vertical and horizontal values.
     *
     * @param vertical   the margin for top and bottom
     * @param horizontal the margin for left and right
     * @return a margin property
     */
    public static MarginProperty margin(double vertical, double horizontal) {
        return new MarginProperty(vertical, horizontal);
    }

    /**
     * Creates a margin property with individual values for each side.
     *
     * @param top    the top margin
     * @param right  the right margin
     * @param bottom the bottom margin
     * @param left   the left margin
     * @return a margin property
     */
    public static MarginProperty margin(double top, double right, double bottom, double left) {
        return new MarginProperty(top, right, bottom, left);
    }

    // ==================== Background ====================

    /**
     * Creates a background color property.
     *
     * @param color the background color (ARGB format)
     * @return a background property
     */
    public static BackgroundProperty background(int color) {
        return new BackgroundProperty(color);
    }

    // ==================== Border ====================

    /**
     * Creates a border property.
     *
     * @param width the border width
     * @param color the border color (ARGB format)
     * @return a border property
     */
    public static BorderProperty border(double width, int color) {
        return new BorderProperty(width, color);
    }

    /**
     * Creates a border property with default color (black).
     *
     * @param width the border width
     * @return a border property
     */
    public static BorderProperty border(double width) {
        return new BorderProperty(width, 0xFF000000);
    }

    // ==================== Border Radius ====================

    /**
     * Creates a border radius property with equal radius on all corners.
     *
     * @param radius the radius for all corners
     * @return a rounded property
     */
    public static RoundedProperty rounded(double radius) {
        return new RoundedProperty(radius);
    }

    /**
     * Creates a border radius property with diagonal pair values.
     *
     * @param topLeftBottomRight  the radius for top-left and bottom-right
     * @param topRightBottomLeft  the radius for top-right and bottom-left
     * @return a rounded property
     */
    public static RoundedProperty rounded(double topLeftBottomRight, double topRightBottomLeft) {
        return new RoundedProperty(topLeftBottomRight, topRightBottomLeft);
    }

    /**
     * Creates a border radius property with individual values for each corner.
     *
     * @param topLeft     the top-left radius
     * @param topRight    the top-right radius
     * @param bottomRight the bottom-right radius
     * @param bottomLeft  the bottom-left radius
     * @return a rounded property
     */
    public static RoundedProperty rounded(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        return new RoundedProperty(topLeft, topRight, bottomRight, bottomLeft);
    }

    // ==================== Size ====================

    /**
     * Creates a size property with equal width and height.
     *
     * @param size the width and height
     * @return a size property
     */
    public static SizeProperty size(double size) {
        return new SizeProperty(size);
    }

    /**
     * Creates a size property with specific width and height.
     *
     * @param width  the width
     * @param height the height
     * @return a size property
     */
    public static SizeProperty size(double width, double height) {
        return new SizeProperty(width, height);
    }

    /**
     * Creates a width property.
     *
     * @param width the width
     * @return a size property with only width set
     */
    public static SizeProperty width(double width) {
        return new SizeProperty(width, -1);
    }

    /**
     * Creates a height property.
     *
     * @param height the height
     * @return a size property with only height set
     */
    public static SizeProperty height(double height) {
        return new SizeProperty(-1, height);
    }

    // ==================== Size Constraints ====================

    /**
     * Creates a minimum width constraint.
     *
     * @param value the minimum width
     * @return a size constraint property
     */
    public static SizeConstraintProperty minWidth(double value) {
        return SizeConstraintProperty.minWidth(value);
    }

    /**
     * Creates a minimum height constraint.
     *
     * @param value the minimum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty minHeight(double value) {
        return SizeConstraintProperty.minHeight(value);
    }

    /**
     * Creates a maximum width constraint.
     *
     * @param value the maximum width
     * @return a size constraint property
     */
    public static SizeConstraintProperty maxWidth(double value) {
        return SizeConstraintProperty.maxWidth(value);
    }

    /**
     * Creates a maximum height constraint.
     *
     * @param value the maximum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty maxHeight(double value) {
        return SizeConstraintProperty.maxHeight(value);
    }

    /**
     * Creates minimum size constraints.
     *
     * @param width  the minimum width
     * @param height the minimum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty minSize(double width, double height) {
        return SizeConstraintProperty.minSize(width, height);
    }

    /**
     * Creates maximum size constraints.
     *
     * @param width  the maximum width
     * @param height the maximum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty maxSize(double width, double height) {
        return SizeConstraintProperty.maxSize(width, height);
    }

    // ==================== Text ====================

    /**
     * Creates a text color property.
     *
     * @param color the text color (ARGB format)
     * @return a text color property
     */
    public static TextColorProperty textColor(int color) {
        return new TextColorProperty(color);
    }

    /**
     * Creates a font size property.
     *
     * @param size the font size
     * @return a font size property
     */
    public static FontSizeProperty fontSize(double size) {
        return new FontSizeProperty(size);
    }

    /**
     * Creates a bold text style property.
     *
     * @return a text style property
     */
    public static TextStyleProperty bold() {
        return TextStyleProperty.bold();
    }

    /**
     * Creates an italic text style property.
     *
     * @return a text style property
     */
    public static TextStyleProperty italic() {
        return TextStyleProperty.italic();
    }

    /**
     * Creates an underline text style property.
     *
     * @return a text style property
     */
    public static TextStyleProperty underline() {
        return TextStyleProperty.underline();
    }

    /**
     * Creates a strikethrough text style property.
     *
     * @return a text style property
     */
    public static TextStyleProperty strikethrough() {
        return TextStyleProperty.strikethrough();
    }

    // ==================== Cursor ====================

    /**
     * Creates a cursor property.
     *
     * @param cursor the cursor type
     * @return a cursor property
     */
    public static CursorProperty cursor(Cursor cursor) {
        return new CursorProperty(cursor);
    }

    // ==================== Opacity ====================

    /**
     * Creates an opacity property.
     *
     * @param opacity the opacity (0.0 to 1.0)
     * @return an opacity property
     */
    public static OpacityProperty opacity(double opacity) {
        return new OpacityProperty(opacity);
    }

    // ==================== Custom ====================

    /**
     * Creates a custom property with a lambda applier.
     * <p>
     * Use this for quick one-off styles or when you don't want to create
     * a full StyleProperty implementation.
     * <p>
     * Example:
     * <pre>{@code
     * var style = Style.of(
     *     padding(10),
     *     custom("shadow", ctx -> {
     *         ctx.getVisualContext().setCustomProperty("shadowOffsetX", 2);
     *         ctx.getVisualContext().setCustomProperty("shadowOffsetY", 2);
     *     })
     * );
     * }</pre>
     *
     * @param name    the property name (for deduplication)
     * @param applier the function to apply the property
     * @return a custom property
     */
    public static CustomProperty custom(String name, Consumer<StyleContext> applier) {
        return new CustomProperty(name, applier);
    }

    // ==================== Flex Layout Properties ====================

    /**
     * Creates a flex direction property for row layout (horizontal).
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexRow() {
        return new FlexDirectionProperty(org.appliedenergistics.yoga.YogaFlexDirection.ROW);
    }

    /**
     * Creates a flex direction property for column layout (vertical).
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexColumn() {
        return new FlexDirectionProperty(org.appliedenergistics.yoga.YogaFlexDirection.COLUMN);
    }

    /**
     * Creates a flex direction property for reversed row layout.
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexRowReverse() {
        return new FlexDirectionProperty(org.appliedenergistics.yoga.YogaFlexDirection.ROW_REVERSE);
    }

    /**
     * Creates a flex direction property for reversed column layout.
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexColumnReverse() {
        return new FlexDirectionProperty(org.appliedenergistics.yoga.YogaFlexDirection.COLUMN_REVERSE);
    }

    /**
     * Creates a flex direction property.
     *
     * @param direction the flex direction
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexDirection(org.appliedenergistics.yoga.YogaFlexDirection direction) {
        return new FlexDirectionProperty(direction);
    }

    /**
     * Creates a flex grow property.
     *
     * @param grow the grow factor
     * @return a flex grow property
     */
    public static FlexGrowProperty flexGrow(float grow) {
        return new FlexGrowProperty(grow);
    }

    /**
     * Creates a flex shrink property.
     *
     * @param shrink the shrink factor
     * @return a flex shrink property
     */
    public static FlexShrinkProperty flexShrink(float shrink) {
        return new FlexShrinkProperty(shrink);
    }

    /**
     * Creates a flex wrap property for wrapping.
     *
     * @return a flex wrap property
     */
    public static FlexWrapProperty flexWrap() {
        return new FlexWrapProperty(org.appliedenergistics.yoga.YogaWrap.WRAP);
    }

    /**
     * Creates a flex wrap property for no wrapping.
     *
     * @return a flex wrap property
     */
    public static FlexWrapProperty flexNoWrap() {
        return new FlexWrapProperty(org.appliedenergistics.yoga.YogaWrap.NO_WRAP);
    }

    /**
     * Creates a flex wrap property for reverse wrapping.
     *
     * @return a flex wrap property
     */
    public static FlexWrapProperty flexWrapReverse() {
        return new FlexWrapProperty(org.appliedenergistics.yoga.YogaWrap.WRAP_REVERSE);
    }

    // ==================== Alignment Properties ====================

    /**
     * Creates an align items center property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsCenter() {
        return new AlignItemsProperty(org.appliedenergistics.yoga.YogaAlign.CENTER);
    }

    /**
     * Creates an align items flex-start property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsFlexStart() {
        return new AlignItemsProperty(org.appliedenergistics.yoga.YogaAlign.FLEX_START);
    }

    /**
     * Creates an align items flex-end property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsFlexEnd() {
        return new AlignItemsProperty(org.appliedenergistics.yoga.YogaAlign.FLEX_END);
    }

    /**
     * Creates an align items stretch property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsStretch() {
        return new AlignItemsProperty(org.appliedenergistics.yoga.YogaAlign.STRETCH);
    }

    /**
     * Creates an align items baseline property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsBaseline() {
        return new AlignItemsProperty(org.appliedenergistics.yoga.YogaAlign.BASELINE);
    }

    /**
     * Creates an align items property.
     *
     * @param align the alignment
     * @return an align items property
     */
    public static AlignItemsProperty alignItems(org.appliedenergistics.yoga.YogaAlign align) {
        return new AlignItemsProperty(align);
    }

    /**
     * Creates an align self center property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfCenter() {
        return new AlignSelfProperty(org.appliedenergistics.yoga.YogaAlign.CENTER);
    }

    /**
     * Creates an align self flex-start property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfFlexStart() {
        return new AlignSelfProperty(org.appliedenergistics.yoga.YogaAlign.FLEX_START);
    }

    /**
     * Creates an align self flex-end property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfFlexEnd() {
        return new AlignSelfProperty(org.appliedenergistics.yoga.YogaAlign.FLEX_END);
    }

    /**
     * Creates an align self stretch property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfStretch() {
        return new AlignSelfProperty(org.appliedenergistics.yoga.YogaAlign.STRETCH);
    }

    /**
     * Creates an align self property.
     *
     * @param align the alignment
     * @return an align self property
     */
    public static AlignSelfProperty alignSelf(org.appliedenergistics.yoga.YogaAlign align) {
        return new AlignSelfProperty(align);
    }

    /**
     * Creates an align content center property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentCenter() {
        return new AlignContentProperty(org.appliedenergistics.yoga.YogaAlign.CENTER);
    }

    /**
     * Creates an align content flex-start property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentFlexStart() {
        return new AlignContentProperty(org.appliedenergistics.yoga.YogaAlign.FLEX_START);
    }

    /**
     * Creates an align content flex-end property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentFlexEnd() {
        return new AlignContentProperty(org.appliedenergistics.yoga.YogaAlign.FLEX_END);
    }

    /**
     * Creates an align content stretch property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentStretch() {
        return new AlignContentProperty(org.appliedenergistics.yoga.YogaAlign.STRETCH);
    }

    /**
     * Creates an align content space-between property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentSpaceBetween() {
        return new AlignContentProperty(org.appliedenergistics.yoga.YogaAlign.SPACE_BETWEEN);
    }

    /**
     * Creates an align content space-around property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentSpaceAround() {
        return new AlignContentProperty(org.appliedenergistics.yoga.YogaAlign.SPACE_AROUND);
    }

    /**
     * Creates an align content property.
     *
     * @param align the alignment
     * @return an align content property
     */
    public static AlignContentProperty alignContent(org.appliedenergistics.yoga.YogaAlign align) {
        return new AlignContentProperty(align);
    }

    // ==================== Justify Content ====================

    /**
     * Creates a justify content center property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifyCenter() {
        return new JustifyContentProperty(org.appliedenergistics.yoga.YogaJustify.CENTER);
    }

    /**
     * Creates a justify content flex-start property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifyFlexStart() {
        return new JustifyContentProperty(org.appliedenergistics.yoga.YogaJustify.FLEX_START);
    }

    /**
     * Creates a justify content flex-end property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifyFlexEnd() {
        return new JustifyContentProperty(org.appliedenergistics.yoga.YogaJustify.FLEX_END);
    }

    /**
     * Creates a justify content space-between property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifySpaceBetween() {
        return new JustifyContentProperty(org.appliedenergistics.yoga.YogaJustify.SPACE_BETWEEN);
    }

    /**
     * Creates a justify content space-around property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifySpaceAround() {
        return new JustifyContentProperty(org.appliedenergistics.yoga.YogaJustify.SPACE_AROUND);
    }

    /**
     * Creates a justify content space-evenly property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifySpaceEvenly() {
        return new JustifyContentProperty(org.appliedenergistics.yoga.YogaJustify.SPACE_EVENLY);
    }

    /**
     * Creates a justify content property.
     *
     * @param justify the justification
     * @return a justify content property
     */
    public static JustifyContentProperty justifyContent(org.appliedenergistics.yoga.YogaJustify justify) {
        return new JustifyContentProperty(justify);
    }

    // ==================== Gap Properties ====================

    /**
     * Creates a gap property for all gutters.
     *
     * @param gap the gap size
     * @return a gap property
     */
    public static GapProperty gap(float gap) {
        return new GapProperty(gap);
    }

    /**
     * Creates a row gap property.
     *
     * @param gap the gap size
     * @return a gap property
     */
    public static GapProperty rowGap(float gap) {
        return new GapProperty(org.appliedenergistics.yoga.YogaGutter.ROW, gap);
    }

    /**
     * Creates a column gap property.
     *
     * @param gap the gap size
     * @return a gap property
     */
    public static GapProperty columnGap(float gap) {
        return new GapProperty(org.appliedenergistics.yoga.YogaGutter.COLUMN, gap);
    }

    // ==================== Position Properties ====================

    /**
     * Creates a relative position type property.
     *
     * @return a position type property
     */
    public static PositionTypeProperty positionRelative() {
        return new PositionTypeProperty(org.appliedenergistics.yoga.YogaPositionType.RELATIVE);
    }

    /**
     * Creates an absolute position type property.
     *
     * @return a position type property
     */
    public static PositionTypeProperty positionAbsolute() {
        return new PositionTypeProperty(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE);
    }

    /**
     * Creates a static position type property.
     *
     * @return a position type property
     */
    public static PositionTypeProperty positionStatic() {
        return new PositionTypeProperty(org.appliedenergistics.yoga.YogaPositionType.STATIC);
    }

    /**
     * Creates a position type property.
     *
     * @param type the position type
     * @return a position type property
     */
    public static PositionTypeProperty positionType(org.appliedenergistics.yoga.YogaPositionType type) {
        return new PositionTypeProperty(type);
    }

    /**
     * Creates a top position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty top(float value) {
        return new PositionEdgeProperty(org.appliedenergistics.yoga.YogaEdge.TOP, value);
    }

    /**
     * Creates a right position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty right(float value) {
        return new PositionEdgeProperty(org.appliedenergistics.yoga.YogaEdge.RIGHT, value);
    }

    /**
     * Creates a bottom position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty bottom(float value) {
        return new PositionEdgeProperty(org.appliedenergistics.yoga.YogaEdge.BOTTOM, value);
    }

    /**
     * Creates a left position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty left(float value) {
        return new PositionEdgeProperty(org.appliedenergistics.yoga.YogaEdge.LEFT, value);
    }

    // ==================== Aspect Ratio ====================

    /**
     * Creates an aspect ratio property.
     *
     * @param ratio the aspect ratio (width/height)
     * @return an aspect ratio property
     */
    public static AspectRatioProperty aspectRatio(float ratio) {
        return new AspectRatioProperty(ratio);
    }
}
