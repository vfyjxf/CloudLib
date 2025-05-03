package dev.vfyjxf.cloudlib.api.ui.widget;

public interface TooltipStack<T> {

    T value();

    Class<T> type();

    default <R> R castValue() {
        return (R) value();
    }

}
