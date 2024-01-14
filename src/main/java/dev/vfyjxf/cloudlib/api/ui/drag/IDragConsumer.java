package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;

public interface IDragConsumer<T extends IWidget> {

    IWidgetGroup<T> self();

    IDragConsumer<T> add(T draggable, Runnable onEnter);

    IDragConsumer<T> add(int index, T draggable);

    void add(T draggable, int index, Runnable onEnter);

    boolean accept(T draggable);

}
