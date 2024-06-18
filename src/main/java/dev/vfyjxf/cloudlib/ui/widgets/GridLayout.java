package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;

public class GridLayout<T extends IWidget> extends WidgetGroup<T> {

    private boolean horizontal;

    public GridLayout<T> horizontal() {
        this.horizontal = true;
        return this;
    }

    public GridLayout<T> vertical() {
        this.horizontal = false;
        return this;
    }
}
