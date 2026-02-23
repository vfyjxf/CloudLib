package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.*;
import org.eclipse.collections.api.list.MutableList;

import java.util.function.Supplier;

public final class GroupBlueprint<T extends Widget> extends BasicGroupBlueprint<T> {
    GroupBlueprint(Supplier<? extends MutableList<? extends Blueprint<T>>> childrenSupplier) {
        super(childrenSupplier);
    }

    @Override
    public CompositeWidget<T> createWidget(Scene scene, SceneContext context) {
        return new CompositeWidget<>();
    }

    @Override
    public void updateWidget(CompositeWidget<T> widget, Scene scene, SceneContext context) {

    }
}
