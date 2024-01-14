package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;

public class LinearLayout<T extends IWidget> extends WidgetGroup<T> {

    private boolean horizontal;

    public LinearLayout() {

    }

    @Override
    public IWidgetGroup<T> add(int index, T widget) {
        super.add(index, widget);
        if (horizontal) {
            int width = 0;
            for (T child : children()) {
                width += child.getWidth();
            }
            widget.setX(width + 1);
        } else {
            int height = 0;
            for (T child : children()) {
                height += child.getHeight();
            }
            widget.setY(height + 1);
        }
        return this;
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
