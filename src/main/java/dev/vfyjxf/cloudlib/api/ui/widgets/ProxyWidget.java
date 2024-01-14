package dev.vfyjxf.cloudlib.api.ui.widgets;

public abstract class ProxyWidget {

    public final IWidget child;

    protected ProxyWidget(IWidget child) {
        this.child = child;
    }
}
