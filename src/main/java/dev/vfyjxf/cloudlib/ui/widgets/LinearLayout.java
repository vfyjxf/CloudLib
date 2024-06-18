package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

//TODO:
public class LinearLayout<T extends IWidget> extends WidgetGroup<T> {

    private boolean horizontal;

    public LinearLayout() {

    }


    public LinearLayout<T> horizontal() {
        this.horizontal = true;
        return this;
    }

    public LinearLayout<T> vertical() {
        this.horizontal = false;
        return this;
    }
}
