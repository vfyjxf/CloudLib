package dev.vfyjxf.cloudlib.api.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A series of event listeners.
 */
public interface ITrait {

    ITrait EMPTY = new ITrait() {
        @Override
        public <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator) {
            return initial;
        }

        @Override
        public <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator) {
            return initial;
        }

        @Override
        public boolean any(Predicate<ITraitElement> predicate) {
            return false;
        }

        @Override
        public boolean all(Predicate<ITraitElement> predicate) {
            return true;
        }

        @Override
        public ITrait then(ITrait trait) {
            return trait;
        }

        @Override
        public String toString() {
            return "Trait";
        }
    };

    <T> T foldIn(T initial, BiFunction<T, ITraitElement, T> operator);

    <T> T foldOut(T initial, BiFunction<T, ITraitElement, T> operator);

    boolean any(Predicate<ITraitElement> predicate);

    boolean all(Predicate<ITraitElement> predicate);

    default ITrait then(ITrait trait) {
        if (trait == ITrait.EMPTY) return this;
        return new CombinedTrait(this, trait);
    }

    default ITrait then(ITrait... traits) {
        ITrait trait = this;
        for (ITrait t : traits) trait = trait.then(t);
        return trait;
    }

    default void init() {

    }

    default void update() {

    }

    abstract class DataNode {
        protected boolean isAttached = false;
        @Nullable
        private IWidget holder = null;

        public boolean isAttached() {
            return isAttached;
        }

        protected void setAttached(boolean attached) {
            isAttached = attached;
        }

        public boolean shouldAutoInvalidate() {
            return true;
        }

        @ApiStatus.Internal
        public void markAsAttached() {
            if (isAttached)
                throw new IllegalStateException("DataNode is already attached");
            if (holder == null)
                throw new IllegalStateException("DataNode attachment called without handler");
            setAttached(true);
        }

        public void onAttach() {
        }

        public void onDetach() {
        }

        public @Nullable IWidget holder() {
            return holder;
        }

        public void updateHolder(@Nullable IWidget holder) {
            this.holder = holder;
        }
    }


}
