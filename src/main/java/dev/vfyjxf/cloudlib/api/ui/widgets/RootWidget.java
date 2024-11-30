package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.UIContext;

public class RootWidget extends WidgetGroup<Widget> {

    private UIContext context;

    public RootWidget() {
        this.root = this;
    }

    @Override
    public void init() {
        this.context = UIContext.current();
        super.init();
    }

    @Override
    public String toString() {
        return "RootWidget{" +
                "context=" + context +
                ", children=" + children +
                ", id='" + id + '\'' +
                ", position=" + position +
                ", absolute=" + absolute +
                ", initialized=" + initialized +
                ", size=" + size +
                ", active=" + active +
                ", visibility=" + visibility +
                '}';
    }
}
