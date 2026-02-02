package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.property.layout.*;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.BackgroundProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.BorderProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.IconProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.ShadowProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.TextColorProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.taffy.style.*;

import java.util.List;

/**
 * Static DSL entry points for creating style properties.
 * <p>
 * Import this class statically to use the fluent style DSL:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
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
 * @see TaffyStyle
 */
public final class UIStyles {

    private UIStyles() {}

    //region spacing properties

    /**
     * Creates a padding property with equal padding on all sides.
     *
     * @param all the padding value for all sides
     * @return a padding property
     * @see TaffyStyle#padding
     * @see LengthPercentage
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
     * @see TaffyStyle#padding
     * @see LengthPercentage
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
     * @see TaffyStyle#padding
     * @see LengthPercentage
     */
    public static PaddingProperty padding(float top, float right, float bottom, float left) {
        return new PaddingProperty(top, right, bottom, left);
    }

    /**
     * Creates a margin property with equal margin on all sides.
     *
     * @param all the margin value for all sides
     * @return a margin property
     * @see TaffyStyle#margin
     * @see LengthPercentageAuto
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
     * @see TaffyStyle#margin
     * @see LengthPercentageAuto
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
     * @see TaffyStyle#margin
     * @see LengthPercentageAuto
     */
    public static MarginProperty margin(float top, float right, float bottom, float left) {
        return new MarginProperty(top, right, bottom, left);
    }

    //endregion

    //region visual properties

    /**
     * Creates a background property.
     *
     * @param texture the background texture
     * @return a background property
     */
    public static BackgroundProperty background(VisualTexture texture) {
        return new BackgroundProperty(texture);
    }

    /**
     * Creates a border property (visual only, not layout).
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

    //endregion

    //region size properties

    /**
     * Creates a size property with taffy dimension values.
     *
     * @param width  the width dimension
     * @param height the height dimension
     * @return a size property
     * @see TaffyStyle#size
     * @see TaffyDimension
     */
    public static SizeProperty size(TaffyDimension width, TaffyDimension height) {
        return new SizeProperty(width, height);
    }

    /**
     * Creates a size property with the same dimension for width and height.
     *
     * @param size the size dimension
     * @return a size property
     * @see TaffyStyle#size
     * @see TaffyDimension
     */
    public static SizeProperty size(TaffyDimension size) {
        return new SizeProperty(size, size);
    }

    /**
     * Creates a size property with equal width and height.
     *
     * @param size the width and height
     * @return a size property
     * @see TaffyStyle#size
     * @see TaffyDimension
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
     * @see TaffyStyle#size
     * @see TaffyDimension
     */
    public static SizeProperty size(float width, float height) {
        return new SizeProperty(width, height);
    }

    /**
     * Creates a width property.
     *
     * @param width the width
     * @return a size property with only width set
     * @see TaffyStyle#size
     * @see TaffyDimension
     */
    public static SizeProperty width(float width) {
        return new SizeProperty(width, -1);
    }

    /**
     * Creates a height property.
     *
     * @param height the height
     * @return a size property with only height set
     * @see TaffyStyle#size
     * @see TaffyDimension
     */
    public static SizeProperty height(float height) {
        return new SizeProperty(-1, height);
    }

    /**
     * Creates a minimum width constraint.
     *
     * @param value the minimum width
     * @return a size constraint property
     * @see TaffyStyle#minSize
     * @see TaffyDimension
     */
    public static SizeConstraintProperty minWidth(float value) {
        return SizeConstraintProperty.minWidth(value);
    }

    /**
     * Creates a minimum height constraint.
     *
     * @param value the minimum height
     * @return a size constraint property
     * @see TaffyStyle#minSize
     * @see TaffyDimension
     */
    public static SizeConstraintProperty minHeight(float value) {
        return SizeConstraintProperty.minHeight(value);
    }

    /**
     * Creates a maximum width constraint.
     *
     * @param value the maximum width
     * @return a size constraint property
     * @see TaffyStyle#maxSize
     * @see TaffyDimension
     */
    public static SizeConstraintProperty maxWidth(float value) {
        return SizeConstraintProperty.maxWidth(value);
    }

    /**
     * Creates a maximum height constraint.
     *
     * @param value the maximum height
     * @return a size constraint property
     * @see TaffyStyle#maxSize
     * @see TaffyDimension
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
     * @see TaffyStyle#minSize
     * @see TaffyDimension
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
     * @see TaffyStyle#maxSize
     * @see TaffyDimension
     */
    public static SizeConstraintProperty maxSize(float width, float height) {
        return SizeConstraintProperty.maxSize(width, height);
    }

    //endregion

    //region text properties

    /**
     * Creates a text color property.
     *
     * @param color the text color (ARGB format)
     * @return a text color property
     */
    public static TextColorProperty textColor(int color) {
        return new TextColorProperty(color);
    }

    //endregion

    //region flexbox properties

    /**
     * Creates a flex direction property for row layout (horizontal).
     *
     * @return a flex direction property
     * @see TaffyStyle#flexDirection
     * @see FlexDirection
     */
    public static FlexDirectionProperty flexRow() {
        return new FlexDirectionProperty(FlexDirection.ROW);
    }

    /**
     * Creates a flex direction property for column layout (vertical).
     *
     * @return a flex direction property
     * @see TaffyStyle#flexDirection
     * @see FlexDirection#COLUMN
     */
    public static FlexDirectionProperty flexColumn() {
        return new FlexDirectionProperty(FlexDirection.COLUMN);
    }

    /**
     * Creates a flex direction property for reversed row layout.
     *
     * @return a flex direction property
     * @see TaffyStyle#flexDirection
     * @see FlexDirection#ROW_REVERSE
     */
    public static FlexDirectionProperty flexRowReverse() {
        return new FlexDirectionProperty(FlexDirection.ROW_REVERSE);
    }

    /**
     * Creates a flex direction property for reversed column layout.
     *
     * @return a flex direction property
     * @see TaffyStyle#flexDirection
     * @see FlexDirection#COLUMN_REVERSE
     */
    public static FlexDirectionProperty flexColumnReverse() {
        return new FlexDirectionProperty(FlexDirection.COLUMN_REVERSE);
    }

    /**
     * Creates a flex direction property.
     *
     * @param direction the flex direction
     * @return a flex direction property
     * @see TaffyStyle#flexDirection
     * @see FlexDirection
     */
    public static FlexDirectionProperty flexDirection(FlexDirection direction) {
        return new FlexDirectionProperty(direction);
    }

    /**
     * Creates a flex grow property.
     *
     * @param grow the grow factor
     * @return a flex grow property
     * @see TaffyStyle#flexGrow
     */
    public static FlexGrowProperty flexGrow(float grow) {
        return new FlexGrowProperty(grow);
    }

    /**
     * Creates a flex shrink property.
     *
     * @param shrink the shrink factor
     * @return a flex shrink property
     * @see TaffyStyle#flexShrink
     */
    public static FlexShrinkProperty flexShrink(float shrink) {
        return new FlexShrinkProperty(shrink);
    }

    /**
     * Creates a flex wrap property for wrapping.
     *
     * @return a flex wrap property
     * @see TaffyStyle#flexWrap
     * @see FlexWrap#WRAP
     */
    public static FlexWrapProperty flexWrap() {
        return new FlexWrapProperty(FlexWrap.WRAP);
    }

    /**
     * Creates a flex wrap property for no wrapping.
     *
     * @return a flex wrap property
     * @see TaffyStyle#flexWrap
     * @see FlexWrap#NO_WRAP
     */
    public static FlexWrapProperty flexNoWrap() {
        return new FlexWrapProperty(FlexWrap.NO_WRAP);
    }

    /**
     * Creates a flex wrap property for reverse wrapping.
     *
     * @return a flex wrap property
     * @see TaffyStyle#flexWrap
     * @see FlexWrap#WRAP_REVERSE
     */
    public static FlexWrapProperty flexWrapReverse() {
        return new FlexWrapProperty(FlexWrap.WRAP_REVERSE);
    }

    //endregion

    //region alignment properties

    /**
     * Creates an align items center property.
     *
     * @return an align items property
     * @see TaffyStyle#alignItems
     * @see AlignItems#CENTER
     */
    public static AlignItemsProperty alignItemsCenter() {
        return new AlignItemsProperty(AlignItems.CENTER);
    }

    /**
     * Creates an align items flex-start property.
     *
     * @return an align items property
     * @see TaffyStyle#alignItems
     * @see AlignItems#FLEX_START
     */
    public static AlignItemsProperty alignItemsFlexStart() {
        return new AlignItemsProperty(AlignItems.FLEX_START);
    }

    /**
     * Creates an align items flex-end property.
     *
     * @return an align items property
     * @see TaffyStyle#alignItems
     * @see AlignItems#FLEX_END
     */
    public static AlignItemsProperty alignItemsFlexEnd() {
        return new AlignItemsProperty(AlignItems.FLEX_END);
    }

    /**
     * Creates an align items stretch property.
     *
     * @return an align items property
     * @see TaffyStyle#alignItems
     * @see AlignItems#STRETCH
     */
    public static AlignItemsProperty alignItemsStretch() {
        return new AlignItemsProperty(AlignItems.STRETCH);
    }

    /**
     * Creates an align items baseline property.
     *
     * @return an align items property
     * @see TaffyStyle#alignItems
     * @see AlignItems#BASELINE
     */
    public static AlignItemsProperty alignItemsBaseline() {
        return new AlignItemsProperty(AlignItems.BASELINE);
    }

    /**
     * Creates an align items property.
     *
     * @param align the alignment
     * @return an align items property
     * @see TaffyStyle#alignItems
     * @see AlignItems
     */
    public static AlignItemsProperty alignItems(AlignItems align) {
        return new AlignItemsProperty(align);
    }

    /**
     * Creates an align self center property.
     *
     * @return an align self property
     * @see TaffyStyle#alignSelf
     * @see AlignItems#CENTER
     */
    public static AlignSelfProperty alignSelfCenter() {
        return new AlignSelfProperty(AlignItems.CENTER);
    }

    /**
     * Creates an align self flex-start property.
     *
     * @return an align self property
     * @see TaffyStyle#alignSelf
     * @see AlignItems#FLEX_START
     */
    public static AlignSelfProperty alignSelfFlexStart() {
        return new AlignSelfProperty(AlignItems.FLEX_START);
    }

    /**
     * Creates an align self flex-end property.
     *
     * @return an align self property
     * @see TaffyStyle#alignSelf
     * @see AlignItems#FLEX_END
     */
    public static AlignSelfProperty alignSelfFlexEnd() {
        return new AlignSelfProperty(AlignItems.FLEX_END);
    }

    /**
     * Creates an align self stretch property.
     *
     * @return an align self property
     * @see TaffyStyle#alignSelf
     * @see AlignItems#STRETCH
     */
    public static AlignSelfProperty alignSelfStretch() {
        return new AlignSelfProperty(AlignItems.STRETCH);
    }

    /**
     * Creates an align self property.
     *
     * @param align the alignment
     * @return an align self property
     * @see TaffyStyle#alignSelf
     * @see AlignItems
     */
    public static AlignSelfProperty alignSelf(AlignItems align) {
        return new AlignSelfProperty(align);
    }

    /**
     * Creates an align content center property.
     *
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent#CENTER
     */
    public static AlignContentProperty alignContentCenter() {
        return new AlignContentProperty(AlignContent.CENTER);
    }

    /**
     * Creates an align content flex-start property.
     *
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent#FLEX_START
     */
    public static AlignContentProperty alignContentFlexStart() {
        return new AlignContentProperty(AlignContent.FLEX_START);
    }

    /**
     * Creates an align content flex-end property.
     *
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent#FLEX_END
     */
    public static AlignContentProperty alignContentFlexEnd() {
        return new AlignContentProperty(AlignContent.FLEX_END);
    }

    /**
     * Creates an align content stretch property.
     *
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent#STRETCH
     */
    public static AlignContentProperty alignContentStretch() {
        return new AlignContentProperty(AlignContent.STRETCH);
    }

    /**
     * Creates an align content space-between property.
     *
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent#SPACE_BETWEEN
     */
    public static AlignContentProperty alignContentSpaceBetween() {
        return new AlignContentProperty(AlignContent.SPACE_BETWEEN);
    }

    /**
     * Creates an align content space-around property.
     *
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent#SPACE_AROUND
     */
    public static AlignContentProperty alignContentSpaceAround() {
        return new AlignContentProperty(AlignContent.SPACE_AROUND);
    }

    /**
     * Creates an align content property.
     *
     * @param align the alignment
     * @return an align content property
     * @see TaffyStyle#alignContent
     * @see AlignContent
     */
    public static AlignContentProperty alignContent(AlignContent align) {
        return new AlignContentProperty(align);
    }

    /**
     * Creates a justify content center property.
     *
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent#CENTER
     */
    public static JustifyContentProperty justifyCenter() {
        return new JustifyContentProperty(JustifyContent.CENTER);
    }

    /**
     * Creates a justify content flex-start property.
     *
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent#FLEX_START
     */
    public static JustifyContentProperty justifyFlexStart() {
        return new JustifyContentProperty(JustifyContent.FLEX_START);
    }

    /**
     * Creates a justify content flex-end property.
     *
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent#FLEX_END
     */
    public static JustifyContentProperty justifyFlexEnd() {
        return new JustifyContentProperty(JustifyContent.FLEX_END);
    }

    /**
     * Creates a justify content space-between property.
     *
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent#SPACE_BETWEEN
     */
    public static JustifyContentProperty justifySpaceBetween() {
        return new JustifyContentProperty(JustifyContent.SPACE_BETWEEN);
    }

    /**
     * Creates a justify content space-around property.
     *
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent#SPACE_AROUND
     */
    public static JustifyContentProperty justifySpaceAround() {
        return new JustifyContentProperty(JustifyContent.SPACE_AROUND);
    }

    /**
     * Creates a justify content space-evenly property.
     *
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent#SPACE_EVENLY
     */
    public static JustifyContentProperty justifySpaceEvenly() {
        return new JustifyContentProperty(JustifyContent.SPACE_EVENLY);
    }

    /**
     * Creates a justify content property.
     *
     * @param justify the justification
     * @return a justify content property
     * @see TaffyStyle#justifyContent
     * @see JustifyContent
     */
    public static JustifyContentProperty justifyContent(JustifyContent justify) {
        return new JustifyContentProperty(justify);
    }

    /**
     * Creates a justify items property.
     *
     * @param align the alignment
     * @return a justify items property
     * @see TaffyStyle#justifyItems
     * @see AlignItems
     */
    public static JustifyItemsProperty justifyItems(AlignItems align) {
        return new JustifyItemsProperty(align);
    }

    /**
     * Creates a justify self property.
     *
     * @param align the alignment
     * @return a justify self property
     * @see TaffyStyle#justifySelf
     * @see AlignItems
     */
    public static JustifySelfProperty justifySelf(AlignItems align) {
        return new JustifySelfProperty(align);
    }

    //endregion

    //region gap properties

    /**
     * Creates a gap property for all gutters.
     *
     * @param gap the gap size
     * @return a gap property
     * @see TaffyStyle#gap
     * @see LengthPercentage
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
     * @see TaffyStyle#gap
     * @see LengthPercentage
     */
    public static GapProperty gap(float rowGap, float columnGap) {
        return GapProperty.both(rowGap, columnGap);
    }

    /**
     * Creates a row gap property.
     *
     * @param gap the gap size
     * @return a gap property
     * @see TaffyStyle#gap
     * @see LengthPercentage
     */
    public static GapProperty rowGap(float gap) {
        return GapProperty.rowGap(gap);
    }

    /**
     * Creates a column gap property.
     *
     * @param gap the gap size
     * @return a gap property
     * @see TaffyStyle#gap
     * @see LengthPercentage
     */
    public static GapProperty columnGap(float gap) {
        return GapProperty.columnGap(gap);
    }

    //endregion

    //region position properties

    /**
     * Creates a relative position type property.
     *
     * @return a position type property
     * @see TaffyStyle#position
     * @see TaffyPosition#RELATIVE
     */
    public static PositionTypeProperty positionRelative() {
        return new PositionTypeProperty(TaffyPosition.RELATIVE);
    }

    /**
     * Creates an absolute position type property.
     *
     * @return a position type property
     * @see TaffyStyle#position
     * @see TaffyPosition#ABSOLUTE
     */
    public static PositionTypeProperty positionAbsolute() {
        return new PositionTypeProperty(TaffyPosition.ABSOLUTE);
    }

    /**
     * Creates a static position type property.
     *
     * @return a position type property
     * @see TaffyStyle#position
     * @see TaffyPosition#RELATIVE
     */
    public static PositionTypeProperty positionStatic() {
        return new PositionTypeProperty(TaffyPosition.RELATIVE);
    }

    /**
     * Creates a position type property.
     *
     * @param type the position type
     * @return a position type property
     * @see TaffyStyle#position
     * @see TaffyPosition
     */
    public static PositionTypeProperty positionType(TaffyPosition type) {
        return new PositionTypeProperty(type);
    }

    /**
     * Creates a top position edge property.
     *
     * @param value the position value
     * @return a position edge property
     * @see TaffyStyle#inset
     * @see LengthPercentageAuto
     */
    public static PositionEdgeProperty top(float value) {
        return new PositionEdgeProperty(Edge.TOP, value);
    }

    /**
     * Creates a right position edge property.
     *
     * @param value the position value
     * @return a position edge property
     * @see TaffyStyle#inset
     * @see LengthPercentageAuto
     */
    public static PositionEdgeProperty right(float value) {
        return new PositionEdgeProperty(Edge.RIGHT, value);
    }

    /**
     * Creates a bottom position edge property.
     *
     * @param value the position value
     * @return a position edge property
     * @see TaffyStyle#inset
     * @see LengthPercentageAuto
     */
    public static PositionEdgeProperty bottom(float value) {
        return new PositionEdgeProperty(Edge.BOTTOM, value);
    }

    /**
     * Creates a left position edge property.
     *
     * @param value the position value
     * @return a position edge property
     * @see TaffyStyle#inset
     * @see LengthPercentageAuto
     */
    public static PositionEdgeProperty left(float value) {
        return new PositionEdgeProperty(Edge.LEFT, value);
    }

    /**
     * Creates an aspect ratio property.
     *
     * @param ratio the aspect ratio (width/height)
     * @return an aspect ratio property
     * @see TaffyStyle#aspectRatio
     */
    public static AspectRatioProperty aspectRatio(float ratio) {
        return new AspectRatioProperty(ratio);
    }

    //endregion

    //region display properties

    /**
     * Creates a display property with the specified display mode.
     *
     * @param display the display mode
     * @return a display property
     * @see TaffyStyle#display
     * @see TaffyDisplay
     */
    public static DisplayProperty display(TaffyDisplay display) {
        return new DisplayProperty(display);
    }

    /**
     * Creates a default display property.
     *
     * @return a display property
     * @see TaffyStyle#display
     * @see TaffyDisplay#DEFAULT
     */
    public static DisplayProperty displayDefault() {
        return new DisplayProperty(TaffyDisplay.DEFAULT);
    }

    /**
     * Creates a none display property (element is not rendered).
     *
     * @return a display property
     * @see TaffyStyle#display
     * @see TaffyDisplay#NONE
     */
    public static DisplayProperty displayNone() {
        return new DisplayProperty(TaffyDisplay.NONE);
    }

    /**
     * Creates a block display property.
     *
     * @return a display property
     * @see TaffyStyle#display
     * @see TaffyDisplay#BLOCK
     */
    public static DisplayProperty displayBlock() {
        return new DisplayProperty(TaffyDisplay.BLOCK);
    }

    /**
     * Creates a flex display property.
     *
     * @return a display property
     * @see TaffyStyle#display
     * @see TaffyDisplay#FLEX
     */
    public static DisplayProperty displayFlex() {
        return new DisplayProperty(TaffyDisplay.FLEX);
    }

    /**
     * Creates a grid display property.
     *
     * @return a display property
     * @see TaffyStyle#display
     * @see TaffyDisplay#GRID
     */
    public static DisplayProperty displayGrid() {
        return new DisplayProperty(TaffyDisplay.GRID);
    }

    /**
     * Creates an item-is-table property.
     *
     * @param value whether the item is a table
     * @return an item-is-table property
     * @see TaffyStyle#itemIsTable
     */
    public static ItemIsTableProperty itemIsTable(boolean value) {
        return new ItemIsTableProperty(value);
    }

    /**
     * Creates an item-is-replaced property.
     *
     * @param value whether the item is replaced
     * @return an item-is-replaced property
     * @see TaffyStyle#itemIsReplaced
     */
    public static ItemIsReplacedProperty itemIsReplaced(boolean value) {
        return new ItemIsReplacedProperty(value);
    }

    /**
     * Creates a box sizing property.
     *
     * @param boxSizing the box sizing mode
     * @return a box sizing property
     * @see TaffyStyle#boxSizing
     * @see BoxSizing
     */
    public static BoxSizingProperty boxSizing(BoxSizing boxSizing) {
        return new BoxSizingProperty(boxSizing);
    }

    /**
     * Creates a border-box box sizing property.
     *
     * @return a box sizing property
     * @see TaffyStyle#boxSizing
     * @see BoxSizing#BORDER_BOX
     */
    public static BoxSizingProperty boxSizingBorderBox() {
        return new BoxSizingProperty(BoxSizing.BORDER_BOX);
    }

    /**
     * Creates a content-box box sizing property.
     *
     * @return a box sizing property
     * @see TaffyStyle#boxSizing
     * @see BoxSizing#CONTENT_BOX
     */
    public static BoxSizingProperty boxSizingContentBox() {
        return new BoxSizingProperty(BoxSizing.CONTENT_BOX);
    }

    /**
     * Sets overflow for both axes.
     *
     * @param overflow the overflow mode
     * @return an overflow property
     * @see TaffyStyle#overflow
     * @see Overflow
     */
    public static OverflowProperty overflow(Overflow overflow) {
        return new OverflowProperty(overflow);
    }

    /**
     * Sets overflow-x only (y is kept).
     *
     * @param overflow the overflow mode for x axis
     * @return an overflow property
     * @see TaffyStyle#overflow
     * @see Overflow
     */
    public static OverflowProperty overflowX(Overflow overflow) {
        return new OverflowProperty(overflow, null);
    }

    /**
     * Sets overflow-y only (x is kept).
     *
     * @param overflow the overflow mode for y axis
     * @return an overflow property
     * @see TaffyStyle#overflow
     * @see Overflow
     */
    public static OverflowProperty overflowY(Overflow overflow) {
        return new OverflowProperty(null, overflow);
    }

    /**
     * Creates a scrollbar width property.
     *
     * @param width the scrollbar width
     * @return a scrollbar width property
     * @see TaffyStyle#scrollbarWidth
     */
    public static ScrollbarWidthProperty scrollbarWidth(float width) {
        return new ScrollbarWidthProperty(width);
    }

    /**
     * Creates a text align property.
     *
     * @param align the text alignment
     * @return a text align property
     * @see TaffyStyle#textAlign
     * @see TextAlign
     */
    public static TextAlignProperty textAlign(TextAlign align) {
        return new TextAlignProperty(align);
    }

    /**
     * Creates a center text align property.
     *
     * @return a text align property
     * @see TaffyStyle#textAlign
     * @see TextAlign#CENTER
     */
    public static TextAlignProperty textAlignCenter() {
        return new TextAlignProperty(TextAlign.CENTER);
    }

    /**
     * Creates a left text align property.
     *
     * @return a text align property
     * @see TaffyStyle#textAlign
     * @see TextAlign#LEFT
     */
    public static TextAlignProperty textAlignLeft() {
        return new TextAlignProperty(TextAlign.LEFT);
    }

    /**
     * Creates a right text align property.
     *
     * @return a text align property
     * @see TaffyStyle#textAlign
     * @see TextAlign#RIGHT
     */
    public static TextAlignProperty textAlignRight() {
        return new TextAlignProperty(TextAlign.RIGHT);
    }

    /**
     * Creates a flex basis property.
     *
     * @param basis the flex basis dimension
     * @return a flex basis property
     * @see TaffyStyle#flexBasis
     * @see TaffyDimension
     */
    public static FlexBasisProperty flexBasis(TaffyDimension basis) {
        return new FlexBasisProperty(basis);
    }

    /**
     * Creates an auto flex basis property.
     *
     * @return a flex basis property
     * @see TaffyStyle#flexBasis
     * @see TaffyDimension#AUTO
     */
    public static FlexBasisProperty flexBasisAuto() {
        return new FlexBasisProperty(TaffyDimension.AUTO);
    }

    /**
     * Creates a flex basis property with a pixel value.
     *
     * @param px the pixel value
     * @return a flex basis property
     * @see TaffyStyle#flexBasis
     * @see TaffyDimension
     */
    public static FlexBasisProperty flexBasisPx(float px) {
        return new FlexBasisProperty(TaffyDimension.length(px));
    }

    /**
     * Creates a flex basis property with a percentage value.
     *
     * @param percent01 percentage in range [0..1], consistent with taffy Dimension.percent
     * @return a flex basis property
     * @see TaffyStyle#flexBasis
     * @see TaffyDimension
     */
    public static FlexBasisProperty flexBasisPercent(float percent01) {
        return new FlexBasisProperty(TaffyDimension.percent(percent01));
    }

    //endregion

    //region direction properties

    /**
     * Creates a direction property with the specified direction.
     *
     * @param direction the text direction
     * @return a direction property
     * @see TaffyStyle#direction
     * @see TaffyDirection
     */
    public static DirectionProperty direction(TaffyDirection direction) {
        return new DirectionProperty(direction);
    }

    /**
     * Creates a direction property that inherits from parent.
     *
     * @return a direction property with INHERIT
     * @see TaffyStyle#direction
     * @see TaffyDirection#INHERIT
     */
    public static DirectionProperty directionInherit() {
        return new DirectionProperty(TaffyDirection.INHERIT);
    }

    /**
     * Creates a left-to-right direction property.
     *
     * @return a direction property with LTR
     * @see TaffyStyle#direction
     * @see TaffyDirection#LTR
     */
    public static DirectionProperty directionLtr() {
        return new DirectionProperty(TaffyDirection.LTR);
    }

    /**
     * Creates a right-to-left direction property.
     *
     * @return a direction property with RTL
     * @see TaffyStyle#direction
     * @see TaffyDirection#RTL
     */
    public static DirectionProperty directionRtl() {
        return new DirectionProperty(TaffyDirection.RTL);
    }

    //endregion

    //region flex shorthand properties

    /**
     * Creates a flex shorthand property.
     * <p>
     * When flex >= 0: flexGrow = flex, flexShrink = 1, flexBasis = 0
     * When flex < 0: flexGrow = 0, flexShrink = -flex, flexBasis = 0
     *
     * @param flex the flex value
     * @return a flex property
     * @see TaffyStyle#flex
     */
    public static FlexProperty flex(float flex) {
        return new FlexProperty(flex);
    }

    /**
     * Clears the flex shorthand, reverting to individual flexGrow/flexShrink/flexBasis values.
     *
     * @return a flex property with NaN (disabled)
     * @see TaffyStyle#flex
     */
    public static FlexProperty flexNone() {
        return new FlexProperty(Float.NaN);
    }

    //endregion

    //region grid properties

    /**
     * Creates a grid template rows property.
     *
     * @param rows the list of track sizing functions
     * @return a grid template rows property
     * @see TaffyStyle#gridTemplateRows
     * @see TrackSizingFunction
     */
    public static GridTemplateRowsProperty gridTemplateRows(List<TrackSizingFunction> rows) {
        return new GridTemplateRowsProperty(rows);
    }

    /**
     * Creates a grid template rows property.
     *
     * @param rows the track sizing functions
     * @return a grid template rows property
     * @see TaffyStyle#gridTemplateRows
     * @see TrackSizingFunction
     */
    public static GridTemplateRowsProperty gridTemplateRows(TrackSizingFunction... rows) {
        return new GridTemplateRowsProperty(java.util.List.of(rows));
    }

    /**
     * Creates a grid template columns property.
     *
     * @param columns the list of track sizing functions
     * @return a grid template columns property
     * @see TaffyStyle#gridTemplateColumns
     * @see TrackSizingFunction
     */
    public static GridTemplateColumnsProperty gridTemplateColumns(List<TrackSizingFunction> columns) {
        return new GridTemplateColumnsProperty(columns);
    }

    /**
     * Creates a grid template columns property.
     *
     * @param columns the track sizing functions
     * @return a grid template columns property
     * @see TaffyStyle#gridTemplateColumns
     * @see TrackSizingFunction
     */
    public static GridTemplateColumnsProperty gridTemplateColumns(TrackSizingFunction... columns) {
        return new GridTemplateColumnsProperty(java.util.List.of(columns));
    }

    /**
     * Creates a grid template rows with repeat property.
     *
     * @param rows the list of grid template components
     * @return a grid template rows with repeat property
     * @see TaffyStyle#gridTemplateRowsWithRepeat
     * @see GridTemplateComponent
     */
    public static GridTemplateRowsWithRepeatProperty gridTemplateRowsWithRepeat(List<GridTemplateComponent> rows) {
        return new GridTemplateRowsWithRepeatProperty(rows);
    }

    /**
     * Creates a grid template columns with repeat property.
     *
     * @param columns the list of grid template components
     * @return a grid template columns with repeat property
     * @see TaffyStyle#gridTemplateColumnsWithRepeat
     * @see GridTemplateComponent
     */
    public static GridTemplateColumnsWithRepeatProperty gridTemplateColumnsWithRepeat(List<GridTemplateComponent> columns) {
        return new GridTemplateColumnsWithRepeatProperty(columns);
    }

    /**
     * Creates a grid auto rows property.
     *
     * @param rows the list of track sizing functions
     * @return a grid auto rows property
     * @see TaffyStyle#gridAutoRows
     * @see TrackSizingFunction
     */
    public static GridAutoRowsProperty gridAutoRows(List<TrackSizingFunction> rows) {
        return new GridAutoRowsProperty(rows);
    }

    /**
     * Creates a grid auto rows property.
     *
     * @param rows the track sizing functions
     * @return a grid auto rows property
     * @see TaffyStyle#gridAutoRows
     * @see TrackSizingFunction
     */
    public static GridAutoRowsProperty gridAutoRows(TrackSizingFunction... rows) {
        return new GridAutoRowsProperty(java.util.List.of(rows));
    }

    /**
     * Creates a grid auto columns property.
     *
     * @param columns the list of track sizing functions
     * @return a grid auto columns property
     * @see TaffyStyle#gridAutoColumns
     * @see TrackSizingFunction
     */
    public static GridAutoColumnsProperty gridAutoColumns(List<TrackSizingFunction> columns) {
        return new GridAutoColumnsProperty(columns);
    }

    /**
     * Creates a grid auto columns property.
     *
     * @param columns the track sizing functions
     * @return a grid auto columns property
     * @see TaffyStyle#gridAutoColumns
     * @see TrackSizingFunction
     */
    public static GridAutoColumnsProperty gridAutoColumns(TrackSizingFunction... columns) {
        return new GridAutoColumnsProperty(java.util.List.of(columns));
    }

    /**
     * Creates a grid auto flow property.
     *
     * @param flow the grid auto flow mode
     * @return a grid auto flow property
     * @see TaffyStyle#gridAutoFlow
     * @see GridAutoFlow
     */
    public static GridAutoFlowProperty gridAutoFlow(GridAutoFlow flow) {
        return new GridAutoFlowProperty(flow);
    }

    /**
     * Creates a grid auto flow row property.
     *
     * @return a grid auto flow property
     * @see TaffyStyle#gridAutoFlow
     * @see GridAutoFlow#ROW
     */
    public static GridAutoFlowProperty gridAutoFlowRow() {
        return new GridAutoFlowProperty(GridAutoFlow.ROW);
    }

    /**
     * Creates a grid auto flow column property.
     *
     * @return a grid auto flow property
     * @see TaffyStyle#gridAutoFlow
     * @see GridAutoFlow#COLUMN
     */
    public static GridAutoFlowProperty gridAutoFlowColumn() {
        return new GridAutoFlowProperty(GridAutoFlow.COLUMN);
    }

    /**
     * Creates a grid auto flow row dense property.
     *
     * @return a grid auto flow property
     * @see TaffyStyle#gridAutoFlow
     * @see GridAutoFlow#ROW_DENSE
     */
    public static GridAutoFlowProperty gridAutoFlowRowDense() {
        return new GridAutoFlowProperty(GridAutoFlow.ROW_DENSE);
    }

    /**
     * Creates a grid auto flow column dense property.
     *
     * @return a grid auto flow property
     * @see TaffyStyle#gridAutoFlow
     * @see GridAutoFlow#COLUMN_DENSE
     */
    public static GridAutoFlowProperty gridAutoFlowColumnDense() {
        return new GridAutoFlowProperty(GridAutoFlow.COLUMN_DENSE);
    }

    /**
     * Creates a grid row property.
     *
     * @param start the grid row start placement
     * @param end   the grid row end placement
     * @return a grid row property
     * @see TaffyStyle#gridRow
     * @see GridPlacement
     */
    public static GridRowProperty gridRow(GridPlacement start, GridPlacement end) {
        return new GridRowProperty(start, end);
    }

    /**
     * Creates a grid column property.
     *
     * @param start the grid column start placement
     * @param end   the grid column end placement
     * @return a grid column property
     * @see TaffyStyle#gridColumn
     * @see GridPlacement
     */
    public static GridColumnProperty gridColumn(GridPlacement start, GridPlacement end) {
        return new GridColumnProperty(start, end);
    }

    /**
     * Creates an auto grid placement.
     *
     * @return a grid placement
     * @see GridPlacement#auto()
     */
    public static GridPlacement gridAuto() {
        return GridPlacement.auto();
    }

    /**
     * Creates a line-based grid placement.
     *
     * @param line the line number (1-based)
     * @return a grid placement
     * @see GridPlacement#line(int)
     */
    public static GridPlacement gridLine(int line) {
        return GridPlacement.line(line);
    }

    /**
     * Creates a span-based grid placement.
     *
     * @param span the number of tracks to span
     * @return a grid placement
     * @see GridPlacement#span(int)
     */
    public static GridPlacement gridSpan(int span) {
        return GridPlacement.span(span);
    }

    /**
     * Creates a named line grid placement.
     *
     * @param name the line name
     * @return a grid placement
     * @see GridPlacement#namedLine(String)
     */
    public static GridPlacement gridNamedLine(String name) {
        return GridPlacement.namedLine(name);
    }

    /**
     * Creates a grid template areas property.
     *
     * @param areas the list of named grid areas
     * @return a grid template areas property
     * @see TaffyStyle#gridTemplateAreas
     * @see GridTemplateArea
     */
    public static GridTemplateAreasProperty gridTemplateAreas(List<GridTemplateArea> areas) {
        return new GridTemplateAreasProperty(areas);
    }

    /**
     * Creates a grid template areas property.
     *
     * @param areas the named grid areas
     * @return a grid template areas property
     * @see TaffyStyle#gridTemplateAreas
     * @see GridTemplateArea
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
     * @see GridTemplateArea
     */
    public static GridTemplateArea gridArea(String name, int rowStart, int rowEnd, int columnStart, int columnEnd) {
        return new GridTemplateArea(name, rowStart, rowEnd, columnStart, columnEnd);
    }

    /**
     * Creates a grid template column names property.
     *
     * @param columnNames the list of named grid lines
     * @return a grid template column names property
     * @see TaffyStyle#gridTemplateColumnNames
     * @see NamedGridLine
     */
    public static GridTemplateColumnNamesProperty gridTemplateColumnNames(List<NamedGridLine> columnNames) {
        return new GridTemplateColumnNamesProperty(columnNames);
    }

    /**
     * Creates a grid template column names property.
     *
     * @param columnNames the named grid lines
     * @return a grid template column names property
     * @see TaffyStyle#gridTemplateColumnNames
     * @see NamedGridLine
     */
    public static GridTemplateColumnNamesProperty gridTemplateColumnNames(NamedGridLine... columnNames) {
        return new GridTemplateColumnNamesProperty(columnNames);
    }

    /**
     * Creates a grid template row names property.
     *
     * @param rowNames the list of named grid lines
     * @return a grid template row names property
     * @see TaffyStyle#gridTemplateRowNames
     * @see NamedGridLine
     */
    public static GridTemplateRowNamesProperty gridTemplateRowNames(List<NamedGridLine> rowNames) {
        return new GridTemplateRowNamesProperty(rowNames);
    }

    /**
     * Creates a grid template row names property.
     *
     * @param rowNames the named grid lines
     * @return a grid template row names property
     * @see TaffyStyle#gridTemplateRowNames
     * @see NamedGridLine
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
     * @see NamedGridLine
     */
    public static NamedGridLine namedGridLine(String name, int index) {
        return new NamedGridLine(name, index);
    }

    //endregion

    //region icon properties

    /**
     * Creates an icon property with the specified texture.
     *
     * @param texture the icon texture
     * @return an icon property
     */
    public static IconProperty icon(VisualTexture texture) {
        return new IconProperty(texture);
    }

    //endregion

    //region shadow properties

    /**
     * Creates a shadow property with no shadow.
     *
     * @return a shadow property with no shadow
     */
    public static ShadowProperty shadowNone() {
        return ShadowProperty.none();
    }

    /**
     * Creates a shadow property with custom values.
     *
     * @param offsetX    horizontal offset
     * @param offsetY    vertical offset
     * @param blurRadius blur radius
     * @param color      shadow color (ARGB)
     * @return a shadow property
     */
    public static ShadowProperty shadow(float offsetX, float offsetY, float blurRadius, int color) {
        return new ShadowProperty(offsetX, offsetY, blurRadius, color);
    }

    /**
     * Creates a subtle shadow property (light shadow effect).
     *
     * @return a shadow property
     */
    public static ShadowProperty shadowSubtle() {
        return ShadowProperty.subtle();
    }

    /**
     * Creates a medium shadow property (moderate shadow effect).
     *
     * @return a shadow property
     */
    public static ShadowProperty shadowMedium() {
        return ShadowProperty.medium();
    }

    /**
     * Creates a strong shadow property (prominent shadow effect).
     *
     * @return a shadow property
     */
    public static ShadowProperty shadowStrong() {
        return ShadowProperty.strong();
    }

    //endregion

}
