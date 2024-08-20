package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.Constraints;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

public interface ILayoutTraitNode {

    LayoutResult layout(IWidget measurable, Constraints constraints);

}
