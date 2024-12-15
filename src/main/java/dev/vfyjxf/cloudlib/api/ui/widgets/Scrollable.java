package dev.vfyjxf.cloudlib.api.ui.widgets;

//TODO:implement scrollable
public interface Scrollable<W extends WidgetGroup<?> & Scrollable<W>> {

    ScrollType scrollType();

    ScrollSide scrollSide();

    void scroll(int amount);

    void scrollUp();

    void scrollDown();

    W setScroll(int amount);

    W setDelta(int delta);

    enum ScrollType {
        VERTICAL, HORIZONTAL
    }

    enum ScrollSide {
        LEFT, RIGHT, TOP, BOTTOM
    }
}