package dev.vfyjxf.cloudlib.api.ui.modular;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;

public interface IModularScreen {

    IModularUI getUI();

    default IWidgetGroup<IWidget> getMainGroup() {
        return getUI().getMainGroup();
    }

}
