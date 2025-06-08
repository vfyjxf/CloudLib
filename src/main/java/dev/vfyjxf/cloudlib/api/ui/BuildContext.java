package dev.vfyjxf.cloudlib.api.ui;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public sealed interface BuildContext<T extends WidgetGroup<E>, E extends Widget> extends Scope<E> {

    static <E extends Widget> BuildContext<? extends WidgetGroup<E>, E> create() {
        return new Impl<>(null);
    }

    static <E extends Widget> BuildContext<? extends WidgetGroup<E>, E> create(@Nullable BuildVisitor visitor) {
        return new Impl<>(visitor);
    }

    @SuppressWarnings("unchecked")
    static <T extends Widget & Group<E>, E extends Widget> T buildFor(GroupSpec<T, E> spec, @Nullable BuildVisitor visitor) {
        Impl<E> scope = new Impl<>(visitor);
        return (T) scope.build((GroupSpec<WidgetGroup<E>, E>) spec);
    }


    T build(GroupSpec<T, E> spec);

    interface BuildVisitor {

        <T extends Widget> void push(ScopeEntry<T> entry, T widget);

        <R extends Widget & Group<E>, E extends Widget> void next(GroupSpec<R, E> spec, R constructing);

        void up();
    }

    sealed interface ScopeEntry<E extends Widget> {}

    record WidgetEntry<E extends Widget>(E widget) implements ScopeEntry<E> {}

    record WidgetSpecEntry<E extends Widget>(WidgetSpec<? extends E> spec)
            implements ScopeEntry<E> {}

    record GroupSpecEntry<R extends Widget & Group<T>, T extends Widget>(
            GroupSpec<R, T> spec
    ) implements ScopeEntry<R> {}

}

final class Impl<E extends Widget> implements BuildContext<WidgetGroup<E>, E> {

    private final MutableList<ScopeEntry<? extends E>> entries = Lists.mutable.empty();
    private final @Nullable BuildVisitor visitor;

    Impl(@Nullable BuildVisitor visitor) {this.visitor = visitor;}

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
    private void applyEntry(WidgetGroup<E> group, ScopeEntry<? extends E> entry) {
        var constructing = switch (entry) {
            case WidgetEntry<? extends E>(var element) -> group.addWidget(element);
            case WidgetSpecEntry<? extends E> specEntry -> group.addWidget(specEntry.spec().construct());
            case GroupSpecEntry<?, ?> specEntry -> {
                //can't represent the type of E & Group<T> here, so we use wildcard
                E applied = (E) BuildContext.buildFor(specEntry.spec(), visitor);
                yield group.addWidget(applied);
            }
        };
        if (visitor != null) {visitor.push((ScopeEntry<? super E>) entry, constructing);}
    }


    @Override
    public void apply(WidgetSpec<? extends E> spec) {
        entries.add(new WidgetSpecEntry<>(spec));
    }

    @Override
    public void apply(E widget) {
        entries.add(new WidgetEntry<>(widget));
    }

    @Override
    public void apply(Collection<? extends E> widgets) {
        for (E widget : widgets) {
            apply(widget);
        }
    }

    @Override
    public <W extends Widget> void group(GroupSpec<? extends E, W> spec) {
        entries.add(new GroupSpecEntry<>(spec));
    }

}


