package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.alignment.Alignment;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public abstract class AutomaticResizer<T extends AutomaticResizer<T>> implements Resizer, ParentResizer {
    protected WidgetGroup<? extends Widget> group;
    //TODO:remove it,use widget Margin and Padding layout
    protected int margin;
    protected int padding;
    protected int height;

    protected Alignment alignment;
    protected Alignment.Horizontal horizontalAlignment;
    protected Alignment.Vertical verticalAlignment;

    public AutomaticResizer(WidgetGroup<? extends Widget> group) {
        this.group = group;
        this.setup();
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    /* Standard properties */

    public T margin(int margin) {
        this.margin = margin;
        return self();
    }

    public T padding(int padding) {
        this.padding = padding;
        return self();
    }

    public T height(int height) {
        this.height = height;
        return self();
    }

    public T alignment(Alignment alignment) {
        this.alignment = alignment;
        return self();
    }

    public T horizontalAlignment(Alignment.Horizontal horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        return self();
    }

    public T verticalAlignment(Alignment.Vertical verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        return self();
    }

    /* Child management */

    public void setup() {
        for (Widget child : this.group.children()) {
            child.resizer(this.child(child));
        }
    }

    public Resizer child(Widget widget) {
        return new ChildResizer(this, widget);
    }

    public MutableList<ChildResizer> getResizers() {
        MutableList<ChildResizer> resizers = Lists.mutable.empty();
        for (Widget widget : this.group.children()) {
            if (widget.resizer() instanceof ChildResizer resizer) {
                resizers.add(resizer);
            }
        }

        return resizers;
    }

    /* Miscellaneous */

    @Override
    public void add(WidgetGroup<?> parent, Widget child) {
        child.resizer(this.child(child));
    }

    @Override
    public void remove(WidgetGroup<?> parent, Widget child) {
        Resizer resizer = child.resizer();

        if (resizer instanceof ChildResizer childResizer) {
            child.resizer(childResizer.flex());
        }
    }

    protected boolean posXDefined() {
        return !this.group.flex().x().isUndefined();
    }

    protected boolean posYDefined() {
        return !this.group.flex().y().isUndefined();
    }

    protected boolean widthDefined() {
        return !this.group.flex().width().isUndefined();
    }

    protected boolean heightDefined() {
        return !this.group.flex().height().isUndefined();
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}