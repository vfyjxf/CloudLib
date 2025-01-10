package dev.vfyjxf.cloudlib.api.data;

public interface ObservableValue<T> {

    static <T> ObservableValue<T> of(T value) {
        return null;
    }

    T get();


}
