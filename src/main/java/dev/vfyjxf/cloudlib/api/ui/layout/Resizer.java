package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public interface Resizer {
    default void preApply(Widget widget) {

    }

    default void apply(Widget widget) {

    }

    default void postApply(Widget widget) {

    }

    default void add(WidgetGroup<?> parent, Widget child) {

    }

    default void remove(WidgetGroup<?> parent, Widget child) {

    }

    int getX();

    int getY();

    int getWidth();

    int getHeight();
}