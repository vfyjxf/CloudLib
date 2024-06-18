package dev.vfyjxf.cloudlib.api.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.Constraints;
import dev.vfyjxf.cloudlib.api.ui.layout.Layoutable;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;

public interface ILayoutTraitNode {

    Constraints measure(Layoutable Layoutable);

    void apply(IWidget widget);

}
