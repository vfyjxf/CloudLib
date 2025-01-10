package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.actor.ActorContainer;
import dev.vfyjxf.cloudlib.api.actor.ActorHolder;
import dev.vfyjxf.cloudlib.api.ui.UIContext;

/**
 * A special widget that is the root of the widget tree.
 */
public final class RootWidget extends WidgetGroup<Widget> implements ActorHolder {

    private final ActorContainer actors = new ActorContainer();
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

    @Override
    public ActorContainer actors() {
        return actors;
    }
}
