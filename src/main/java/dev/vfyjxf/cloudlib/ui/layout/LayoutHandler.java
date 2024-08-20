package dev.vfyjxf.cloudlib.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

public class LayoutHandler {

    private final IWidget owner;
    private boolean attached = false;

    public LayoutHandler(IWidget owner) {
        this.owner = owner;
    }

    public boolean attached() {
        return attached;
    }

    public void attach() {
        attached = true;
    }

}
