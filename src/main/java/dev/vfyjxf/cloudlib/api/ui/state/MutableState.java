package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.ui.state.compound.CompoundState;
import org.eclipse.collections.api.list.MutableList;

public interface MutableState<T> extends ReadableState<T> {

    static <T> MutableState<MutableList<T>> mutableListOf() {
        return State.mutableOf(CompoundState.listOf());
    }

    @SafeVarargs
    static <T> MutableState<MutableList<T>> mutableListOf(T... elements) {
        return State.mutableOf(CompoundState.listOf(elements));
    }

    /**
     * @param value the new state value.
     * @return the previous state value.
     */
    T set(T value);
}
