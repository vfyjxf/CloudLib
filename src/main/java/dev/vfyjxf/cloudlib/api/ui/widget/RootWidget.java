package dev.vfyjxf.cloudlib.api.ui.widget;

import dev.vfyjxf.cloudlib.api.performer.Backstage;
import dev.vfyjxf.cloudlib.api.performer.PerformerContainer;
import dev.vfyjxf.cloudlib.api.ui.UIContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * A special widget that is the root of the widget tree.
 */
public final class RootWidget extends WidgetGroup<Widget> implements Backstage {

    private final PerformerContainer performers = new PerformerContainer();
    private UIContext context;

    @ApiStatus.Internal
    WidgetManager manager = new WidgetManager(this);

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

    public void tick() {
        //rebuild/reform widget tree
        //ticking widget,for timer widget or something else
        //tick-end:update snapshot state
    }
}
