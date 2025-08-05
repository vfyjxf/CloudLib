package dev.vfyjxf.cloudlib.api.ui.base;

public interface WidgetSpec<T extends Widget> {

    T construct(BuildContext context);

}
