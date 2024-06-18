package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.UISide;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.math.Dimension;

public class Toolbar<T extends IWidget> extends LinearLayout<T> {

    private UISide side = UISide.LEFT;
    private boolean fixedSize = true;
    private int length = 10;


    public Toolbar<T> side(UISide side) {
        this.side = side;
        return this;
    }

    @Override
    public Toolbar<T> setSize(Dimension size) {
        if (fixedSize) return this;
        super.setSize(size);
        return this;
    }

    public Toolbar<T> width(int width) {
        this.length = width;
        return this;
    }


    public Toolbar<T> fixedSize() {
        this.fixedSize = true;
        return this;
    }

    public Toolbar<T> setFixedSize(boolean fixedSize) {
        this.fixedSize = fixedSize;
        return this;
    }

}
