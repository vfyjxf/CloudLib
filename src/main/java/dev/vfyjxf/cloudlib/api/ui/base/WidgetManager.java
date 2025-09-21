package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.state.State;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

final class WidgetManager {

    private final GroupSpec<RootWidget, Widget> rootSpec;
    private final GroupNode rootNode;
    private final StateManager stateManager;

    public WidgetManager(GroupSpec<RootWidget, Widget> rootSpec) {
        Objects.requireNonNull(rootSpec, "Root spec cannot be null");
        this.rootSpec = rootSpec;
        this.rootNode = new GroupNode(
            rootSpec,
            Sets.immutable.empty(),
            MutableLists.empty()
        );
        this.stateManager = new StateManager(rootNode);
    }

    RootWidget rootWidget() {
        return Objects.requireNonNull(maybeRootWidget());
    }

    @Nullable RootWidget maybeRootWidget() {
        return rootNode.<RootWidget>specAndInstance().instance;
    }

    void rebuildRequired() {
        RootWidget rootWidget = maybeRootWidget();
        if (rootWidget == null) {
            SpecTreeBuilder builder = new SpecTreeBuilder(rootNode);
            rootNode.specAndInstance().instance = ConstructiblePlan.buildFor(rootSpec, builder);
        } else {

        }
    }

    static class SpecTreeBuilder implements ConstructiblePlan.BuildVisitor {

        public SpecTreeBuilder() {
            start = null;
        }

        public <R extends Group<T>, T extends Widget> SpecTreeBuilder(GroupNode start) {
            this.start = start;
        }

        private final GroupNode start;

        private final Deque<GroupNode> groupQueue = new ArrayDeque<>();

        @Override
        public <T extends Widget> void push(ConstructiblePlan.PlanEntry<T> entry, T widget) {
            var groupNode = groupQueue.peek();
            Objects.requireNonNull(groupNode, "GroupNode stack should not be empty when pushing an entry");
            switch (entry) {
                case ConstructiblePlan.WidgetEntry<?>(var instance) ->
                    // Handle constant widget instance
                    groupNode.children().add(new ConstantNode(instance));
                case ConstructiblePlan.WidgetSpecEntry(var spec) -> {
//                    var widgetStates = SpecUtils.findSpecState(spec);
                    groupNode.children().add(new ElementNode(spec, Sets.immutable.empty(), widget));
                }
                //handled by "next" method
                case ConstructiblePlan.GroupSpecEntry<?, ?> ignored -> {}
            }
        }

        @Override
        public <R extends Widget & Group<E>, E extends Widget> void next(GroupSpec<R, E> spec, R constructing) {
            if (groupQueue.isEmpty() && start != null && start.spec() == spec) {
                SpecAndInstance<R> specAndInstance = start.specAndInstance();
                specAndInstance.instance = constructing;
                groupQueue.push(start);
            } else {
                var node = new GroupNode(spec,
                    Sets.immutable.empty(),
                    MutableLists.empty(),
                    constructing);
                GroupNode parent = groupQueue.peek();
                if (parent != null) parent.children().add(node);
                groupQueue.push(node);
            }
        }

        @Override
        public void up() {
            if (!groupQueue.isEmpty()) {
                //size == 1 as RootNode
                if (groupQueue.size() != 1) groupQueue.pop();
            } else throw new IllegalStateException("GroupNode Queue is empty, cannot pop.");
        }

    }

    sealed interface SpecNode {}

    record ConstantNode(Widget instance) implements SpecNode {
    }

    static final class ElementNode implements SpecNode {
        private final WidgetSpec<? extends Widget> spec;
        private final ImmutableSet<? extends State> states;
        private Widget instance;

        private ElementNode(
            WidgetSpec<? extends Widget> spec,
            ImmutableSet<? extends State> states,
            Widget widget
        ) {
            this.spec = spec;
            this.states = states;
            this.instance = widget;
        }

        public WidgetSpec<?> spec() {return spec;}

        public ImmutableSet<? extends State> states() {return states;}

        public Widget instance() {return instance;}

        public void setInstance(Widget instance) {
            this.instance = instance;
        }

    }

    static final class GroupNode implements SpecNode {
        private final SpecAndInstance<?> specAndInstance;
        private final ImmutableSet<? extends State> states;
        private final MutableList<SpecNode> children;

        <R extends Widget & Group<E>, E extends Widget> GroupNode(
            GroupSpec<R, ? extends E> spec,
            ImmutableSet<? extends State> states,
            MutableList<SpecNode> children,
            @Nullable R instance
        ) {
            this.specAndInstance = new SpecAndInstance<>(spec, instance);
            this.states = states;
            this.children = children;
        }

        <R extends Widget & Group<E>, E extends Widget> GroupNode(
            GroupSpec<R, ? extends E> spec,
            ImmutableSet<? extends State> states,
            MutableList<SpecNode> children
        ) {
            this(spec, states, children, null);
        }

        @SuppressWarnings("unchecked")
        <R extends Widget & Group<?>> SpecAndInstance<R> specAndInstance() {
            return (SpecAndInstance<R>) specAndInstance;
        }

        GroupSpec<?, ?> spec() {return specAndInstance.spec;}

        ImmutableSet<? extends State> states() {return states;}

        MutableList<SpecNode> children() {return children;}

    }

    static final class SpecAndInstance<T extends Widget & Group<?>> {
        private final GroupSpec<T, ? extends Widget> spec;
        private @Nullable T instance;

        SpecAndInstance(GroupSpec<T, ? extends Widget> spec, @Nullable T instance) {
            this.spec = spec;
            this.instance = instance;
        }
    }

}

