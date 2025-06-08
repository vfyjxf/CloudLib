package dev.vfyjxf.cloudlib.api.ui;

import java.util.Collection;

public sealed interface Scope<E extends Widget> permits BuildContext {

    void apply(WidgetSpec<? extends E> spec);

    void apply(E widget);

    void apply(Collection<? extends E> widgets);

    <T extends Widget> void group(GroupSpec<? extends E, T> spec);

}
