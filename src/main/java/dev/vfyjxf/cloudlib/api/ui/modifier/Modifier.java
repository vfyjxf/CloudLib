package dev.vfyjxf.cloudlib.api.ui.modifier;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.ContextMenuBuilder;
import dev.vfyjxf.cloudlib.api.ui.ContextMenuProvider;
import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.alignment.Alignment;
import dev.vfyjxf.cloudlib.api.ui.alignment.AlignmentReviver;
import dev.vfyjxf.cloudlib.api.ui.layout.Flex;
import dev.vfyjxf.cloudlib.api.ui.layout.Resizer;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.ui.ContextMenuManager;
import dev.vfyjxf.cloudlib.ui.textures.ColorTexture;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.jetbrains.annotations.Range;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
        public void apply(Widget widget) {

        }
    };

    static Modifier builder() {
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

    default Modifier then(Consumer<Widget> function) {
        return then(new FunctionModifier(function));
    }

    //TODO:compile time check
    default Modifier next(Consumer<WidgetGroup<?>> function) {
        return then(new FunctionModifier(widget -> {
            if (widget instanceof WidgetGroup<?> group) {
                function.accept(group);
            } else throw new IllegalArgumentException("This modifier can only be applied to WidgetGroup");
        }));
    }

    void apply(Widget widget);

    //endregion

    //region Position

    default Modifier pos(int x, int y) {
        return then(widget -> {
            widget.flex().x().setValue(x);
            widget.flex().y().setValue(y);
        });
    }

    default Modifier pos(Pos pos) {
        return pos(pos.x, pos.y);
    }

    default Modifier posX(int x) {
        return pos(x, 0);
    }

    default Modifier posY(int y) {
        return pos(0, y);
    }

    default Modifier posRel(@Range(from = 0, to = 1) double x, @Range(from = 0, to = 1) double y) {
        Checks.checkRangeClosed(x, 0, 1);
        return then(widget -> {
            widget.flex().x().setFactor((float) x);
            widget.flex().y().setFactor((float) y);
        });
    }

    default Modifier posRelX(@Range(from = 0, to = 1) double x) {
        return posRel(x, 0);
    }

    default Modifier posRelY(@Range(from = 0, to = 1) double y) {
        return posRel(0, y);
    }

    //endregion

    //region Width
    default Modifier widthFixed(int width) {
        return then(widget -> {
            widget.flex().width().setValue(width).setMin(width).setMax(width);
            widget.flex().setWidthExpandable(false);
        });
    }

    /**
     * constrain the width of the widget
     *
     * @param minWidth the minimum width of the widget
     * @param maxWidth the maximum width of the widget
     * @return the modifier
     */
    default Modifier widthIn(int minWidth, int maxWidth) {
        return then(widget -> {
            widget.flex().width().setMin(minWidth).setMax(maxWidth);
        });
    }

    /**
     * C helper method to avoid declaring the parameter as float when using this method
     */
    default Modifier fillMaxWidth(@Range(from = 0, to = 1) double fraction) {
        Checks.checkRangeClosed(fraction, 0, 1);
        return then(widget -> {
            widget.flex().width().setFactor((float) fraction);
        });
    }

    default Modifier requiredWidth(int width) {
        return then(widget -> {
            widget.flex().width().setValue(width).setMin(width).setMax(width).setEnforced(true);
            widget.flex().setWidthExpandable(false);
        });
    }

    //endregion

    // region Height

    default Modifier heightFixed(int height) {
        return then(widget -> {
            widget.flex().height().setValue(height).setMin(height).setMax(height);
            widget.flex().setHeightExpandable(false);
        });
    }

    default Modifier heightIn(int minHeight, int maxHeight) {
        return then(widget -> {
            widget.flex().height().setMin(minHeight).setMax(maxHeight);
        });
    }

    default Modifier fillMaxHeight(@Range(from = 0, to = 1) double fraction) {
        Checks.checkRangeClosed(fraction, 0, 1);
        return then(widget -> {
            widget.flex().height().setFactor((float) fraction);
        });
    }

    default Modifier requiredHeight(int height) {
        return then(widget -> {
            widget.flex().height().setValue(height).setMin(height).setMax(height).setEnforced(true);
            widget.flex().setHeightExpandable(false);
        });
    }

    //endregion

    // region Size

    default Modifier size(int size) {
        return size(size, size);
    }

    default Modifier size(int width, int height) {
        return then(widget -> {
            widget.flex().width().setValue(width);
            widget.flex().height().setValue(height);
            widget.flex().setWidthExpandable(false).setHeightExpandable(false);
        });
    }

    default Modifier fillMaxSize(@Range(from = 0, to = 1) double width, @Range(from = 0, to = 1) double height) {
        Checks.checkRangeClosed(width, 0, 1);
        Checks.checkRangeClosed(height, 0, 1);
        return then(widget -> {
            widget.flex().width().setFactor((float) width);
            widget.flex().height().setFactor((float) height);
        });
    }

    default Modifier sizeIn(int minWidth, int maxWidth, int minHeight, int maxHeight) {
        return then(widget -> {
            widget.flex().width().setMin(minWidth).setMax(maxWidth);
            widget.flex().height().setMin(minHeight).setMax(maxHeight);
        });
    }

    default Modifier requiredSize(int width, int height) {
        return then(widget -> {
            widget.flex().width().setValue(width).setMax(width).setMax(width).setEnforced(true);
            widget.flex().height().setValue(height).setMax(height).setMax(height).setEnforced(true);
        });
    }

    default Modifier requireSize(int size) {
        return requiredSize(size, size);
    }

    //endregion

    //region Anchor

    default Modifier anchor(@Range(from = 0, to = 1) double x, @Range(from = 0, to = 1) double y) {
        Checks.checkRangeClosed(x, 0, 1);
        Checks.checkRangeClosed(y, 0, 1);
        return then(widget -> {
            widget.flex().x().setAnchor((float) x);
            widget.flex().y().setAnchor((float) y);
        });
    }

    default Modifier anchor(@Range(from = 0, to = 1) double anchor) {
        return anchor(anchor, anchor);
    }

    default Modifier anchorX(@Range(from = 0, to = 1) double x) {
        return anchor(x, 0);
    }

    default Modifier anchorY(@Range(from = 0, to = 1) double y) {
        return anchor(0, y);
    }

    //endregion

    //region Margin

    default Modifier margin(int margin) {
        return then(widget -> {
            widget.margin().all(margin);
        });
    }

    default Modifier margin(int horizontal, int vertical) {
        return then(widget -> {
            widget.margin().start(horizontal).end(horizontal).top(vertical).bottom(vertical);
        });
    }

    default Modifier margin(int top, int right, int bottom, int left) {
        return then(widget -> {
            widget.margin().top(top).end(right).bottom(bottom).start(left);
        });
    }

    default Modifier marginLeft(int left) {
        return then(widget -> {
            widget.margin().start(left);
        });
    }

    default Modifier marginRight(int right) {
        return then(widget -> {
            widget.margin().end(right);
        });
    }

    default Modifier marginTop(int top) {
        return then(widget -> {
            widget.margin().top(top);
        });
    }

    default Modifier marginBottom(int bottom) {
        return then(widget -> {
            widget.margin().bottom(bottom);
        });
    }

    //endregion

    //region Padding

    default Modifier padding(int padding) {
        return then(widget -> {
            widget.padding().all(padding);
        });
    }

    default Modifier padding(int horizontal, int vertical) {
        return then(widget -> {
            widget.padding().all(horizontal, vertical);
        });
    }

    default Modifier padding(int start, int top, int end, int bottom) {
        return then(widget -> {
            widget.padding().all(start, top, end, bottom);
        });
    }

    //endregion

    //region Offset

    default Modifier offset(int x, int y) {
        return then(widget -> {
            widget.offset()
                    .x(x)
                    .y(y);
        });
    }

    //endregion

    //region Align

    /**
     * NOTE:the align modifier will override the alignment of parent's default alignment
     *
     * @param alignment the alignment to define the alignment of a layout inside a parent layout.
     */
    default Modifier align(Alignment alignment) {
        return then(widget -> {
            widget.flex().alignment(alignment);
            if (widget instanceof AlignmentReviver<?> reviver) {
                reviver.alignment(alignment);
            }
        });
    }

    /**
     * see {@link Modifier#align(Alignment)}
     */
    default Modifier horizontalAlign(Alignment.Horizontal alignment) {
        return then(widget -> {
            widget.flex().horizontalAlignment(alignment);
            if (widget instanceof AlignmentReviver<?> reviver) {
                reviver.horizontalAlignment(alignment);
            }
        });
    }

    /**
     * see {@link Modifier#align(Alignment)}
     */
    default Modifier verticalAlign(Alignment.Vertical alignment) {
        return then(widget -> {
            widget.flex().verticalAlignment(alignment);
            if (widget instanceof AlignmentReviver<?> reviver) {
                reviver.verticalAlignment(alignment);
            }
        });
    }

    //endregion

    //region Background

    default Modifier background(RenderableTexture background) {
        return then(widget -> {
            widget.setBackground(background);
        });
    }

    default Modifier background(int color) {
        return then(widget -> {
            widget.setBackground(new ColorTexture(color));
        });
    }

    //FIXME:size and position scale bug
    default Modifier scale(double scale) {
        return then(widget -> {
            widget.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
                graphics.pose().scale((float) scale, (float) scale, 1);
            });
        });
    }

    default Modifier scale(double scaleX, double scaleY) {
        return then(widget -> {
            widget.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
                graphics.pose().scale((float) scaleX, (float) scaleY, 1);
            });
        });
    }

    default Modifier scaleWhen(float scaleX, float scaleY, Predicate<Widget> predicate) {
        return then(widget -> {
            widget.onRender((graphics, mouseX, mouseY, partialTicks, context) -> {
                if (predicate.test(widget)) {
                    graphics.pose().scale(scaleX, scaleY, 1);
                }
            });
        });
    }

    //endregion

    //region Layout
    default Modifier layout(Resizer layout) {
        return next(widget -> {
            widget.flex().setPost(layout);
        });
    }

    default <T extends Resizer> Modifier layoutWith(Function<WidgetGroup<? extends Widget>, T> builder) {
        return layoutWith(builder, l -> {});
    }

    default <T extends Resizer> Modifier layoutWith(Function<WidgetGroup<? extends Widget>, T> builder, Consumer<T> configurator) {
        return next(widget -> {
            T layout = builder.apply(widget);
            configurator.accept(layout);
            widget.flex().setPost(layout);
        });
    }

    default <T extends Resizer> Modifier layout(Supplier<T> layoutBuilder) {
        return layout(layoutBuilder.get());
    }

    //endregion

    //region Extra Widget
    default Modifier contextMenu(ContextMenuProvider provider) {
        return then(widget -> {
            widget.onMouseClicked((input, context) -> {
                if (input.isRightClick()) {
                    var builder = ContextMenuBuilder.builder();
                    provider.buildMenu(builder, widget, input.mouseX(), input.mouseY());
                    ContextMenuManager.getInstance().display(builder);
                }
                return false;
            });
        });
    }

    //endregion

    //region Reset
    default Modifier resetPos() {
        return then(widget -> {
            widget.flex().resetX();
            widget.flex().resetY();
        });
    }

    default Modifier resetX() {
        return then(widget -> {
            widget.flex().resetX();
        });
    }

    default Modifier resetY() {
        return then(widget -> {
            widget.flex().resetY();
        });
    }

    default Modifier resetSize() {
        return then(widget -> {
            widget.flex().resetWidth();
            widget.flex().resetHeight();
        });
    }

    default Modifier resetWidth() {
        return then(widget -> {
            widget.flex().resetWidth();
        });
    }

    default Modifier resetHeight() {
        return then(widget -> {
            widget.flex().resetHeight();
        });
    }

    default Modifier resetPadding() {
        return then(widget -> {
            widget.padding().reset();
        });
    }

    default Modifier resetMargin() {
        return then(widget -> {
            widget.margin().reset();
        });
    }

    default Modifier resetOffset() {
        return then(widget -> {
            widget.offset().reset();
        });
    }


    //endregion

    //region Custom

    /**
     * Normally, accessing Flex directly from the Widget is the faster option, and this method only exists to maintain coherence.
     */
    default Modifier custom(Consumer<Flex> function) {
        return then(widget -> {
            function.accept(widget.flex());
        });
    }

    /**
     * @see Modifier#custom(Consumer)  same as custom
     */
    default Modifier set(Consumer<Widget> function) {
        return then(function);
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
}
