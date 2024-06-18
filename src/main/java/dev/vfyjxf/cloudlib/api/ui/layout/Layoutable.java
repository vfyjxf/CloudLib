package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.Constraints;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

public interface Layoutable extends IntrinsicLayoutable {

    IWidget layout(Constraints constraints);

}
