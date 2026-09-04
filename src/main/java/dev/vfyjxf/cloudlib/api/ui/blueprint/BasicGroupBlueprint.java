package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.eclipse.collections.api.list.MutableList;

import java.util.function.Supplier;

public abstract class BasicGroupBlueprint<T extends Widget> implements Blueprint.Group<CompositeWidget<T>, T> {

    protected final Supplier<? extends MutableList<? extends Blueprint<T>>> childrenSupplier;

    protected BasicGroupBlueprint(Supplier<? extends MutableList<? extends Blueprint<T>>> childrenSupplier) {
        this.childrenSupplier = childrenSupplier;
    }

    @Override
    public MutableList<? extends Blueprint<T>> children() {
        return childrenSupplier.get();
    }
}
