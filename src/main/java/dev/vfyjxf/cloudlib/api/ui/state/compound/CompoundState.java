package dev.vfyjxf.cloudlib.api.ui.state.compound;

import dev.vfyjxf.cloudlib.api.ui.state.State;

/**
 * State like List<A>
 */
public sealed interface CompoundState
        extends State
        permits MutableListState {

    static <T> MutableListState<T> listOf() {
        return new MutableListState<>();
    }

    @SafeVarargs
    static <T> MutableListState<T> listOf(T... elements) {
        return new MutableListState<>(elements);
    }

    @Override
    boolean changed();
}
