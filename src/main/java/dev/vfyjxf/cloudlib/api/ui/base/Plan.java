package dev.vfyjxf.cloudlib.api.ui.base;

import java.util.Collection;
import java.util.function.Consumer;

public sealed interface Plan<E extends Widget> permits ConstructiblePlan {

    BuildContext context();

    void use(Consumer<StateUsage> usage);

    void include(WidgetSpec<? extends E> spec);

    void include(E widget);

    void include(Collection<? extends E> widgets);

    <T extends Widget> void group(GroupSpec<? extends E, T> spec);

}
