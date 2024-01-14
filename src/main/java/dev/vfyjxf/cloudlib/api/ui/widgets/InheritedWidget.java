package dev.vfyjxf.cloudlib.api.ui.widgets;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;

public abstract class InheritedWidget extends ProxyWidget {

    private final MutableMap<Element, Object> dependencies = Maps.mutable.empty();

    protected InheritedWidget(IWidget child) {
        super(child);
    }

    @Nullable
    protected Object getDependency(Element element) {
        return dependencies.get(element);
    }

    protected void setDependency(Element element, Object dependency) {
        dependencies.put(element, dependency);
    }

}
