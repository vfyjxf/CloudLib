package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.state.MutableState;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.collector.Collectors2;
import org.eclipse.collections.impl.factory.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public sealed interface ConstructiblePlan<T extends WidgetGroup<E>, E extends Widget> extends Plan<E> {

    //region factories

    static <E extends Widget> ConstructiblePlan<? extends WidgetGroup<E>, E> create() {
        return new PlanImpl<>(new BuildContext(), null);
    }

    static <E extends Widget> ConstructiblePlan<? extends WidgetGroup<E>, E> create(@Nullable BuildVisitor visitor) {
        return new PlanImpl<>(new BuildContext(), visitor);
    }

    static <T extends Widget & Group<E>, E extends Widget> T buildFor(GroupSpec<T, E> spec, @Nullable BuildVisitor visitor) {
        return buildFor(spec, new BuildContext(), visitor);
    }

    @SuppressWarnings("unchecked")
    static <T extends Widget & Group<E>, E extends Widget> T buildFor(GroupSpec<T, E> spec, BuildContext context, @Nullable BuildVisitor visitor) {
        PlanImpl<E> plan = new PlanImpl<>(context, visitor);
        return (T) plan.build((GroupSpec<WidgetGroup<E>, E>) spec);
    }

    //endregion

    MutableSet<MutableState<?>> requiredStates();

    T build(GroupSpec<T, E> spec);

    interface BuildVisitor {

        <T extends Widget> void push(PlanEntry<T> entry, T widget);

        <R extends Widget & Group<E>, E extends Widget> void next(GroupSpec<R, E> spec, R constructing);

        void up();
    }

    //region entries

    sealed interface PlanEntry<E extends Widget> {}

    record WidgetEntry<E extends Widget>(E widget) implements PlanEntry<E> {}

    record WidgetSpecEntry<E extends Widget>(WidgetSpec<? extends E> spec)
            implements PlanEntry<E> {}

    record GroupSpecEntry<R extends Widget & Group<T>, T extends Widget>(
            GroupSpec<R, T> spec
    ) implements PlanEntry<R> {}

    //endregion

}

final class PlanImpl<E extends Widget> implements ConstructiblePlan<WidgetGroup<E>, E> {

    private final BuildContext context;
    private final Set<Consumer<StateUsage>> stateUsages = new ObjectLinkedOpenHashSet<>();
    private final MutableList<PlanEntry<? extends E>> entries = Lists.mutable.empty();
    private final @Nullable BuildVisitor visitor;

    PlanImpl(BuildContext context, @Nullable BuildVisitor visitor) {
        this.context = context;
        this.visitor = visitor;
    }

    @Override
    public MutableSet<MutableState<?>> requiredStates() {
        var collector = new StateCollector();

        for (var stateUsage : stateUsages) {
            stateUsage.accept(collector);
        }

        return collector.states
                .stream()
                .collect(Collectors2.toSet());
    }

    @Override
    public WidgetGroup<E> build(GroupSpec<WidgetGroup<E>, E> spec) {
        var constructing = spec.construct(this);
        if (visitor != null) {visitor.next(spec, constructing);}
        for (var entry : entries) {
            applyEntry(constructing, entry);
        }
        if (visitor != null) {visitor.up();}
        return constructing;
    }

    @SuppressWarnings("unchecked")
    private void applyEntry(WidgetGroup<E> group, PlanEntry<? extends E> entry) {
        var constructing = switch (entry) {
            case WidgetEntry<? extends E>(var element) -> group.addWidget(element);
            case WidgetSpecEntry<? extends E> specEntry -> group.addWidget(specEntry.spec().construct(context));
            case GroupSpecEntry<?, ?> specEntry -> {
                //can't represent the type of E & Group<T> here, so we use wildcard
                E applied = (E) ConstructiblePlan.buildFor(specEntry.spec(), context, visitor);
                yield group.addWidget(applied);
            }
        };
        if (visitor != null) {visitor.push((PlanEntry<? super E>) entry, constructing);}
    }


    @Override
    public BuildContext context() {
        return context;
    }

    @Override
    public void use(Consumer<StateUsage> usage) {
        stateUsages.add(usage);
    }

    @Override
    public void include(WidgetSpec<? extends E> spec) {
        entries.add(new WidgetSpecEntry<>(spec));
    }

    @Override
    public void include(E widget) {
        entries.add(new WidgetEntry<>(widget));
    }

    @Override
    public void include(Collection<? extends E> widgets) {
        for (E widget : widgets) {
            include(widget);
        }
    }

    @Override
    public <W extends Widget> void group(GroupSpec<? extends E, W> spec) {
        entries.add(new GroupSpecEntry<>(spec));
    }

}


