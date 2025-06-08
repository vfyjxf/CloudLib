package dev.vfyjxf.cloudlib.api.ui;

public interface WidgetSpec<T extends Widget> {

    T construct();

}
