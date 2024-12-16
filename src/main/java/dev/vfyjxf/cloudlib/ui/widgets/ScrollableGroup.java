package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.UISide;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public class ScrollableGroup<T extends Widget> extends WidgetGroup<T> {

    private UISide scrollBarSide = UISide.LEFT;

    public ScrollableGroup<T> setScrollBarSide(UISide scrollBarSide) {
        this.scrollBarSide = scrollBarSide;
        return this;
    }

}
