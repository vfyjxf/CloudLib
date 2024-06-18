package dev.vfyjxf.cloudlib.api.ui.layout;

public interface LayoutResult extends Runnable {

    /**
     * apply the layout result to the widgets.
     */
    @Override
    void run();
}
