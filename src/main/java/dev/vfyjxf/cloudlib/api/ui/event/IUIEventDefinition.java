package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

public interface IUIEventDefinition<T> extends IEventDefinition<T> {

    default <W extends IWidget> void onEvent(W widget, T listener) {
        widget.onEvent(this, listener);
    }

}
