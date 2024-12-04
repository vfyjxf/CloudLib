package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class AutomaticResizer extends BasicResizer {
    public WidgetGroup<? extends Widget> parent;
    public int margin;
    public int padding;
    public int height;

    public AutomaticResizer(WidgetGroup<? extends Widget> parent, int margin) {
        this.parent = parent;
        this.margin = margin;

        this.setup();
    }

    /* Standard properties */

    public AutomaticResizer padding(int padding) {
        this.padding = padding;

        return this;
    }

    public AutomaticResizer height(int height) {
        this.height = height;

        return this;
    }

    /* Child management */

    public void setup() {
        for (Widget child : this.parent.children()) {
            child.resizer(this.child(child));
        }
    }

    public Resizer child(Widget widget) {
        return new ChildResizer(this, widget);
    }

    public List<ChildResizer> getResizers() {
        List<ChildResizer> resizers = new ArrayList<>();


        for (Widget widget : this.parent.children()) {
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
            child.resizer(childResizer.resizer);
        }
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