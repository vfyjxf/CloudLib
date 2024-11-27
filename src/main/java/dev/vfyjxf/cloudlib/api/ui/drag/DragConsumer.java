package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public interface DragConsumer<T extends Widget> {

    WidgetGroup<T> self();

    DragConsumer<T> add(T draggable, Runnable onEnter);

    boolean add(int index, T draggable);

    void add(T draggable, int index, Runnable onEnter);

    boolean accept(T draggable);

}
