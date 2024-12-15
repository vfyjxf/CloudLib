package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

public interface ParentResizer {

    default void apply(Widget widget, Flex resizer, ChildResizer child) {

    }
}
