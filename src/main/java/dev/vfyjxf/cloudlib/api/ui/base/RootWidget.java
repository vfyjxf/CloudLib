package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.performer.Backstage;
import dev.vfyjxf.cloudlib.api.performer.PerformerContainer;
import dev.vfyjxf.cloudlib.api.ui.UIContext;

/**
 * A special widget that is the root of the widget tree.
 */
public final class RootWidget extends WidgetGroup<Widget> implements Backstage {

    private final PerformerContainer performers = new PerformerContainer();
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
    public UIContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "RootWidget{" +
                       "context=" + context +
                       ", children=" + children() +
                       ", key='" + (key == null ? "null" : key) + '\'' +
                       ", position=" + position +
                       ", absolute=" + absolute +
                       ", initialized=" + initialized +
                       ", size=" + size +
                       ", active=" + active +
                       ", visibility=" + visibility +
                       '}';
    }

    @Override
    public PerformerContainer performers() {
        return performers;
    }

    public void tick() {
        //TODO:Decide whether to keep this method
        //rebuild/reform widget tree
        //ticking widget,for timer widget or something else
        //tick-end:update snapshot state
    }

    @Override
    protected Widget addWidget(Widget widget) {
        if (this.add(children().size(), widget)) {
            widget.setParent(this);
            widget.onPositionUpdate();
        } else {
            throw new IllegalStateException("Widget " + widget + " cannot be added to RootWidget");
        }
        return widget;
    }
}
