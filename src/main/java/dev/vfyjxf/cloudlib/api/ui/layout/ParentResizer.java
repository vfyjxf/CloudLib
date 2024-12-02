package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

public interface ParentResizer {
    void apply(Widget widget, Resizer resizer, ChildResizer child);
}
