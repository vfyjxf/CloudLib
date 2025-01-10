package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.UISide;
import dev.vfyjxf.cloudlib.api.ui.layout.RowResizer;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public class ScrollableGroup<T extends Widget> extends WidgetGroup<T> {

    private UISide scrollBarSide = UISide.LEFT;
    private boolean scrollBarVisible = true;

    public ScrollableGroup<T> setScrollBarSide(UISide scrollBarSide) {
        this.scrollBarSide = scrollBarSide;
        return this;
    }

    public boolean shouldRenderScrollBar() {
        boolean outOfBounds = false;
        if (horizontal()) {
            int maxBound = childrenView.summarizeInt(widget -> widget.getBounds().right()).getMax();
            outOfBounds = maxBound > getWidth();
        } else {
            int maxBound = childrenView.summarizeInt(widget -> widget.getBounds().bottom()).getMax();
            outOfBounds = maxBound > getHeight();
        }
        return outOfBounds;
    }


    public boolean horizontal() {
        return this.resizer instanceof RowResizer;
    }

}
