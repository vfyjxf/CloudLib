package dev.vfyjxf.cloudlib.api.ui.concept;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

//TODO:重新设计WidgetGroup的Api
public class CWidgetGroup<T extends Widget> extends Widget {

    private final MutableList<T> children = Lists.mutable.empty();
    private final MutableList<T> childrenView = children.asUnmodifiable();

    private final MutableList<T> toAdd = Lists.mutable.empty();
    private final MutableList<T> toRemove = Lists.mutable.empty();


}
