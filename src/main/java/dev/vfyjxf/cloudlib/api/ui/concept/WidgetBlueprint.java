package dev.vfyjxf.cloudlib.api.ui.concept;

import dev.vfyjxf.cloudlib.api.ui.state.State;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;

import java.util.function.Supplier;

/**
 * Where is widget,and how to create it.
 */
public class WidgetBlueprint {

    private Lifecycle lifecycle = Lifecycle.NOMADIC;

    /**
     * The material to create the widget.
     */
    private State state;
    private final Supplier<State> stateFactory;

    private final WidgetFactory<State, Widget> factory;
    private Widget currentInstance;
    private boolean outdated = false;

    @SuppressWarnings("unchecked")
    public <S extends State, W extends Widget> WidgetBlueprint(
            Supplier<S> stateFactory,
            WidgetFactory<S, W> factory
    ) {
        this.stateFactory = (Supplier<State>) stateFactory;
        this.factory = (WidgetFactory<State, Widget>) factory;
    }

    void construct() {
        if (state == null) {state = stateFactory.get();}
        currentInstance = factory.create(state);
        lifecycle = Lifecycle.ATTACHED;
    }

    void destroy() {
        if (currentInstance != null) {
            currentInstance = null;
        }
        lifecycle = Lifecycle.DETACHED;
    }

    public boolean outdated() {
        return outdated;
    }

    public void stateChanged(Runnable callback) {
        callback.run();
        this.outdated = true;
    }
}
