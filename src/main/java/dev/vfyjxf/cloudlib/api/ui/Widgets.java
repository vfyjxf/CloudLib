package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.ui.widgets.BasicWidget;

public class Widgets {

    public static Widget create() {
        return new BasicWidget();
    }

    private Widgets() {
    }

}
