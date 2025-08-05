package dev.vfyjxf.cloudlib.api.ui.base;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * The Spec of Spec :P
 */
public interface PlanFragment<T extends Widget> {

    static <T extends Widget> PlanFragment<T> create() {
        return new PlanFragmentImpl<>();
    }

    void applyTo(Plan<T> plan);

    void extend(Consumer<PlanConfigure<T>> configure);

    sealed interface PlanConfigure<E extends Widget> {

        void include(WidgetSpec<? extends E> spec);

        void include(E widget);

        void include(Collection<? extends E> widgets);

        <T extends Widget> void group(GroupSpec<? extends E, T> spec);
    }

}

final class PlanFragmentImpl<T extends Widget> implements PlanFragment<T> {

    private final MutableList<PlanConfigurator<T>> configurators = Lists.mutable.empty();

    @SuppressWarnings("unchecked")
    @Override
    public void applyTo(Plan<T> plan) {
        for (var configurator : configurators) {
            for (var entry : configurator.entries) {
                switch (entry) {
                    case ConstructiblePlan.WidgetEntry<? extends T> widgetEntry -> plan.include(widgetEntry.widget());
                    case ConstructiblePlan.WidgetSpecEntry<? extends T> widgetSpecEntry ->
                            plan.include(widgetSpecEntry.spec());
                    case ConstructiblePlan.GroupSpecEntry<?, ?> groupSpecEntry ->
                            plan.group((GroupSpec<? extends T, ? extends Widget>) groupSpecEntry.spec());
                }
            }
        }
    }

    @Override
    public void extend(Consumer<PlanConfigure<T>> configure) {
        PlanConfigurator<T> configurator = new PlanConfigurator<>();
        configure.accept(configurator);
        configurators.add(configurator);
    }
}

final class PlanConfigurator<E extends Widget> implements PlanFragment.PlanConfigure<E> {

    final MutableList<ConstructiblePlan.PlanEntry<? extends E>> entries = Lists.mutable.empty();

    @Override
    public void include(WidgetSpec<? extends E> spec) {
        entries.add(new ConstructiblePlan.WidgetSpecEntry<>(spec));
    }

    @Override
    public void include(E widget) {
        entries.add(new ConstructiblePlan.WidgetEntry<>(widget));
    }

    @Override
    public void include(Collection<? extends E> widgets) {
        for (E widget : widgets) {
            include(widget);
        }
    }

    @Override
    public <T extends Widget> void group(GroupSpec<? extends E, T> spec) {
        entries.add(new ConstructiblePlan.GroupSpecEntry<>(spec));
    }
}
