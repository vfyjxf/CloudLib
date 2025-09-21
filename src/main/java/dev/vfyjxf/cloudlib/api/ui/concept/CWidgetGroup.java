package dev.vfyjxf.cloudlib.api.ui.concept;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.list.MutableList;

//TODO:重新设计WidgetGroup的Api
public class CWidgetGroup<T extends Widget> extends Widget {

    private final MutableList<T> children = MutableLists.empty();
    private final MutableList<T> childrenView = children.asUnmodifiable();

    private final MutableList<T> toAdd = MutableLists.empty();
    private final MutableList<T> toRemove = MutableLists.empty();


}
