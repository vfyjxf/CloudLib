package dev.vfyjxf.cloudlib.api.ui.base;

public class WidgetGroup<T extends Widget> extends CompositeWidget<T> {

    @Override
    public <W extends T> W addWidget(W widget) {
        return super.addWidget(widget);
    }

    @Override
    public boolean remove(Widget widget) {
        return super.remove(widget);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
