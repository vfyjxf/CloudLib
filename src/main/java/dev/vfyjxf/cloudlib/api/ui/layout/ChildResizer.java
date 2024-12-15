package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public class ChildResizer implements Resizer, ParentResizer {
    private final Flex flex;
    public ParentResizer parent;
    public Widget widget;
    private int x;
    private int y;
    private int w;
    private int h;

    public ChildResizer(ParentResizer parent, Widget widget) {
        this.flex = widget.flex();
        this.parent = parent;
        this.widget = widget;
    }

    public Flex flex() {
        return flex;
    }

    @Override
    public void apply(Widget widget) {
        if (this.flex != null) {
            this.flex.apply(widget);
        }
        this.parent.apply(widget, flex, this);
        this.x = widget.posX();
        this.y = widget.posY();
        this.w = widget.getWidth();
        this.h = widget.getHeight();
    }

    @Override
    public void postApply(Widget widget) {
        if (flex != null) {
            flex.postApply(widget);
        }
    }

    @Override
    public void add(WidgetGroup<?> parent, Widget child) {
        if (this.flex != null) {
            this.flex.add(parent, child);
        }
    }

    @Override
    public void remove(WidgetGroup<?> parent, Widget child) {
        if (this.flex != null) {
            this.flex.remove(parent, child);
        }
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.w;
    }

    @Override
    public int getHeight() {
        return this.h;
    }
}
