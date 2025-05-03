package dev.vfyjxf.cloudlib.api.ui.layout.modifier;

import dev.vfyjxf.cloudlib.api.ui.layout.LayoutConfigurator;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Design from Jetpack Compose
 * <p>
 * The layout modifier to pass layout parameters to a widget.
 */
public interface Modifier {

    Modifier EMPTY = new Modifier() {
        @Override
        public <R> R foldIn(R initial, BiFunction<R, Modifier, R> operation) {
            return initial;
        }

        @Override
        public <R> R foldOut(R initial, BiFunction<R, Modifier, R> operation) {
            return initial;
        }

        @Override
        public boolean any(Predicate<Modifier> predicate) {
            return false;
        }

        @Override
        public boolean all(Predicate<Modifier> predicate) {
            return true;
        }

        @Override
        public Modifier then(Modifier other) {
            return other;
        }

        @Override
        public void apply(YogaNode node) {

        }
    };

    static Modifier builder() {
        return EMPTY;
    }

    static Modifier start() {
        return EMPTY;
    }

    //region Basic

    <R> R foldIn(R initial, BiFunction<R, Modifier, R> operation);

    <R> R foldOut(R initial, BiFunction<R, Modifier, R> operation);

    boolean any(Predicate<Modifier> predicate);

    boolean all(Predicate<Modifier> predicate);

    default Modifier then(Modifier other) {
        if (other == EMPTY) return this;
        else return new CombinedModifier(this, other);
    }

    default Modifier then(Consumer<YogaNode> function) {
        return then(new FunctionModifier(function));
    }

    void apply(YogaNode node);

    //endregion

    //region Composition function

    default Modifier layoutWith(LayoutConfigurator configurator) {
        return then(configurator);
    }

    default <T extends Modifier> T with(Function<Modifier, T> caster, Function<T, T> domainConfig) {
        return domainConfig.apply(caster.apply(this));
    }

    default <M extends Modifier> M with(Function<Modifier, M> caster) {
        return caster.apply(this);
    }

    default <M extends Modifier, P> M with(BiFunction<Modifier, P, M> delegator, P parameter, Function<M, M> domainConfig) {
        M modifier = delegator.apply(this, parameter);
        return domainConfig.apply(modifier);
    }

    default <M extends Modifier, P> M with(BiFunction<Modifier, P, M> delegator, P parameter) {
        return delegator.apply(this, parameter);
    }

    //endregion

    //region Yoga operations

    //region Width properties

    default Modifier width(StyleSizeLength length) {
        return then(node -> node.setWidth(length));
    }

    default Modifier width(float width) {
        return width(StyleSizeLength.points(width));
    }

    default Modifier widthPercent(float percent) {
        return width(StyleSizeLength.percent(percent));
    }

    default Modifier widthAuto() {
        return width(StyleSizeLength.ofAuto());
    }

    default Modifier widthMaxContent() {
        return width(StyleSizeLength.ofMaxContent());
    }

    default Modifier widthFitContent() {
        return width(StyleSizeLength.ofFitContent());
    }

    default Modifier widthStretch() {
        return width(StyleSizeLength.ofStretch());
    }

    default Modifier widthIn(StyleSizeLength minWidth, StyleSizeLength maxWidth) {
        return then(node -> {
            node.setMinWidth(minWidth);
            node.setMaxWidth(maxWidth);
        });
    }

    default Modifier widthIn(float minWidth, float maxWidth) {
        return widthIn(StyleSizeLength.points(minWidth), StyleSizeLength.points(maxWidth));
    }

    default Modifier widthInPercent(StyleSizeLength minWidth, StyleSizeLength maxWidth) {
        return widthIn(minWidth, maxWidth);
    }

    default Modifier widthInPercent(float minWidth, float maxWidth) {
        return widthIn(StyleSizeLength.percent(minWidth), StyleSizeLength.percent(maxWidth));
    }

    default Modifier minWidth(StyleSizeLength length) {
        return then(node -> node.setMinWidth(length));
    }

    default Modifier minWidth(float minWidth) {
        return minWidth(StyleSizeLength.points(minWidth));
    }

    default Modifier minWidthPercent(float percent) {
        return minWidth(StyleSizeLength.percent(percent));
    }

    default Modifier minWidthMaxContent() {
        return minWidth(StyleSizeLength.ofMaxContent());
    }

    default Modifier minWidthFitContent() {
        return minWidth(StyleSizeLength.ofFitContent());
    }

    default Modifier minWidthStretch() {
        return minWidth(StyleSizeLength.ofStretch());
    }

    default Modifier maxWidth(StyleSizeLength length) {
        return then(node -> node.setMaxWidth(length));
    }

    default Modifier maxWidth(float maxWidth) {
        return maxWidth(StyleSizeLength.points(maxWidth));
    }

    default Modifier maxWidthPercent(float percent) {
        return maxWidth(StyleSizeLength.percent(percent));
    }

    default Modifier maxWidthMaxContent() {
        return maxWidth(StyleSizeLength.ofMaxContent());
    }

    default Modifier maxWidthFitContent() {
        return maxWidth(StyleSizeLength.ofFitContent());
    }

    default Modifier maxWidthStretch() {
        return maxWidth(StyleSizeLength.ofStretch());
    }

    //endregion

    //region Height properties

    default Modifier height(StyleSizeLength length) {
        return then(node -> node.setHeight(length));
    }

    default Modifier height(float height) {
        return height(StyleSizeLength.points(height));
    }

    default Modifier heightPercent(float percent) {
        return height(StyleSizeLength.percent(percent));
    }

    default Modifier heightAuto() {
        return height(StyleSizeLength.ofAuto());
    }

    default Modifier heightMaxContent() {
        return height(StyleSizeLength.ofMaxContent());
    }

    default Modifier heightFitContent() {
        return height(StyleSizeLength.ofFitContent());
    }

    default Modifier heightStretch() {
        return height(StyleSizeLength.ofStretch());
    }

    default Modifier heightIn(StyleSizeLength minHeight, StyleSizeLength maxHeight) {
        return then(node -> {
            node.setMinHeight(minHeight);
            node.setMaxHeight(maxHeight);
        });
    }

    default Modifier heightIn(float minHeight, float maxHeight) {
        return heightIn(StyleSizeLength.points(minHeight), StyleSizeLength.points(maxHeight));
    }

    default Modifier heightInPercent(StyleSizeLength minHeight, StyleSizeLength maxHeight) {
        return heightIn(minHeight, maxHeight);
    }

    default Modifier heightInPercent(float minHeight, float maxHeight) {
        return heightIn(StyleSizeLength.percent(minHeight), StyleSizeLength.percent(maxHeight));
    }

    default Modifier minHeight(StyleSizeLength length) {
        return then(node -> node.setMinHeight(length));
    }

    default Modifier minHeight(float minHeight) {
        return minHeight(StyleSizeLength.points(minHeight));
    }

    default Modifier minHeightPercent(float percent) {
        return minHeight(StyleSizeLength.percent(percent));
    }

    default Modifier minHeightMaxContent() {
        return minHeight(StyleSizeLength.ofMaxContent());
    }

    default Modifier minHeightFitContent() {
        return minHeight(StyleSizeLength.ofFitContent());
    }

    default Modifier minHeightStretch() {
        return minHeight(StyleSizeLength.ofStretch());
    }

    default Modifier maxHeight(StyleSizeLength length) {
        return then(node -> node.setMaxHeight(length));
    }

    default Modifier maxHeight(float maxHeight) {
        return maxHeight(StyleSizeLength.points(maxHeight));
    }

    default Modifier maxHeightPercent(float percent) {
        return maxHeight(StyleSizeLength.percent(percent));
    }

    default Modifier maxHeightMaxContent() {
        return maxHeight(StyleSizeLength.ofMaxContent());
    }

    default Modifier maxHeightFitContent() {
        return maxHeight(StyleSizeLength.ofFitContent());
    }

    default Modifier maxHeightStretch() {
        return maxHeight(StyleSizeLength.ofStretch());
    }

    //endregion

    //region Size properties

    default Modifier size(StyleSizeLength width, StyleSizeLength height) {
        return then(node -> {
            node.setWidth(width);
            node.setHeight(height);
        });
    }

    default Modifier size(float width, float height) {
        return size(StyleSizeLength.points(width), StyleSizeLength.points(height));
    }

    default Modifier sizeIn(StyleSizeLength minWidth, StyleSizeLength minHeight, StyleSizeLength maxWidth, StyleSizeLength maxHeight) {
        return then(node -> {
            node.setMinWidth(minWidth);
            node.setMinHeight(minHeight);
            node.setMaxWidth(maxWidth);
            node.setMaxHeight(maxHeight);
        });
    }

    default Modifier sizeIn(float minWidth, float minHeight, float maxWidth, float maxHeight) {
        return sizeIn(StyleSizeLength.points(minWidth), StyleSizeLength.points(minHeight), StyleSizeLength.points(maxWidth), StyleSizeLength.points(maxHeight));
    }


    default Modifier sizeInPercent(StyleSizeLength minWidth, StyleSizeLength minHeight, StyleSizeLength maxWidth, StyleSizeLength maxHeight) {
        return sizeIn(minWidth, minHeight, maxWidth, maxHeight);
    }

    default Modifier sizeInPercent(float minWidth, float minHeight, float maxWidth, float maxHeight) {
        return sizeIn(StyleSizeLength.percent(minWidth), StyleSizeLength.percent(minHeight), StyleSizeLength.percent(maxWidth), StyleSizeLength.percent(maxHeight));
    }

    //endregion

    //region Margin properties

    default Modifier margin(StyleLength length) {
        return margin(YogaEdge.ALL, length);
    }

    default Modifier margin(float margin) {
        return margin(StyleLength.points(margin));
    }

    default Modifier margin(YogaEdge edge, StyleLength length) {
        return then(node -> node.setMargin(edge, length));
    }

    default Modifier margin(YogaEdge edge, float margin) {
        return margin(edge, StyleLength.points(margin));
    }

    default Modifier marginPercent(YogaEdge edge, float percent) {
        return margin(edge, StyleLength.percent(percent));
    }

    default Modifier marginAuto(YogaEdge edge) {
        return margin(edge, StyleLength.ofAuto());
    }

    //endregion

    //region Padding properties

    default Modifier padding(StyleLength length) {
        return padding(YogaEdge.ALL, length);
    }

    default Modifier padding(float padding) {
        return padding(StyleLength.points(padding));
    }

    default Modifier padding(YogaEdge edge, StyleLength length) {
        return then(node -> node.setPadding(edge, length));
    }

    default Modifier padding(YogaEdge edge, float padding) {
        return padding(edge, StyleLength.points(padding));
    }

    default Modifier paddingPercent(YogaEdge edge, float percent) {
        return padding(edge, StyleLength.percent(percent));
    }

    //endregion

    //region Position properties

    default Modifier positionType(YogaPositionType positionType) {
        return then(node -> node.setPositionType(positionType));
    }

    default Modifier position(YogaEdge edge, StyleLength length) {
        return then(node -> node.setPosition(edge, length));
    }

    default Modifier position(YogaEdge edge, float position) {
        return position(edge, StyleLength.points(position));
    }

    default Modifier positionPercent(YogaEdge edge, float percent) {
        return position(edge, StyleLength.percent(percent));
    }

    default Modifier positionAuto(YogaEdge edge) {
        return position(edge, StyleLength.ofAuto());
    }

    //endregion

    //region Alignment properties

    //region Align Content

    default Modifier alignContentAuto() {
        return alignContent(YogaAlign.AUTO);
    }

    default Modifier alignContentFlexStart() {
        return alignContent(YogaAlign.FLEX_START);
    }

    default Modifier alignContentCenter() {
        return alignContent(YogaAlign.CENTER);
    }

    default Modifier alignContentFlexEnd() {
        return alignContent(YogaAlign.FLEX_END);
    }

    default Modifier alignContentStretch() {
        return alignContent(YogaAlign.STRETCH);
    }

    default Modifier alignContentBaseline() {
        return alignContent(YogaAlign.BASELINE);
    }

    default Modifier alignContentSpaceBetween() {
        return alignContent(YogaAlign.SPACE_BETWEEN);
    }

    default Modifier alignContentSpaceAround() {
        return alignContent(YogaAlign.SPACE_AROUND);
    }

    default Modifier alignContentSpaceEvenly() {
        return alignContent(YogaAlign.SPACE_EVENLY);
    }

    default Modifier alignContent(YogaAlign alignContent) {
        return then(node -> node.setAlignContent(alignContent));
    }

    //endregion

    //region Align Items

    default Modifier alignItemsAuto() {
        return alignItems(YogaAlign.AUTO);
    }

    default Modifier alignItemsFlexStart() {
        return alignItems(YogaAlign.FLEX_START);
    }

    default Modifier alignItemsCenter() {
        return alignItems(YogaAlign.CENTER);
    }

    default Modifier alignItemsFlexEnd() {
        return alignItems(YogaAlign.FLEX_END);
    }

    default Modifier alignItemsStretch() {
        return alignItems(YogaAlign.STRETCH);
    }

    default Modifier alignItemsBaseline() {
        return alignItems(YogaAlign.BASELINE);
    }

    default Modifier alignItemsSpaceBetween() {
        return alignItems(YogaAlign.SPACE_BETWEEN);
    }

    default Modifier alignItemsSpaceAround() {
        return alignItems(YogaAlign.SPACE_AROUND);
    }

    default Modifier alignItemsSpaceEvenly() {
        return alignItems(YogaAlign.SPACE_EVENLY);
    }

    default Modifier alignItems(YogaAlign alignItems) {
        return then(node -> node.setAlignItems(alignItems));
    }

    //endregion

    //region Align Self

    default Modifier alignSelfAuto() {
        return alignSelf(YogaAlign.AUTO);
    }

    default Modifier alignSelfFlexStart() {
        return alignSelf(YogaAlign.FLEX_START);
    }

    default Modifier alignSelfCenter() {
        return alignSelf(YogaAlign.CENTER);
    }

    default Modifier alignSelfFlexEnd() {
        return alignSelf(YogaAlign.FLEX_END);
    }

    default Modifier alignSelfStretch() {
        return alignSelf(YogaAlign.STRETCH);
    }

    default Modifier alignSelfBaseline() {
        return alignSelf(YogaAlign.BASELINE);
    }

    default Modifier alignSelfSpaceBetween() {
        return alignSelf(YogaAlign.SPACE_BETWEEN);
    }

    default Modifier alignSelfSpaceAround() {
        return alignSelf(YogaAlign.SPACE_AROUND);
    }

    default Modifier alignSelfSpaceEvenly() {
        return alignSelf(YogaAlign.SPACE_EVENLY);
    }

    default Modifier alignSelf(YogaAlign alignSelf) {
        return then(node -> node.setAlignSelf(alignSelf));
    }

    //endregion

    //endregion

    //region Flex properties

    default Modifier flex(float flex) {
        return then(node -> node.setFlex(flex));
    }

    default Modifier flexBasisAuto() {
        return then(YogaNode::setFlexBasisAuto);
    }

    default Modifier flexBasisPercent(float percent) {
        return then(node -> node.setFlexBasisPercent(percent));
    }

    default Modifier flexBasis(float flexBasis) {
        return then(node -> node.setFlexBasis(flexBasis));
    }

    default Modifier flexBasisMaxContent() {
        return then(YogaNode::setFlexBasisMaxContent);
    }

    default Modifier flexBasisFitContent() {
        return then(YogaNode::setFlexBasisFitContent);
    }

    default Modifier flexBasisStretch() {
        return then(YogaNode::setFlexBasisStretch);
    }

    default Modifier flexDirection(YogaFlexDirection direction) {
        return then(node -> node.setFlexDirection(direction));
    }

    default Modifier flexGrow(float flexGrow) {
        return then(node -> node.setFlexGrow(flexGrow));
    }

    default Modifier flexShrink(float flexShrink) {
        return then(node -> node.setFlexShrink(flexShrink));
    }

    //endregion

    //region Other properties

    default Modifier justifyContent(YogaJustify justifyContent) {
        return then(node -> node.setJustifyContent(justifyContent));
    }

    default Modifier direction(YogaDirection direction) {
        return then(node -> node.setDirection(direction));
    }

    default Modifier border(YogaEdge edge, float value) {
        return then(node -> node.setBorder(edge, value));
    }

    default Modifier wrap(YogaWrap wrap) {
        return then(node -> node.setWrap(wrap));
    }

    default Modifier aspectRatio(float aspectRatio) {
        return then(node -> node.setAspectRatio(aspectRatio));
    }

    default Modifier isReferenceBaseline(boolean isReferenceBaseline) {
        return then(node -> node.setIsReferenceBaseline(isReferenceBaseline));
    }

    default Modifier measureFunction(YogaMeasureFunction measureFunction) {
        return then(node -> node.setMeasureFunction(measureFunction));
    }

    default Modifier baselineFunction(YogaBaselineFunction yogaBaselineFunction) {
        return then(node -> node.setBaselineFunction(yogaBaselineFunction));
    }

    default Modifier boxSizing(YogaBoxSizing boxSizing) {
        return then(node -> node.setBoxSizing(boxSizing));
    }

    //endregion

    //region Gap properties


    default Modifier gap(YogaGutter gutter, StyleLength gap) {
        return then(node -> node.setGap(gutter, gap));
    }

    default Modifier gap(YogaGutter gutter, float gap) {
        return gap(gutter, StyleLength.points(gap));
    }

    default Modifier gapPercent(YogaGutter gutter, float percent) {
        return gap(gutter, StyleLength.percent(percent));
    }

    default Modifier gapAuto(YogaGutter gutter) {
        return gap(gutter, StyleLength.ofAuto());
    }

    default Modifier gap(float gap) {
        return gap(YogaGutter.ALL, gap);
    }

    default Modifier gapPercent(float percent) {
        return gapPercent(YogaGutter.ALL, percent);
    }

    default Modifier rowGap(float gap) {
        return then(node -> node.setGap(YogaGutter.ROW, StyleLength.points(gap)));
    }

    default Modifier rowGapPercent(float percent) {
        return then(node -> node.setGap(YogaGutter.ROW, StyleLength.percent(percent)));
    }

    default Modifier columnGap(float gap) {
        return then(node -> node.setGap(YogaGutter.COLUMN, StyleLength.points(gap)));
    }

    default Modifier columnGapPercent(float percent) {
        return then(node -> node.setGap(YogaGutter.COLUMN, StyleLength.percent(percent)));
    }

    //endregion

    //endregion
}
