package dev.vfyjxf.cloudlib.api.ui.layout.modifier;

import dev.vfyjxf.cloudlib.api.ui.layout.Flex;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.utils.Checks;
import org.jetbrains.annotations.Range;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Design from Jetpack Compose
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

    static Modifier start() {
        return EMPTY;
    }

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

    void apply(Widget widget);

    //////////////////////////////////////
    //********     Position    *********//
    //////////////////////////////////////

    default Modifier pos(int x, int y) {
        return then(widget -> {
            widget.flex().x.setValue(x);
            widget.flex().y.setValue(y);
        });
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
            widget.flex().x.setFactor((float) x);
            widget.flex().y.setFactor((float) y);
        });
    }

    default Modifier posRelX(@Range(from = 0, to = 1) double x) {
        return posRel(x, 0);
    }

    default Modifier posRelY(@Range(from = 0, to = 1) double y) {
        return posRel(0, y);
    }

    //////////////////////////////////////
    //********      Width      *********//
    //////////////////////////////////////

    default Modifier widthFixed(int width) {
        return then(widget -> {
            widget.flex().width.setValue(width);
        });
    }

    default Modifier widthIn(int minWidth, int maxWidth) {
        return then(widget -> {
            widget.flex().width.setMin(minWidth).setMax(maxWidth);
        });
    }

    /**
     * A helper method to avoid declaring the parameter as float when using this method
     */
    default Modifier fillMaxWidth(@Range(from = 0, to = 1) double fraction) {
        Checks.checkRangeClosed(fraction, 0, 1);
        return then(widget -> {
            widget.flex().width.setFactor((float) fraction);
        });
    }

    //////////////////////////////////////
    //********     Height      *********//
    //////////////////////////////////////

    default Modifier heightFixed(int height) {
        return then(widget -> {
            widget.flex().height.setValue(height);
        });
    }

    default Modifier heightIn(int minHeight, int maxHeight) {
        return then(widget -> {
            widget.flex().height.setMin(minHeight).setMax(maxHeight);
        });
    }

    default Modifier fillMaxHeight(@Range(from = 0, to = 1) double fraction) {
        Checks.checkRangeClosed(fraction, 0, 1);
        return then(widget -> {
            widget.flex().height.setFactor((float) fraction);
        });
    }

    //////////////////////////////////////
    //********      Size       *********//
    //////////////////////////////////////

    default Modifier size(int size) {
        return size(size, size);
    }

    default Modifier size(int width, int height) {
        return then(widget -> {
            widget.flex().width.setValue(width);
            widget.flex().height.setValue(height);
        });
    }

    default Modifier fillMaxSize(@Range(from = 0, to = 1) double width, @Range(from = 0, to = 1) double height) {
        Checks.checkRangeClosed(width, 0, 1);
        Checks.checkRangeClosed(height, 0, 1);
        return then(widget -> {
            widget.flex().width.setFactor((float) width);
            widget.flex().height.setFactor((float) height);
        });
    }

    //////////////////////////////////////
    //********      Anchor     *********//
    //////////////////////////////////////

    default Modifier anchor(@Range(from = 0, to = 1) double x, @Range(from = 0, to = 1) double y) {
        Checks.checkRangeClosed(x, 0, 1);
        Checks.checkRangeClosed(y, 0, 1);
        return then(widget -> {
            widget.flex().x.setAnchor((float) x);
            widget.flex().y.setAnchor((float) y);
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

    //////////////////////////////////////
    //********      Margin     *********//
    //////////////////////////////////////

    default Modifier margin(int margin) {
        return then(widget -> {
            widget.margin().all(margin);
        });
    }

    default Modifier margin(int top, int right, int bottom, int left) {
        return then(widget -> {
            widget.margin().top(top).right(right).bottom(bottom).left(left);
        });
    }

    default Modifier marginLeft(int left) {
        return then(widget -> {
            widget.margin().left(left);
        });
    }

    default Modifier marginRight(int right) {
        return then(widget -> {
            widget.margin().right(right);
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

    //////////////////////////////////////
    //********      Custom     *********//
    //////////////////////////////////////

    /**
     * Normally, accessing Flex directly from the Widget is the faster option, and this method only exists to maintain coherence.
     */
    default Modifier custom(Consumer<Flex> function) {
        return then(widget -> {
            function.accept(widget.flex());
        });
    }

    default Modifier set(Consumer<Widget> function) {
        return then(function);
    }

    default <T extends Modifier> T with(Function<Modifier, T> caster, Function<T, T> domainConfig) {
        return domainConfig.apply(caster.apply(this));
    }

    default <M extends Modifier> Modifier with(Function<Modifier, M> caster) {
        return caster.apply(this);
    }

    default <M extends Modifier, P> M with(BiFunction<Modifier, P, M> delegator, P parameter, Function<M, M> domainConfig) {
        M modifier = delegator.apply(this, parameter);
        return domainConfig.apply(modifier);
    }

    default <M extends Modifier, P> M with(BiFunction<Modifier, P, M> delegator, P parameter) {
        return delegator.apply(this, parameter);
    }

}
