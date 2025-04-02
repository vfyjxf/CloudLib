package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.performer.Backstage;
import dev.vfyjxf.cloudlib.api.performer.PerformerContainer;
import dev.vfyjxf.cloudlib.api.ui.UIContext;

/**
 * A special widget that is the root of the widget tree.
 */
//TODO:make root widget properties to be immutable to prevent modification from other
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
    public PerformerContainer performers() {
        return performers;
    }
}
