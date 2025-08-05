package dev.vfyjxf.cloudlib.api.ui.base;

import java.util.function.Function;

public interface GroupDef<T extends Widget> extends GroupSpec<WidgetGroup<T>, T> {
    static <T extends Widget> GroupDef<T> of(Function<Plan<T>, WidgetGroup<T>> configurator) {
        return configurator::apply;
    }
}
