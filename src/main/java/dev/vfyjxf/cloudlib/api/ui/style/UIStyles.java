package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.property.layout.*;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.BackgroundProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.BorderProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.OpacityProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.RoundedProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.TextColorProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.TextStyleProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.List;

/**
 * Static DSL entry points for creating style properties.
 * <p>
 * Import this class statically to use the fluent style DSL:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.style.Styles.*;
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
 *     rounded(4)
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
 * @see UIStyle
 * @see StyleProperty
 */
public final class UIStyles {

    private UIStyles() {}

    /**
     * Creates a padding property with equal padding on all sides.
     *
     * @param all the padding value for all sides
     * @return a padding property
     */
    public static PaddingProperty padding(float all) {
        return new PaddingProperty(all);
    }

    /**
     * Creates a padding property with vertical and horizontal values.
     *
     * @param vertical   the padding for top and bottom
     * @param horizontal the padding for left and right
     * @return a padding property
     */
    public static PaddingProperty padding(float vertical, float horizontal) {
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
    public static PaddingProperty padding(float top, float right, float bottom, float left) {
        return new PaddingProperty(top, right, bottom, left);
    }

    /**
     * Creates a margin property with equal margin on all sides.
     *
     * @param all the margin value for all sides
     * @return a margin property
     */
    public static MarginProperty margin(float all) {
        return new MarginProperty(all);
    }

    /**
     * Creates a margin property with vertical and horizontal values.
     *
     * @param vertical   the margin for top and bottom
     * @param horizontal the margin for left and right
     * @return a margin property
     */
    public static MarginProperty margin(float vertical, float horizontal) {
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
    public static MarginProperty margin(float top, float right, float bottom, float left) {
        return new MarginProperty(top, right, bottom, left);
    }

    /**
     * Creates a background color property.
     *
     * @param texture the background texture
     * @return a background property
     */
    public static BackgroundProperty background(VisualTexture texture) {
        return new BackgroundProperty(texture);
    }

    /**
     * Creates a border property.
     *
     * @param width the border width
     * @param color the border color (ARGB format)
     * @return a border property
     */
    public static BorderProperty border(float width, int color) {
        return new BorderProperty(width, color);
    }

    /**
     * Creates a border property with default color (black).
     *
     * @param width the border width
     * @return a border property
     */
    public static BorderProperty border(float width) {
        return new BorderProperty(width, 0xFF000000);
    }

    /**
     * Creates a border radius property with equal radius on all corners.
     *
     * @param radius the radius for all corners
     * @return a rounded property
     */
    public static RoundedProperty rounded(float radius) {
        return new RoundedProperty(radius);
    }

    /**
     * Creates a border radius property with diagonal pair values.
     *
     * @param topLeftBottomRight the radius for top-left and bottom-right
     * @param topRightBottomLeft the radius for top-right and bottom-left
     * @return a rounded property
     */
    public static RoundedProperty rounded(float topLeftBottomRight, float topRightBottomLeft) {
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
    public static RoundedProperty rounded(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        return new RoundedProperty(topLeft, topRight, bottomRight, bottomLeft);
    }


    public static SizeProperty size(TaffyDimension width, TaffyDimension height) {
        return new SizeProperty(width, height);
    }

    public static SizeProperty size(TaffyDimension size) {
        return new SizeProperty(size, size);
    }

    /**
     * Creates a size property with equal width and height.
     *
     * @param size the width and height
     * @return a size property
     */
    public static SizeProperty size(float size) {
        return new SizeProperty(size);
    }

    /**
     * Creates a size property with specific width and height.
     *
     * @param width  the width
     * @param height the height
     * @return a size property
     */
    public static SizeProperty size(float width, float height) {
        return new SizeProperty(width, height);
    }

    /**
     * Creates a width property.
     *
     * @param width the width
     * @return a size property with only width set
     */
    public static SizeProperty width(float width) {
        return new SizeProperty(width, -1);
    }

    /**
     * Creates a height property.
     *
     * @param height the height
     * @return a size property with only height set
     */
    public static SizeProperty height(float height) {
        return new SizeProperty(-1, height);
    }

    /**
     * Creates a minimum width constraint.
     *
     * @param value the minimum width
     * @return a size constraint property
     */
    public static SizeConstraintProperty minWidth(float value) {
        return SizeConstraintProperty.minWidth(value);
    }

    /**
     * Creates a minimum height constraint.
     *
     * @param value the minimum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty minHeight(float value) {
        return SizeConstraintProperty.minHeight(value);
    }

    /**
     * Creates a maximum width constraint.
     *
     * @param value the maximum width
     * @return a size constraint property
     */
    public static SizeConstraintProperty maxWidth(float value) {
        return SizeConstraintProperty.maxWidth(value);
    }

    /**
     * Creates a maximum height constraint.
     *
     * @param value the maximum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty maxHeight(float value) {
        return SizeConstraintProperty.maxHeight(value);
    }

    /**
     * Creates minimum size constraints.
     *
     * @param width  the minimum width
     * @param height the minimum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty minSize(float width, float height) {
        return SizeConstraintProperty.minSize(width, height);
    }

    /**
     * Creates maximum size constraints.
     *
     * @param width  the maximum width
     * @param height the maximum height
     * @return a size constraint property
     */
    public static SizeConstraintProperty maxSize(float width, float height) {
        return SizeConstraintProperty.maxSize(width, height);
    }

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

    /**
     * Creates an opacity property.
     *
     * @param opacity the opacity (0.0 to 1.0)
     * @return an opacity property
     */
    public static OpacityProperty opacity(float opacity) {
        return new OpacityProperty(opacity);
    }

    /**
     * Creates a flex direction property for row layout (horizontal).
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexRow() {
        return new FlexDirectionProperty(FlexDirection.ROW);
    }

    /**
     * Creates a flex direction property for column layout (vertical).
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexColumn() {
        return new FlexDirectionProperty(FlexDirection.COLUMN);
    }

    /**
     * Creates a flex direction property for reversed row layout.
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexRowReverse() {
        return new FlexDirectionProperty(FlexDirection.ROW_REVERSE);
    }

    /**
     * Creates a flex direction property for reversed column layout.
     *
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexColumnReverse() {
        return new FlexDirectionProperty(FlexDirection.COLUMN_REVERSE);
    }

    /**
     * Creates a flex direction property.
     *
     * @param direction the flex direction
     * @return a flex direction property
     */
    public static FlexDirectionProperty flexDirection(FlexDirection direction) {
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
        return new FlexWrapProperty(FlexWrap.WRAP);
    }

    /**
     * Creates a flex wrap property for no wrapping.
     *
     * @return a flex wrap property
     */
    public static FlexWrapProperty flexNoWrap() {
        return new FlexWrapProperty(FlexWrap.NO_WRAP);
    }

    /**
     * Creates a flex wrap property for reverse wrapping.
     *
     * @return a flex wrap property
     */
    public static FlexWrapProperty flexWrapReverse() {
        return new FlexWrapProperty(FlexWrap.WRAP_REVERSE);
    }

    /**
     * Creates an align items center property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsCenter() {
        return new AlignItemsProperty(AlignItems.CENTER);
    }

    /**
     * Creates an align items flex-start property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsFlexStart() {
        return new AlignItemsProperty(AlignItems.FLEX_START);
    }

    /**
     * Creates an align items flex-end property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsFlexEnd() {
        return new AlignItemsProperty(AlignItems.FLEX_END);
    }

    /**
     * Creates an align items stretch property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsStretch() {
        return new AlignItemsProperty(AlignItems.STRETCH);
    }

    /**
     * Creates an align items baseline property.
     *
     * @return an align items property
     */
    public static AlignItemsProperty alignItemsBaseline() {
        return new AlignItemsProperty(AlignItems.BASELINE);
    }

    /**
     * Creates an align items property.
     *
     * @param align the alignment
     * @return an align items property
     */
    public static AlignItemsProperty alignItems(AlignItems align) {
        return new AlignItemsProperty(align);
    }

    /**
     * Creates an align self center property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfCenter() {
        return new AlignSelfProperty(AlignItems.CENTER);
    }

    /**
     * Creates an align self flex-start property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfFlexStart() {
        return new AlignSelfProperty(AlignItems.FLEX_START);
    }

    /**
     * Creates an align self flex-end property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfFlexEnd() {
        return new AlignSelfProperty(AlignItems.FLEX_END);
    }

    /**
     * Creates an align self stretch property.
     *
     * @return an align self property
     */
    public static AlignSelfProperty alignSelfStretch() {
        return new AlignSelfProperty(AlignItems.STRETCH);
    }

    /**
     * Creates an align self property.
     *
     * @param align the alignment
     * @return an align self property
     */
    public static AlignSelfProperty alignSelf(AlignItems align) {
        return new AlignSelfProperty(align);
    }

    /**
     * Creates an align content center property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentCenter() {
        return new AlignContentProperty(AlignContent.CENTER);
    }

    /**
     * Creates an align content flex-start property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentFlexStart() {
        return new AlignContentProperty(AlignContent.FLEX_START);
    }

    /**
     * Creates an align content flex-end property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentFlexEnd() {
        return new AlignContentProperty(AlignContent.FLEX_END);
    }

    /**
     * Creates an align content stretch property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentStretch() {
        return new AlignContentProperty(AlignContent.STRETCH);
    }

    /**
     * Creates an align content space-between property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentSpaceBetween() {
        return new AlignContentProperty(AlignContent.SPACE_BETWEEN);
    }

    /**
     * Creates an align content space-around property.
     *
     * @return an align content property
     */
    public static AlignContentProperty alignContentSpaceAround() {
        return new AlignContentProperty(AlignContent.SPACE_AROUND);
    }

    /**
     * Creates an align content property.
     *
     * @param align the alignment
     * @return an align content property
     */
    public static AlignContentProperty alignContent(AlignContent align) {
        return new AlignContentProperty(align);
    }

    /**
     * Creates a justify content center property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifyCenter() {
        return new JustifyContentProperty(JustifyContent.CENTER);
    }

    /**
     * Creates a justify content flex-start property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifyFlexStart() {
        return new JustifyContentProperty(JustifyContent.FLEX_START);
    }

    /**
     * Creates a justify content flex-end property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifyFlexEnd() {
        return new JustifyContentProperty(JustifyContent.FLEX_END);
    }

    /**
     * Creates a justify content space-between property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifySpaceBetween() {
        return new JustifyContentProperty(JustifyContent.SPACE_BETWEEN);
    }

    /**
     * Creates a justify content space-around property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifySpaceAround() {
        return new JustifyContentProperty(JustifyContent.SPACE_AROUND);
    }

    /**
     * Creates a justify content space-evenly property.
     *
     * @return a justify content property
     */
    public static JustifyContentProperty justifySpaceEvenly() {
        return new JustifyContentProperty(JustifyContent.SPACE_EVENLY);
    }

    /**
     * Creates a justify content property.
     *
     * @param justify the justification
     * @return a justify content property
     */
    public static JustifyContentProperty justifyContent(JustifyContent justify) {
        return new JustifyContentProperty(justify);
    }

    /**
     * Creates a gap property for all gutters.
     *
     * @param gap the gap size
     * @return a gap property
     */
    public static GapProperty gap(float gap) {
        return GapProperty.all(gap);
    }

    /**
     * Creates a gap property with independent row/column values.
     * <p>
     * Note: In taffy gap, {@code columnGap} maps to {@code Style.gap.width} and {@code rowGap}
     * maps to {@code Style.gap.height}.
     *
     * @param rowGap    the row gap (height)
     * @param columnGap the column gap (width)
     * @return a gap property
     */
    public static GapProperty gap(float rowGap, float columnGap) {
        return GapProperty.both(rowGap, columnGap);
    }

    /**
     * Creates a row gap property.
     *
     * @param gap the gap size
     * @return a gap property
     */
    public static GapProperty rowGap(float gap) {
        return GapProperty.rowGap(gap);
    }

    /**
     * Creates a column gap property.
     *
     * @param gap the gap size
     * @return a gap property
     */
    public static GapProperty columnGap(float gap) {
        return GapProperty.columnGap(gap);
    }

    /**
     * Creates a relative position type property.
     *
     * @return a position type property
     */
    public static PositionTypeProperty positionRelative() {
        return new PositionTypeProperty(TaffyPosition.RELATIVE);
    }

    /**
     * Creates an absolute position type property.
     *
     * @return a position type property
     */
    public static PositionTypeProperty positionAbsolute() {
        return new PositionTypeProperty(TaffyPosition.ABSOLUTE);
    }

    /**
     * Creates a static position type property.
     *
     * @return a position type property
     */
    public static PositionTypeProperty positionStatic() {
        return new PositionTypeProperty(TaffyPosition.RELATIVE);
    }

    /**
     * Creates a position type property.
     *
     * @param type the position type
     * @return a position type property
     */
    public static PositionTypeProperty positionType(TaffyPosition type) {
        return new PositionTypeProperty(type);
    }

    /**
     * Creates a top position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty top(float value) {
        return new PositionEdgeProperty(Edge.TOP, value);
    }

    /**
     * Creates a right position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty right(float value) {
        return new PositionEdgeProperty(Edge.RIGHT, value);
    }

    /**
     * Creates a bottom position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty bottom(float value) {
        return new PositionEdgeProperty(Edge.BOTTOM, value);
    }

    /**
     * Creates a left position edge property.
     *
     * @param value the position value
     * @return a position edge property
     */
    public static PositionEdgeProperty left(float value) {
        return new PositionEdgeProperty(Edge.LEFT, value);
    }

    /**
     * Creates an aspect ratio property.
     *
     * @param ratio the aspect ratio (width/height)
     * @return an aspect ratio property
     */
    public static AspectRatioProperty aspectRatio(float ratio) {
        return new AspectRatioProperty(ratio);
    }

    public static DisplayProperty display(TaffyDisplay display) {
        return new DisplayProperty(display);
    }

    public static DisplayProperty displayDefault() {
        return new DisplayProperty(TaffyDisplay.DEFAULT);
    }

    public static DisplayProperty displayNone() {
        return new DisplayProperty(TaffyDisplay.NONE);
    }

    public static DisplayProperty displayBlock() {
        return new DisplayProperty(TaffyDisplay.BLOCK);
    }

    public static DisplayProperty displayFlex() {
        return new DisplayProperty(TaffyDisplay.FLEX);
    }

    public static DisplayProperty displayGrid() {
        return new DisplayProperty(TaffyDisplay.GRID);
    }

    public static ItemIsTableProperty itemIsTable(boolean value) {
        return new ItemIsTableProperty(value);
    }

    public static ItemIsReplacedProperty itemIsReplaced(boolean value) {
        return new ItemIsReplacedProperty(value);
    }

    public static BoxSizingProperty boxSizing(BoxSizing boxSizing) {
        return new BoxSizingProperty(boxSizing);
    }

    public static BoxSizingProperty boxSizingBorderBox() {
        return new BoxSizingProperty(BoxSizing.BORDER_BOX);
    }

    public static BoxSizingProperty boxSizingContentBox() {
        return new BoxSizingProperty(BoxSizing.CONTENT_BOX);
    }

    /**
     * Sets overflow for both axes.
     */
    public static OverflowProperty overflow(Overflow overflow) {
        return new OverflowProperty(overflow);
    }

    /**
     * Sets overflow-x only (y is kept).
     */
    public static OverflowProperty overflowX(Overflow overflow) {
        return new OverflowProperty(overflow, null);
    }

    /**
     * Sets overflow-y only (x is kept).
     */
    public static OverflowProperty overflowY(Overflow overflow) {
        return new OverflowProperty(null, overflow);
    }

    public static ScrollbarWidthProperty scrollbarWidth(float width) {
        return new ScrollbarWidthProperty(width);
    }

    public static JustifyItemsProperty justifyItems(AlignItems align) {
        return new JustifyItemsProperty(align);
    }

    public static JustifySelfProperty justifySelf(AlignItems align) {
        return new JustifySelfProperty(align);
    }

    public static TextAlignProperty textAlign(TextAlign align) {
        return new TextAlignProperty(align);
    }

    public static TextAlignProperty textAlignCenter() {
        return new TextAlignProperty(TextAlign.CENTER);
    }

    public static TextAlignProperty textAlignLeft() {
        return new TextAlignProperty(TextAlign.LEFT);
    }

    public static TextAlignProperty textAlignRight() {
        return new TextAlignProperty(TextAlign.RIGHT);
    }

    public static FlexBasisProperty flexBasis(TaffyDimension basis) {
        return new FlexBasisProperty(basis);
    }

    public static FlexBasisProperty flexBasisAuto() {
        return new FlexBasisProperty(TaffyDimension.AUTO);
    }

    public static FlexBasisProperty flexBasisPx(float px) {
        return new FlexBasisProperty(TaffyDimension.length(px));
    }

    /**
     * @param percent01 percentage in range [0..1], consistent with taffy Dimension.percent
     */
    public static FlexBasisProperty flexBasisPercent(float percent01) {
        return new FlexBasisProperty(TaffyDimension.percent(percent01));
    }

    public static GridTemplateRowsProperty gridTemplateRows(List<TrackSizingFunction> rows) {
        return new GridTemplateRowsProperty(rows);
    }

    public static GridTemplateRowsProperty gridTemplateRows(TrackSizingFunction... rows) {
        return new GridTemplateRowsProperty(java.util.List.of(rows));
    }

    public static GridTemplateColumnsProperty gridTemplateColumns(List<TrackSizingFunction> columns) {
        return new GridTemplateColumnsProperty(columns);
    }

    public static GridTemplateColumnsProperty gridTemplateColumns(TrackSizingFunction... columns) {
        return new GridTemplateColumnsProperty(java.util.List.of(columns));
    }

    public static GridTemplateRowsWithRepeatProperty gridTemplateRowsWithRepeat(List<GridTemplateComponent> rows) {
        return new GridTemplateRowsWithRepeatProperty(rows);
    }

    public static GridTemplateColumnsWithRepeatProperty gridTemplateColumnsWithRepeat(List<GridTemplateComponent> columns) {
        return new GridTemplateColumnsWithRepeatProperty(columns);
    }

    public static GridAutoRowsProperty gridAutoRows(List<TrackSizingFunction> rows) {
        return new GridAutoRowsProperty(rows);
    }

    public static GridAutoRowsProperty gridAutoRows(TrackSizingFunction... rows) {
        return new GridAutoRowsProperty(java.util.List.of(rows));
    }

    public static GridAutoColumnsProperty gridAutoColumns(List<TrackSizingFunction> columns) {
        return new GridAutoColumnsProperty(columns);
    }

    public static GridAutoColumnsProperty gridAutoColumns(TrackSizingFunction... columns) {
        return new GridAutoColumnsProperty(java.util.List.of(columns));
    }

    public static GridAutoFlowProperty gridAutoFlow(GridAutoFlow flow) {
        return new GridAutoFlowProperty(flow);
    }

    public static GridAutoFlowProperty gridAutoFlowRow() {
        return new GridAutoFlowProperty(GridAutoFlow.ROW);
    }

    public static GridAutoFlowProperty gridAutoFlowColumn() {
        return new GridAutoFlowProperty(GridAutoFlow.COLUMN);
    }

    public static GridAutoFlowProperty gridAutoFlowRowDense() {
        return new GridAutoFlowProperty(GridAutoFlow.ROW_DENSE);
    }

    public static GridAutoFlowProperty gridAutoFlowColumnDense() {
        return new GridAutoFlowProperty(GridAutoFlow.COLUMN_DENSE);
    }

    public static GridRowProperty gridRow(GridPlacement start, GridPlacement end) {
        return new GridRowProperty(start, end);
    }

    public static GridColumnProperty gridColumn(GridPlacement start, GridPlacement end) {
        return new GridColumnProperty(start, end);
    }

    public static GridPlacement gridAuto() {
        return GridPlacement.auto();
    }

    public static GridPlacement gridLine(int line) {
        return GridPlacement.line(line);
    }

    public static GridPlacement gridSpan(int span) {
        return GridPlacement.span(span);
    }

    public static GridPlacement gridNamedLine(String name) {
        return GridPlacement.namedLine(name);
    }

    // === Direction Properties ===

    /**
     * Creates a direction property with the specified direction.
     *
     * @param direction the text direction
     * @return a direction property
     */
    public static DirectionProperty direction(TaffyDirection direction) {
        return new DirectionProperty(direction);
    }

    /**
     * Creates a direction property that inherits from parent.
     *
     * @return a direction property with INHERIT
     */
    public static DirectionProperty directionInherit() {
        return new DirectionProperty(TaffyDirection.INHERIT);
    }

    /**
     * Creates a left-to-right direction property.
     *
     * @return a direction property with LTR
     */
    public static DirectionProperty directionLtr() {
        return new DirectionProperty(TaffyDirection.LTR);
    }

    /**
     * Creates a right-to-left direction property.
     *
     * @return a direction property with RTL
     */
    public static DirectionProperty directionRtl() {
        return new DirectionProperty(TaffyDirection.RTL);
    }

    // === Flex Shorthand Properties ===

    /**
     * Creates a flex shorthand property.
     * <p>
     * When flex >= 0: flexGrow = flex, flexShrink = 1, flexBasis = 0
     * When flex < 0: flexGrow = 0, flexShrink = -flex, flexBasis = 0
     *
     * @param flex the flex value
     * @return a flex property
     */
    public static FlexProperty flex(float flex) {
        return new FlexProperty(flex);
    }

    /**
     * Clears the flex shorthand, reverting to individual flexGrow/flexShrink/flexBasis values.
     *
     * @return a flex property with NaN (disabled)
     */
    public static FlexProperty flexNone() {
        return new FlexProperty(Float.NaN);
    }

    // === Grid Template Areas Properties ===

    /**
     * Creates a grid template areas property.
     *
     * @param areas the list of named grid areas
     * @return a grid template areas property
     */
    public static GridTemplateAreasProperty gridTemplateAreas(List<GridTemplateArea> areas) {
        return new GridTemplateAreasProperty(areas);
    }

    /**
     * Creates a grid template areas property.
     *
     * @param areas the named grid areas
     * @return a grid template areas property
     */
    public static GridTemplateAreasProperty gridTemplateAreas(GridTemplateArea... areas) {
        return new GridTemplateAreasProperty(areas);
    }

    /**
     * Creates a named grid area.
     *
     * @param name        the area name
     * @param rowStart    the row start line (1-based)
     * @param rowEnd      the row end line (1-based)
     * @param columnStart the column start line (1-based)
     * @param columnEnd   the column end line (1-based)
     * @return a grid template area
     */
    public static GridTemplateArea gridArea(String name, int rowStart, int rowEnd, int columnStart, int columnEnd) {
        return new GridTemplateArea(name, rowStart, rowEnd, columnStart, columnEnd);
    }

    // === Grid Template Named Lines Properties ===

    /**
     * Creates a grid template column names property.
     *
     * @param columnNames the list of named grid lines
     * @return a grid template column names property
     */
    public static GridTemplateColumnNamesProperty gridTemplateColumnNames(List<NamedGridLine> columnNames) {
        return new GridTemplateColumnNamesProperty(columnNames);
    }

    /**
     * Creates a grid template column names property.
     *
     * @param columnNames the named grid lines
     * @return a grid template column names property
     */
    public static GridTemplateColumnNamesProperty gridTemplateColumnNames(NamedGridLine... columnNames) {
        return new GridTemplateColumnNamesProperty(columnNames);
    }

    /**
     * Creates a grid template row names property.
     *
     * @param rowNames the list of named grid lines
     * @return a grid template row names property
     */
    public static GridTemplateRowNamesProperty gridTemplateRowNames(List<NamedGridLine> rowNames) {
        return new GridTemplateRowNamesProperty(rowNames);
    }

    /**
     * Creates a grid template row names property.
     *
     * @param rowNames the named grid lines
     * @return a grid template row names property
     */
    public static GridTemplateRowNamesProperty gridTemplateRowNames(NamedGridLine... rowNames) {
        return new GridTemplateRowNamesProperty(rowNames);
    }

    /**
     * Creates a named grid line.
     *
     * @param name  the line name
     * @param index the line index (1-based)
     * @return a named grid line
     */
    public static NamedGridLine namedGridLine(String name, int index) {
        return new NamedGridLine(name, index);
    }
}
