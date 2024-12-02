package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public abstract class BasicResizer implements Resizer, ParentResizer {

    @Override
    public void apply(Widget widget, Resizer resizer, ChildResizer child) {

    }

    @Override
    public void preApply(Widget widget) {

    }

    @Override
    public void apply(Widget widget) {

    }

    @Override
    public void postApply(Widget widget) {

    }

    @Override
    public void add(WidgetGroup<?> parent, Widget child) {
    }

    @Override
    public void remove(WidgetGroup<?> parent, Widget child) {
    }
}