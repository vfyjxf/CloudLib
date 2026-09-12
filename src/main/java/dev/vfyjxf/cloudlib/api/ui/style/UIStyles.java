package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValues;
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
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.Arrays;
import java.util.List;

/**
 * Factory methods for building {@link StyleValue}s — the java dsl front-end of
 * the style vocabulary.
 * <p>
 * Every factory delegates to {@code Styles.<key>.of(value)}; box factories
 * ({@code padding(...)}, {@code margin(...)}, …) return a {@link StyleValues}
 * group that flattens into the four longhand keys — the same expansion the css
 * shorthand performs.
 * <p>
 * Example usage:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
 *
 * var cardStyle = UIStyle.of(
 *     padding(4),
 *     displayFlex(),
 *     gap(6),
 *     background(NineSliceTexture.of(loc, 3))
 * );
 * widget.useStyle(cardStyle);
 * }</pre>
 */
@SuppressWarnings("unused")
public final class UIStyles {

    private UIStyles() {}

    // region length helpers

    private static LengthPercentage lp(float px) {
        return LengthPercentage.length(px);
    }

    private static LengthPercentage lpPct(float percent) {
        return LengthPercentage.percent(percent / 100f);
    }

    private static LengthPercentageAuto lpa(float px) {
        return LengthPercentageAuto.from(LengthPercentage.length(px));
    }

    private static LengthPercentageAuto lpaPct(float percent) {
        return LengthPercentageAuto.from(LengthPercentage.percent(percent / 100f));
    }

    private static TaffyDimension dim(float px) {
        return TaffyDimension.from(LengthPercentage.length(px));
    }

    private static TaffyDimension dimPct(float percent) {
        return TaffyDimension.from(LengthPercentage.percent(percent / 100f));
    }

    /** Four longhands from a css 1/2/3/4 box spec. */
    private static StyleValues box(
            StyleKey<LengthPercentage> top,
            StyleKey<LengthPercentage> right,
            StyleKey<LengthPercentage> bottom,
            StyleKey<LengthPercentage> left,
            LengthPercentage t,
            LengthPercentage r,
            LengthPercentage b,
            LengthPercentage l) {
        return StyleValues.of(top.of(t), right.of(r), bottom.of(b), left.of(l));
    }

    private static StyleValues boxAuto(
            StyleKey<LengthPercentageAuto> top,
            StyleKey<LengthPercentageAuto> right,
            StyleKey<LengthPercentageAuto> bottom,
            StyleKey<LengthPercentageAuto> left,
            LengthPercentageAuto t,
            LengthPercentageAuto r,
            LengthPercentageAuto b,
            LengthPercentageAuto l) {
        return StyleValues.of(top.of(t), right.of(r), bottom.of(b), left.of(l));
    }

    // endregion

    // region padding

    public static StyleValues padding(LengthPercentage all) {
        return box(
                Styles.paddingTop, Styles.paddingRight, Styles.paddingBottom, Styles.paddingLeft, all, all, all, all);
    }

    public static StyleValues padding(LengthPercentage vertical, LengthPercentage horizontal) {
        return box(
                Styles.paddingTop,
                Styles.paddingRight,
                Styles.paddingBottom,
                Styles.paddingLeft,
                vertical,
                horizontal,
                vertical,
                horizontal);
    }

    public static StyleValues padding(
            LengthPercentage top, LengthPercentage right, LengthPercentage bottom, LengthPercentage left) {
        return box(
                Styles.paddingTop,
                Styles.paddingRight,
                Styles.paddingBottom,
                Styles.paddingLeft,
                top,
                right,
                bottom,
                left);
    }

    public static StyleValues padding(float all) {
        return padding(lp(all));
    }

    public static StyleValues padding(float vertical, float horizontal) {
        return padding(lp(vertical), lp(horizontal));
    }

    public static StyleValues padding(float top, float right, float bottom, float left) {
        return padding(lp(top), lp(right), lp(bottom), lp(left));
    }

    public static StyleValues paddingPercent(float percent) {
        return padding(lpPct(percent));
    }

    public static StyleValues paddingPercent(float vertical, float horizontal) {
        return padding(lpPct(vertical), lpPct(horizontal));
    }

    public static StyleValue<LengthPercentage> paddingTop(float value) {
        return Styles.paddingTop.of(lp(value));
    }

    public static StyleValue<LengthPercentage> paddingTop(LengthPercentage value) {
        return Styles.paddingTop.of(value);
    }

    public static StyleValue<LengthPercentage> paddingRight(float value) {
        return Styles.paddingRight.of(lp(value));
    }

    public static StyleValue<LengthPercentage> paddingRight(LengthPercentage value) {
        return Styles.paddingRight.of(value);
    }

    public static StyleValue<LengthPercentage> paddingBottom(float value) {
        return Styles.paddingBottom.of(lp(value));
    }

    public static StyleValue<LengthPercentage> paddingBottom(LengthPercentage value) {
        return Styles.paddingBottom.of(value);
    }

    public static StyleValue<LengthPercentage> paddingLeft(float value) {
        return Styles.paddingLeft.of(lp(value));
    }

    public static StyleValue<LengthPercentage> paddingLeft(LengthPercentage value) {
        return Styles.paddingLeft.of(value);
    }

    public static StyleValues paddingHorizontal(float value) {
        return StyleValues.of(Styles.paddingLeft.of(lp(value)), Styles.paddingRight.of(lp(value)));
    }

    public static StyleValues paddingHorizontal(LengthPercentage value) {
        return StyleValues.of(Styles.paddingLeft.of(value), Styles.paddingRight.of(value));
    }

    public static StyleValues paddingVertical(float value) {
        return StyleValues.of(Styles.paddingTop.of(lp(value)), Styles.paddingBottom.of(lp(value)));
    }

    public static StyleValues paddingVertical(LengthPercentage value) {
        return StyleValues.of(Styles.paddingTop.of(value), Styles.paddingBottom.of(value));
    }

    // endregion

    // region margin

    public static StyleValues margin(LengthPercentageAuto all) {
        return boxAuto(
                Styles.marginTop, Styles.marginRight, Styles.marginBottom, Styles.marginLeft, all, all, all, all);
    }

    public static StyleValues margin(LengthPercentageAuto vertical, LengthPercentageAuto horizontal) {
        return boxAuto(
                Styles.marginTop,
                Styles.marginRight,
                Styles.marginBottom,
                Styles.marginLeft,
                vertical,
                horizontal,
                vertical,
                horizontal);
    }

    public static StyleValues margin(
            LengthPercentageAuto top,
            LengthPercentageAuto right,
            LengthPercentageAuto bottom,
            LengthPercentageAuto left) {
        return boxAuto(
                Styles.marginTop, Styles.marginRight, Styles.marginBottom, Styles.marginLeft, top, right, bottom, left);
    }

    public static StyleValues margin(float all) {
        return margin(lpa(all));
    }

    public static StyleValues margin(float vertical, float horizontal) {
        return margin(lpa(vertical), lpa(horizontal));
    }

    public static StyleValues margin(float top, float right, float bottom, float left) {
        return margin(lpa(top), lpa(right), lpa(bottom), lpa(left));
    }

    public static StyleValues marginAuto() {
        return margin(LengthPercentageAuto.AUTO);
    }

    public static StyleValues marginAutoHorizontal() {
        return StyleValues.of(
                Styles.marginLeft.of(LengthPercentageAuto.AUTO), Styles.marginRight.of(LengthPercentageAuto.AUTO));
    }

    public static StyleValues marginAutoVertical() {
        return StyleValues.of(
                Styles.marginTop.of(LengthPercentageAuto.AUTO), Styles.marginBottom.of(LengthPercentageAuto.AUTO));
    }

    public static StyleValues marginPercent(float percent) {
        return margin(lpaPct(percent));
    }

    public static StyleValues marginPercent(float vertical, float horizontal) {
        return margin(lpaPct(vertical), lpaPct(horizontal));
    }

    public static StyleValue<LengthPercentageAuto> marginTop(float value) {
        return Styles.marginTop.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> marginTop(LengthPercentageAuto value) {
        return Styles.marginTop.of(value);
    }

    public static StyleValue<LengthPercentageAuto> marginRight(float value) {
        return Styles.marginRight.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> marginRight(LengthPercentageAuto value) {
        return Styles.marginRight.of(value);
    }

    public static StyleValue<LengthPercentageAuto> marginBottom(float value) {
        return Styles.marginBottom.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> marginBottom(LengthPercentageAuto value) {
        return Styles.marginBottom.of(value);
    }

    public static StyleValue<LengthPercentageAuto> marginLeft(float value) {
        return Styles.marginLeft.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> marginLeft(LengthPercentageAuto value) {
        return Styles.marginLeft.of(value);
    }

    public static StyleValues marginHorizontal(float value) {
        return StyleValues.of(Styles.marginLeft.of(lpa(value)), Styles.marginRight.of(lpa(value)));
    }

    public static StyleValues marginHorizontal(LengthPercentageAuto value) {
        return StyleValues.of(Styles.marginLeft.of(value), Styles.marginRight.of(value));
    }

    public static StyleValues marginVertical(float value) {
        return StyleValues.of(Styles.marginTop.of(lpa(value)), Styles.marginBottom.of(lpa(value)));
    }

    public static StyleValues marginVertical(LengthPercentageAuto value) {
        return StyleValues.of(Styles.marginTop.of(value), Styles.marginBottom.of(value));
    }

    // endregion

    // region border (taffy edge widths)

    public static StyleValues border(LengthPercentage all) {
        return box(
                Styles.borderTopWidth,
                Styles.borderRightWidth,
                Styles.borderBottomWidth,
                Styles.borderLeftWidth,
                all,
                all,
                all,
                all);
    }

    public static StyleValues border(LengthPercentage vertical, LengthPercentage horizontal) {
        return box(
                Styles.borderTopWidth,
                Styles.borderRightWidth,
                Styles.borderBottomWidth,
                Styles.borderLeftWidth,
                vertical,
                horizontal,
                vertical,
                horizontal);
    }

    public static StyleValues border(
            LengthPercentage top, LengthPercentage right, LengthPercentage bottom, LengthPercentage left) {
        return box(
                Styles.borderTopWidth,
                Styles.borderRightWidth,
                Styles.borderBottomWidth,
                Styles.borderLeftWidth,
                top,
                right,
                bottom,
                left);
    }

    public static StyleValues border(float all) {
        return border(lp(all));
    }

    public static StyleValues border(float vertical, float horizontal) {
        return border(lp(vertical), lp(horizontal));
    }

    public static StyleValues border(float top, float right, float bottom, float left) {
        return border(lp(top), lp(right), lp(bottom), lp(left));
    }

    public static StyleValue<LengthPercentage> borderTop(float value) {
        return Styles.borderTopWidth.of(lp(value));
    }

    public static StyleValue<LengthPercentage> borderTop(LengthPercentage value) {
        return Styles.borderTopWidth.of(value);
    }

    public static StyleValue<LengthPercentage> borderRight(float value) {
        return Styles.borderRightWidth.of(lp(value));
    }

    public static StyleValue<LengthPercentage> borderRight(LengthPercentage value) {
        return Styles.borderRightWidth.of(value);
    }

    public static StyleValue<LengthPercentage> borderBottom(float value) {
        return Styles.borderBottomWidth.of(lp(value));
    }

    public static StyleValue<LengthPercentage> borderBottom(LengthPercentage value) {
        return Styles.borderBottomWidth.of(value);
    }

    public static StyleValue<LengthPercentage> borderLeft(float value) {
        return Styles.borderLeftWidth.of(lp(value));
    }

    public static StyleValue<LengthPercentage> borderLeft(LengthPercentage value) {
        return Styles.borderLeftWidth.of(value);
    }

    public static StyleValues borderHorizontal(float value) {
        return StyleValues.of(Styles.borderLeftWidth.of(lp(value)), Styles.borderRightWidth.of(lp(value)));
    }

    public static StyleValues borderHorizontal(LengthPercentage value) {
        return StyleValues.of(Styles.borderLeftWidth.of(value), Styles.borderRightWidth.of(value));
    }

    public static StyleValues borderVertical(float value) {
        return StyleValues.of(Styles.borderTopWidth.of(lp(value)), Styles.borderBottomWidth.of(lp(value)));
    }

    public static StyleValues borderVertical(LengthPercentage value) {
        return StyleValues.of(Styles.borderTopWidth.of(value), Styles.borderBottomWidth.of(value));
    }

    public static StyleValues borderPercent(float percent) {
        return border(lpPct(percent));
    }

    public static StyleValues borderPercent(float vertical, float horizontal) {
        return border(lpPct(vertical), lpPct(horizontal));
    }

    /** Visual border color — the drawn outline, not the taffy layout rect. */
    public static StyleValue<Integer> borderColor(int argb) {
        return Styles.borderColor.of(argb);
    }

    /** Visual border thickness — the drawn outline width. */
    public static StyleValue<Float> borderThickness(float width) {
        return Styles.borderThickness.of(width);
    }

    // endregion

    // region visual — background / icon / text / depth

    public static StyleValue<VisualTexture> background(VisualTexture texture) {
        return Styles.background.of(texture);
    }

    public static StyleValue<Integer> backgroundColor(int argb) {
        return Styles.backgroundColor.of(argb);
    }

    public static StyleValue<VisualTexture> icon(VisualTexture texture) {
        return Styles.icon.of(texture);
    }

    public static StyleValue<Integer> textColor(int color) {
        return Styles.color.of(color);
    }

    public static StyleValue<Float> opacity(float opacity) {
        return Styles.opacity.of(opacity);
    }

    public static StyleValue<Integer> zIndex(int zIndex) {
        return Styles.zIndex.of(zIndex);
    }

    public static StyleValue<SceneLayer> sceneLayer(SceneLayer layer) {
        return Styles.sceneLayer.of(layer);
    }

    public static StyleValue<ScrollbarStyleData> scrollbarStyle(VisualTexture track, VisualTexture thumb) {
        return Styles.scrollbarStyle.of(new ScrollbarStyleData(track, thumb, -1, -1));
    }

    public static StyleValue<ScrollbarStyleData> scrollbarStyle(VisualTexture track, VisualTexture thumb, int width) {
        return Styles.scrollbarStyle.of(new ScrollbarStyleData(track, thumb, width, -1));
    }

    public static StyleValue<ScrollbarStyleData> scrollbarStyle(
            VisualTexture track, VisualTexture thumb, int width, int minThumbSize) {
        return Styles.scrollbarStyle.of(new ScrollbarStyleData(track, thumb, width, minThumbSize));
    }

    public static StyleValue<VisualTexture> borderTexture(VisualTexture texture) {
        return Styles.borderTexture.of(texture);
    }

    public static StyleValue<Shadow> shadowNone() {
        return Styles.boxShadow.of(Shadow.none);
    }

    public static StyleValue<Shadow> shadow(float offsetX, float offsetY, float blurRadius, int color) {
        return Styles.boxShadow.of(new Shadow(offsetX, offsetY, blurRadius, color));
    }

    public static StyleValue<Shadow> shadowSubtle() {
        return shadow(0, 1, 2, 0x40000000);
    }

    public static StyleValue<Shadow> shadowMedium() {
        return shadow(0, 2, 4, 0x60000000);
    }

    public static StyleValue<Shadow> shadowStrong() {
        return shadow(0, 4, 8, 0x80000000);
    }

    // endregion

    // region size

    public static StyleValues sizeOf(TaffyDimension width, TaffyDimension height) {
        return StyleValues.of(Styles.width.of(width), Styles.height.of(height));
    }

    public static StyleValues sizeOf(TaffyDimension size) {
        return sizeOf(size, size);
    }

    public static StyleValues sizeOf(float size) {
        return sizeOf(dim(size));
    }

    public static StyleValues sizeOf(float width, float height) {
        return sizeOf(dim(width), dim(height));
    }

    public static StyleValue<TaffyDimension> widthOf(float width) {
        return Styles.width.of(dim(width));
    }

    public static StyleValue<TaffyDimension> widthOf(TaffyDimension width) {
        return Styles.width.of(width);
    }

    public static StyleValue<TaffyDimension> heightOf(float height) {
        return Styles.height.of(dim(height));
    }

    public static StyleValue<TaffyDimension> heightOf(TaffyDimension height) {
        return Styles.height.of(height);
    }

    public static StyleValues sizePercent(float percent) {
        return sizeOf(dimPct(percent));
    }

    public static StyleValues sizePercent(float width, float height) {
        return sizeOf(dimPct(width), dimPct(height));
    }

    public static StyleValues sizeFull() {
        return sizeOf(TaffyDimension.from(LengthPercentage.percent(1.0f)));
    }

    public static StyleValues sizeAuto() {
        return sizeOf(TaffyDimension.AUTO);
    }

    public static StyleValues sizeStretch() {
        return sizeOf(TaffyDimension.STRETCH);
    }

    public static StyleValues sizeFitContent() {
        return sizeOf(TaffyDimension.FIT_CONTENT);
    }

    public static StyleValue<TaffyDimension> minWidth(TaffyDimension value) {
        return Styles.minWidth.of(value);
    }

    public static StyleValue<TaffyDimension> minWidth(float value) {
        return Styles.minWidth.of(dim(value));
    }

    public static StyleValue<TaffyDimension> minHeight(TaffyDimension value) {
        return Styles.minHeight.of(value);
    }

    public static StyleValue<TaffyDimension> minHeight(float value) {
        return Styles.minHeight.of(dim(value));
    }

    public static StyleValue<TaffyDimension> maxWidth(TaffyDimension value) {
        return Styles.maxWidth.of(value);
    }

    public static StyleValue<TaffyDimension> maxWidth(float value) {
        return Styles.maxWidth.of(dim(value));
    }

    public static StyleValue<TaffyDimension> maxHeight(TaffyDimension value) {
        return Styles.maxHeight.of(value);
    }

    public static StyleValue<TaffyDimension> maxHeight(float value) {
        return Styles.maxHeight.of(dim(value));
    }

    public static StyleValues minSize(TaffyDimension width, TaffyDimension height) {
        return StyleValues.of(Styles.minWidth.of(width), Styles.minHeight.of(height));
    }

    public static StyleValues minSize(float width, float height) {
        return minSize(dim(width), dim(height));
    }

    public static StyleValues maxSize(TaffyDimension width, TaffyDimension height) {
        return StyleValues.of(Styles.maxWidth.of(width), Styles.maxHeight.of(height));
    }

    public static StyleValues maxSize(float width, float height) {
        return maxSize(dim(width), dim(height));
    }

    public static StyleValue<TaffyDimension> minWidthPercent(float percent) {
        return Styles.minWidth.of(dimPct(percent));
    }

    public static StyleValue<TaffyDimension> minHeightPercent(float percent) {
        return Styles.minHeight.of(dimPct(percent));
    }

    public static StyleValue<TaffyDimension> maxWidthPercent(float percent) {
        return Styles.maxWidth.of(dimPct(percent));
    }

    public static StyleValue<TaffyDimension> maxHeightPercent(float percent) {
        return Styles.maxHeight.of(dimPct(percent));
    }

    public static StyleValue<Float> aspectRatio(float ratio) {
        return Styles.aspectRatio.of(ratio);
    }

    // endregion

    // region flex container

    public static StyleValue<FlexDirection> flexRow() {
        return Styles.flexDirection.of(FlexDirection.ROW);
    }

    public static StyleValue<FlexDirection> flexColumn() {
        return Styles.flexDirection.of(FlexDirection.COLUMN);
    }

    public static StyleValue<FlexDirection> flexRowReverse() {
        return Styles.flexDirection.of(FlexDirection.ROW_REVERSE);
    }

    public static StyleValue<FlexDirection> flexColumnReverse() {
        return Styles.flexDirection.of(FlexDirection.COLUMN_REVERSE);
    }

    public static StyleValue<FlexDirection> flexDirection(FlexDirection direction) {
        return Styles.flexDirection.of(direction);
    }

    public static StyleValue<Float> flexGrow(float grow) {
        return Styles.flexGrow.of(grow);
    }

    public static StyleValue<Float> flexShrink(float shrink) {
        return Styles.flexShrink.of(shrink);
    }

    public static StyleValue<FlexWrap> flexWrap() {
        return Styles.flexWrap.of(FlexWrap.WRAP);
    }

    public static StyleValue<FlexWrap> flexNoWrap() {
        return Styles.flexWrap.of(FlexWrap.NO_WRAP);
    }

    public static StyleValue<FlexWrap> flexWrapReverse() {
        return Styles.flexWrap.of(FlexWrap.WRAP_REVERSE);
    }

    public static StyleValue<TaffyDimension> flexBasis(TaffyDimension basis) {
        return Styles.flexBasis.of(basis);
    }

    public static StyleValue<TaffyDimension> flexBasisAuto() {
        return Styles.flexBasis.of(TaffyDimension.AUTO);
    }

    public static StyleValue<TaffyDimension> flexBasisPx(float px) {
        return Styles.flexBasis.of(dim(px));
    }

    public static StyleValue<TaffyDimension> flexBasisPercent(float percent) {
        return Styles.flexBasis.of(dimPct(percent));
    }

    /** css {@code flex: <grow>} shorthand → grow + shrink. */
    public static StyleValues flex(float grow) {
        return StyleValues.of(Styles.flexGrow.of(grow), Styles.flexShrink.of(1f));
    }

    public static StyleValues flexNone() {
        return StyleValues.of(Styles.flexGrow.of(0f), Styles.flexShrink.of(0f));
    }

    // endregion

    // region alignment

    public static StyleValue<AlignItems> alignItemsCenter() {
        return Styles.alignItems.of(AlignItems.CENTER);
    }

    public static StyleValue<AlignItems> alignItemsFlexStart() {
        return Styles.alignItems.of(AlignItems.FLEX_START);
    }

    public static StyleValue<AlignItems> alignItemsFlexEnd() {
        return Styles.alignItems.of(AlignItems.FLEX_END);
    }

    public static StyleValue<AlignItems> alignItemsStretch() {
        return Styles.alignItems.of(AlignItems.STRETCH);
    }

    public static StyleValue<AlignItems> alignItemsBaseline() {
        return Styles.alignItems.of(AlignItems.BASELINE);
    }

    public static StyleValue<AlignItems> alignItems(AlignItems align) {
        return Styles.alignItems.of(align);
    }

    public static StyleValue<AlignItems> alignSelfCenter() {
        return Styles.alignSelf.of(AlignItems.CENTER);
    }

    public static StyleValue<AlignItems> alignSelfFlexStart() {
        return Styles.alignSelf.of(AlignItems.FLEX_START);
    }

    public static StyleValue<AlignItems> alignSelfFlexEnd() {
        return Styles.alignSelf.of(AlignItems.FLEX_END);
    }

    public static StyleValue<AlignItems> alignSelfStretch() {
        return Styles.alignSelf.of(AlignItems.STRETCH);
    }

    public static StyleValue<AlignItems> alignSelf(AlignItems align) {
        return Styles.alignSelf.of(align);
    }

    public static StyleValue<AlignContent> alignContentCenter() {
        return Styles.alignContent.of(AlignContent.CENTER);
    }

    public static StyleValue<AlignContent> alignContentFlexStart() {
        return Styles.alignContent.of(AlignContent.FLEX_START);
    }

    public static StyleValue<AlignContent> alignContentFlexEnd() {
        return Styles.alignContent.of(AlignContent.FLEX_END);
    }

    public static StyleValue<AlignContent> alignContentStretch() {
        return Styles.alignContent.of(AlignContent.STRETCH);
    }

    public static StyleValue<AlignContent> alignContentSpaceBetween() {
        return Styles.alignContent.of(AlignContent.SPACE_BETWEEN);
    }

    public static StyleValue<AlignContent> alignContentSpaceAround() {
        return Styles.alignContent.of(AlignContent.SPACE_AROUND);
    }

    public static StyleValue<AlignContent> alignContent(AlignContent align) {
        return Styles.alignContent.of(align);
    }

    public static StyleValue<JustifyContent> justifyCenter() {
        return Styles.justifyContent.of(JustifyContent.CENTER);
    }

    public static StyleValue<JustifyContent> justifyFlexStart() {
        return Styles.justifyContent.of(JustifyContent.FLEX_START);
    }

    public static StyleValue<JustifyContent> justifyFlexEnd() {
        return Styles.justifyContent.of(JustifyContent.FLEX_END);
    }

    public static StyleValue<JustifyContent> justifySpaceBetween() {
        return Styles.justifyContent.of(JustifyContent.SPACE_BETWEEN);
    }

    public static StyleValue<JustifyContent> justifySpaceAround() {
        return Styles.justifyContent.of(JustifyContent.SPACE_AROUND);
    }

    public static StyleValue<JustifyContent> justifySpaceEvenly() {
        return Styles.justifyContent.of(JustifyContent.SPACE_EVENLY);
    }

    public static StyleValue<JustifyContent> justifyContent(JustifyContent justify) {
        return Styles.justifyContent.of(justify);
    }

    public static StyleValue<AlignItems> justifyItems(AlignItems align) {
        return Styles.justifyItems.of(align);
    }

    public static StyleValue<AlignItems> justifySelf(AlignItems align) {
        return Styles.justifySelf.of(align);
    }

    public static StyleValue<TextAlign> textAlign(TextAlign align) {
        return Styles.textAlign.of(align);
    }

    public static StyleValue<TextAlign> textAlignCenter() {
        return Styles.textAlign.of(TextAlign.CENTER);
    }

    public static StyleValue<TextAlign> textAlignLeft() {
        return Styles.textAlign.of(TextAlign.LEFT);
    }

    public static StyleValue<TextAlign> textAlignRight() {
        return Styles.textAlign.of(TextAlign.RIGHT);
    }

    // endregion

    // region gap

    public static StyleValues gap(float gap) {
        return gap(lp(gap));
    }

    public static StyleValues gap(float rowGap, float columnGap) {
        return gap(lp(rowGap), lp(columnGap));
    }

    public static StyleValues gap(LengthPercentage gap) {
        return StyleValues.of(Styles.rowGap.of(gap), Styles.columnGap.of(gap));
    }

    public static StyleValues gap(LengthPercentage rowGap, LengthPercentage columnGap) {
        return StyleValues.of(Styles.rowGap.of(rowGap), Styles.columnGap.of(columnGap));
    }

    public static StyleValue<LengthPercentage> rowGap(float gap) {
        return Styles.rowGap.of(lp(gap));
    }

    public static StyleValue<LengthPercentage> columnGap(float gap) {
        return Styles.columnGap.of(lp(gap));
    }

    public static StyleValue<LengthPercentage> rowGap(LengthPercentage gap) {
        return Styles.rowGap.of(gap);
    }

    public static StyleValue<LengthPercentage> columnGap(LengthPercentage gap) {
        return Styles.columnGap.of(gap);
    }

    // endregion

    // region position & inset

    public static StyleValue<TaffyPosition> positionRelative() {
        return Styles.position.of(TaffyPosition.RELATIVE);
    }

    public static StyleValue<TaffyPosition> positionAbsolute() {
        return Styles.position.of(TaffyPosition.ABSOLUTE);
    }

    public static StyleValue<TaffyPosition> positionType(TaffyPosition type) {
        return Styles.position.of(type);
    }

    public static StyleValues inset(float all) {
        return inset(lpa(all));
    }

    public static StyleValues inset(float vertical, float horizontal) {
        return inset(lpa(vertical), lpa(horizontal));
    }

    public static StyleValues inset(float top, float right, float bottom, float left) {
        return inset(lpa(top), lpa(right), lpa(bottom), lpa(left));
    }

    public static StyleValues inset(LengthPercentageAuto all) {
        return boxAuto(Styles.insetTop, Styles.insetRight, Styles.insetBottom, Styles.insetLeft, all, all, all, all);
    }

    public static StyleValues inset(LengthPercentageAuto vertical, LengthPercentageAuto horizontal) {
        return boxAuto(
                Styles.insetTop,
                Styles.insetRight,
                Styles.insetBottom,
                Styles.insetLeft,
                vertical,
                horizontal,
                vertical,
                horizontal);
    }

    public static StyleValues inset(
            LengthPercentageAuto top,
            LengthPercentageAuto right,
            LengthPercentageAuto bottom,
            LengthPercentageAuto left) {
        return boxAuto(
                Styles.insetTop, Styles.insetRight, Styles.insetBottom, Styles.insetLeft, top, right, bottom, left);
    }

    public static StyleValue<LengthPercentageAuto> insetTop(float value) {
        return Styles.insetTop.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> insetTop(LengthPercentageAuto value) {
        return Styles.insetTop.of(value);
    }

    public static StyleValue<LengthPercentageAuto> insetTopPercent(float percent) {
        return Styles.insetTop.of(lpaPct(percent));
    }

    public static StyleValue<LengthPercentageAuto> insetTopAuto() {
        return Styles.insetTop.of(LengthPercentageAuto.AUTO);
    }

    public static StyleValue<LengthPercentageAuto> insetRight(float value) {
        return Styles.insetRight.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> insetRight(LengthPercentageAuto value) {
        return Styles.insetRight.of(value);
    }

    public static StyleValue<LengthPercentageAuto> insetRightPercent(float percent) {
        return Styles.insetRight.of(lpaPct(percent));
    }

    public static StyleValue<LengthPercentageAuto> insetRightAuto() {
        return Styles.insetRight.of(LengthPercentageAuto.AUTO);
    }

    public static StyleValue<LengthPercentageAuto> insetBottom(float value) {
        return Styles.insetBottom.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> insetBottom(LengthPercentageAuto value) {
        return Styles.insetBottom.of(value);
    }

    public static StyleValue<LengthPercentageAuto> insetBottomPercent(float percent) {
        return Styles.insetBottom.of(lpaPct(percent));
    }

    public static StyleValue<LengthPercentageAuto> insetBottomAuto() {
        return Styles.insetBottom.of(LengthPercentageAuto.AUTO);
    }

    public static StyleValue<LengthPercentageAuto> insetLeft(float value) {
        return Styles.insetLeft.of(lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> insetLeft(LengthPercentageAuto value) {
        return Styles.insetLeft.of(value);
    }

    public static StyleValue<LengthPercentageAuto> insetLeftPercent(float percent) {
        return Styles.insetLeft.of(lpaPct(percent));
    }

    public static StyleValue<LengthPercentageAuto> insetLeftAuto() {
        return Styles.insetLeft.of(LengthPercentageAuto.AUTO);
    }

    public static StyleValue<LengthPercentageAuto> insetEdge(Edge edge, float value) {
        return insetEdge(edge, lpa(value));
    }

    public static StyleValue<LengthPercentageAuto> insetEdge(Edge edge, LengthPercentageAuto value) {
        return switch (edge) {
            case top -> Styles.insetTop.of(value);
            case right -> Styles.insetRight.of(value);
            case bottom -> Styles.insetBottom.of(value);
            case left -> Styles.insetLeft.of(value);
        };
    }

    public static StyleValues insetHorizontal(float value) {
        return StyleValues.of(Styles.insetLeft.of(lpa(value)), Styles.insetRight.of(lpa(value)));
    }

    public static StyleValues insetHorizontal(LengthPercentageAuto value) {
        return StyleValues.of(Styles.insetLeft.of(value), Styles.insetRight.of(value));
    }

    public static StyleValues insetVertical(float value) {
        return StyleValues.of(Styles.insetTop.of(lpa(value)), Styles.insetBottom.of(lpa(value)));
    }

    public static StyleValues insetVertical(LengthPercentageAuto value) {
        return StyleValues.of(Styles.insetTop.of(value), Styles.insetBottom.of(value));
    }

    public static StyleValues insetAuto() {
        return inset(LengthPercentageAuto.AUTO);
    }

    public static StyleValues insetZero() {
        return inset(lpa(0));
    }

    // endregion

    // region box properties

    public static StyleValue<TaffyDisplay> display(TaffyDisplay display) {
        return Styles.display.of(display);
    }

    public static StyleValue<TaffyDisplay> displayDefault() {
        return Styles.display.of(TaffyDisplay.DEFAULT);
    }

    public static StyleValue<TaffyDisplay> displayNone() {
        return Styles.display.of(TaffyDisplay.NONE);
    }

    public static StyleValue<TaffyDisplay> displayBlock() {
        return Styles.display.of(TaffyDisplay.BLOCK);
    }

    public static StyleValue<TaffyDisplay> displayFlex() {
        return Styles.display.of(TaffyDisplay.FLEX);
    }

    public static StyleValue<TaffyDisplay> displayGrid() {
        return Styles.display.of(TaffyDisplay.GRID);
    }

    public static StyleValue<Boolean> itemIsTable(boolean value) {
        return Styles.itemIsTable.of(value);
    }

    public static StyleValue<Boolean> itemIsReplaced(boolean value) {
        return Styles.itemIsReplaced.of(value);
    }

    public static StyleValue<BoxSizing> boxSizing(BoxSizing boxSizing) {
        return Styles.boxSizing.of(boxSizing);
    }

    public static StyleValue<BoxSizing> boxSizingBorderBox() {
        return Styles.boxSizing.of(BoxSizing.BORDER_BOX);
    }

    public static StyleValue<BoxSizing> boxSizingContentBox() {
        return Styles.boxSizing.of(BoxSizing.CONTENT_BOX);
    }

    public static StyleValues overflow(Overflow overflow) {
        return StyleValues.of(Styles.overflowX.of(overflow), Styles.overflowY.of(overflow));
    }

    public static StyleValue<Overflow> overflowX(Overflow overflow) {
        return Styles.overflowX.of(overflow);
    }

    public static StyleValue<Overflow> overflowY(Overflow overflow) {
        return Styles.overflowY.of(overflow);
    }

    public static StyleValue<Float> scrollbarWidth(float width) {
        return Styles.scrollbarWidth.of(width);
    }

    public static StyleValue<TaffyDirection> direction(TaffyDirection direction) {
        return Styles.direction.of(direction);
    }

    public static StyleValue<TaffyDirection> directionInherit() {
        return Styles.direction.of(TaffyDirection.INHERIT);
    }

    public static StyleValue<TaffyDirection> directionLtr() {
        return Styles.direction.of(TaffyDirection.LTR);
    }

    public static StyleValue<TaffyDirection> directionRtl() {
        return Styles.direction.of(TaffyDirection.RTL);
    }

    // endregion

    // region grid

    public static StyleValue<List<TrackSizingFunction>> gridTemplateRows(List<TrackSizingFunction> rows) {
        return Styles.gridTemplateRows.of(rows);
    }

    public static StyleValue<List<TrackSizingFunction>> gridTemplateRows(TrackSizingFunction... rows) {
        return Styles.gridTemplateRows.of(Arrays.asList(rows));
    }

    public static StyleValue<List<TrackSizingFunction>> gridTemplateColumns(List<TrackSizingFunction> columns) {
        return Styles.gridTemplateColumns.of(columns);
    }

    public static StyleValue<List<TrackSizingFunction>> gridTemplateColumns(TrackSizingFunction... columns) {
        return Styles.gridTemplateColumns.of(Arrays.asList(columns));
    }

    public static StyleValue<List<GridTemplateComponent>> gridTemplateRowsWithRepeat(List<GridTemplateComponent> rows) {
        return Styles.gridTemplateRowsWithRepeat.of(rows);
    }

    public static StyleValue<List<GridTemplateComponent>> gridTemplateColumnsWithRepeat(
            List<GridTemplateComponent> columns) {
        return Styles.gridTemplateColumnsWithRepeat.of(columns);
    }

    public static StyleValue<List<TrackSizingFunction>> gridAutoRows(List<TrackSizingFunction> rows) {
        return Styles.gridAutoRows.of(rows);
    }

    public static StyleValue<List<TrackSizingFunction>> gridAutoRows(TrackSizingFunction... rows) {
        return Styles.gridAutoRows.of(Arrays.asList(rows));
    }

    public static StyleValue<List<TrackSizingFunction>> gridAutoColumns(List<TrackSizingFunction> columns) {
        return Styles.gridAutoColumns.of(columns);
    }

    public static StyleValue<List<TrackSizingFunction>> gridAutoColumns(TrackSizingFunction... columns) {
        return Styles.gridAutoColumns.of(Arrays.asList(columns));
    }

    public static StyleValue<GridAutoFlow> gridAutoFlow(GridAutoFlow flow) {
        return Styles.gridAutoFlow.of(flow);
    }

    public static StyleValue<GridAutoFlow> gridAutoFlowRow() {
        return Styles.gridAutoFlow.of(GridAutoFlow.ROW);
    }

    public static StyleValue<GridAutoFlow> gridAutoFlowColumn() {
        return Styles.gridAutoFlow.of(GridAutoFlow.COLUMN);
    }

    public static StyleValue<GridAutoFlow> gridAutoFlowRowDense() {
        return Styles.gridAutoFlow.of(GridAutoFlow.ROW_DENSE);
    }

    public static StyleValue<GridAutoFlow> gridAutoFlowColumnDense() {
        return Styles.gridAutoFlow.of(GridAutoFlow.COLUMN_DENSE);
    }

    /** {@code grid-row: <start> [/ <end>]} */
    public static StyleValues gridRow(GridPlacement start, GridPlacement end) {
        return StyleValues.of(Styles.gridRowStart.of(start), Styles.gridRowEnd.of(end));
    }

    /** {@code grid-column: <start> [/ <end>]} */
    public static StyleValues gridColumn(GridPlacement start, GridPlacement end) {
        return StyleValues.of(Styles.gridColumnStart.of(start), Styles.gridColumnEnd.of(end));
    }

    public static StyleValue<List<GridTemplateArea>> gridTemplateAreas(List<GridTemplateArea> areas) {
        return Styles.gridTemplateAreas.of(areas);
    }

    public static StyleValue<List<GridTemplateArea>> gridTemplateAreas(GridTemplateArea... areas) {
        return Styles.gridTemplateAreas.of(Arrays.asList(areas));
    }

    public static GridTemplateArea gridArea(String name, int rowStart, int rowEnd, int columnStart, int columnEnd) {
        return new GridTemplateArea(name, rowStart, rowEnd, columnStart, columnEnd);
    }

    public static StyleValue<List<NamedGridLine>> gridTemplateColumnNames(List<NamedGridLine> columnNames) {
        return Styles.gridTemplateColumnNames.of(columnNames);
    }

    public static StyleValue<List<NamedGridLine>> gridTemplateColumnNames(NamedGridLine... columnNames) {
        return Styles.gridTemplateColumnNames.of(Arrays.asList(columnNames));
    }

    public static StyleValue<List<NamedGridLine>> gridTemplateRowNames(List<NamedGridLine> rowNames) {
        return Styles.gridTemplateRowNames.of(rowNames);
    }

    public static StyleValue<List<NamedGridLine>> gridTemplateRowNames(NamedGridLine... rowNames) {
        return Styles.gridTemplateRowNames.of(Arrays.asList(rowNames));
    }

    public static NamedGridLine namedGridLine(String name, int index) {
        return new NamedGridLine(name, index);
    }

    // region grid placement values

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

    // endregion

    // endregion
}
