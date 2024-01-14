package dev.vfyjxf.cloudlib.api.ui.widgets;

public interface ITooltipStack<T> {

    T value();

    Class<T> type();

    default <R> R castValue() {
        return (R) value();
    }

}
