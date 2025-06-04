package dev.vfyjxf.cloudlib.api.ui.state;

public interface ReadableState<T> extends State {

    default T get() {
        return value();
    }

    T value();
}
