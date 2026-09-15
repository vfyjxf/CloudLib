package dev.vfyjxf.cloudlib.api.ui.base;

import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable UI description. Blueprint configures Widget via updateWidget().
 */
public interface Blueprint<T extends Widget> {

    /**
     * Key for reconciliation. Null means match by position.
     */
    @Nullable
    default Object key() {
        return null;
    }

    T createWidget(Scene scene, SceneContext context);

    /**
     * Apply configuration to the widget. Called on mount and update.
     */
    void updateWidget(T widget, Scene scene, SceneContext context);


    /**
     * Container node with children. e.g. VStack, HStack.
     */
    interface Group<T extends CompositeWidget<E>, E extends Widget> extends Blueprint<T> {
        MutableList<? extends Blueprint<E>> children();
    }
}
