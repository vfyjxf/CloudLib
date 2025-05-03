package dev.vfyjxf.cloudlib.api.ui.state;

public interface MutableState<T> extends ReadableState<T> {

    /**
     * @param value the new state value.
     * @return the previous state value.
     */
    T set(T value);
}
