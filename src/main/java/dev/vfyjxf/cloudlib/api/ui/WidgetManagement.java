package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.state.State;
import net.lenni0451.reflect.stream.RStream;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.stream.Collectors;

//TODO:自底向上的变更检查和更新
final class WidgetManagement {

    private final GroupNode<RootWidget> rootNode;
    private final RootWidget rootWidget;

    @SuppressWarnings("unchecked")
    public WidgetManagement(GroupSpec<RootWidget, Widget> rootSpec) {
        var builder = new SpecTreeBuilder();
        this.rootWidget = BuildContext.buildFor(rootSpec, builder);
        this.rootNode = (GroupNode<RootWidget>) builder.groupQueue.pop();
    }

    private static class SpecTreeBuilder implements BuildContext.BuildVisitor {

        private final Deque<GroupNode<?>> groupQueue = new ArrayDeque<>();

        @Override
        public <T extends Widget> void push(BuildContext.ScopeEntry<T> entry, T widget) {
            var groupNode = groupQueue.peek();
            Objects.requireNonNull(groupNode, "GroupNode stack should not be empty when pushing an entry");
            switch (entry) {
                case BuildContext.WidgetEntry<?>(var instance) ->
                    // Handle constant widget instance
                        groupNode.children().add(new ConstantNode(instance));
                case BuildContext.WidgetSpecEntry(var spec) -> {
                    var widgetStates = SpecUtils.findSpecState(spec);
                    groupNode.children().add(new ElementNode(spec, widgetStates.toImmutable(), widget));
                }
                case BuildContext.GroupSpecEntry<?, ?> ignored -> {}
            }
        }

        @Override
        public <R extends Widget & Group<E>, E extends Widget> void next(GroupSpec<R, E> spec, R constructing) {
            var node = new GroupNode<>(spec,
                    SpecUtils.findSpecState(spec).toImmutable(),
                    Lists.mutable.empty(),
                    constructing);
            GroupNode<?> parent = groupQueue.peek();
            if (parent != null) parent.children().add(node);
            groupQueue.push(node);
        }

        @Override
        public void up() {
            if (!groupQueue.isEmpty()) {
                if (groupQueue.size() != 1) {//size == 1 as RootNode
                    groupQueue.pop();
                }
            } else {
                throw new IllegalStateException("GroupNode Queue is empty, cannot pop.");
            }
        }
    }

    private sealed interface SpecNode {
        boolean shouldReconstruct();
    }

    private record ConstantNode(Widget instance) implements SpecNode {
        @Override
        public boolean shouldReconstruct() {return false;}
    }

    private static final class ElementNode implements SpecNode {
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

        public boolean shouldReconstruct() {
            boolean changed = false;
            for (State state : states) {
                changed |= state.changed();
            }
            return changed;
        }
    }

    private static final class GroupNode<T extends WidgetGroup<?>> implements SpecNode {
        private final GroupSpec<? extends T, ?> spec;
        private final ImmutableSet<? extends State> states;
        private final MutableList<SpecNode> children;
        private T instance;

        @SuppressWarnings("unchecked")
        private <R extends Widget & Group<E>, E extends Widget> GroupNode(
                GroupSpec<R, ? extends Widget> spec,
                ImmutableSet<? extends State> states,
                MutableList<SpecNode> children,
                R instance
        ) {
            //narrow R => T extends WidgetGroup<E>
            this.spec = (GroupSpec<? extends T, ?>) spec;
            this.states = states;
            this.children = children;
            this.instance = (T) instance;
        }

        public GroupSpec<?, ?> spec() {return spec;}

        public ImmutableSet<? extends State> states() {return states;}

        public MutableList<SpecNode> children() {return children;}

        public T instance() {return instance;}

        public void setInstance(T instance) {
            this.instance = instance;
        }

        public boolean shouldReconstruct() {
            boolean changed = false;
            for (State state : states) {
                //implNote:必须手动触发所有的状态变更检查，否则可能会导致意外重构
                changed |= state.changed();
            }
            return changed;
        }
    }

}

//TODO:支持更智能的State依赖关系分析
final class SpecUtils {

    public static MutableSet<? extends State> findSpecState(WidgetSpec<?> spec) {
        return findSpecStateInternal(spec);
    }

    public static MutableSet<? extends State> findSpecState(GroupSpec<?, ?> spec) {
        return findSpecStateInternal(spec);
    }

    private static MutableSet<? extends State> findSpecStateInternal(Object spec) {
        return findSpecStateInternal(spec, 3);
    }

    private static MutableSet<? extends State> findSpecStateInternal(Object spec, int maxLevel) {
        return findSpecStateInternal(spec, maxLevel, 0);
    }

    //TODO:支持复杂的类型套嵌（Spec套Spec,Spec套Function等）
    //TODO:支持更智能的State依赖关系分析
    private static MutableSet<? extends State> findSpecStateInternal(Object spec, int maxLevel, int currentLevel) {
        if (currentLevel >= maxLevel) return Sets.mutable.empty();
        var fields = RStream.of(spec.getClass())
                .withSuper()
                .fields()
                .jstream()
                .collect(Collectors.toSet());
        MutableSet<State> states = Sets.mutable.empty();
        for (var field : fields) {
            if (field.type().isAssignableFrom(State.class)) {
                states.add(field.get(spec));
            } else {
                var fieldValue = field.get(spec);
                boolean invalidType = fieldValue == null ||
                        fieldValue.getClass().isPrimitive() ||
                        fieldValue.getClass().isArray() ||
                        fieldValue.getClass().isEnum() ||
                        fieldValue.getClass() == Object.class;
                if (fieldValue != null && !invalidType) {
                    var nestedStates = findSpecStateInternal(fieldValue, maxLevel, currentLevel + 1);
                    states.addAll(nestedStates);
                }
            }
        }
        return states;
    }
}

