package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;

public class ContextMenu<T extends Widget> extends WidgetGroup<T> {

    public ContextMenu() {
        this(10);
    }

    public ContextMenu(int height) {
    }

}
