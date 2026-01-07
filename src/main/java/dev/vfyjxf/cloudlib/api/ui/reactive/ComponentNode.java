package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runtime node that manages a component's lifecycle, state, and reconciliation.
 * <p>
 * ComponentNode is the runtime counterpart to {@link Component}. While Component
 * describes what should be rendered, ComponentNode manages the actual execution:
 * <ul>
 *   <li>Lifecycle (mount, update, unmount)</li>
 *   <li>State management via {@link ComponentContext}</li>
 *   <li>Effect scheduling and cleanup</li>
 *   <li>Child reconciliation</li>
 * </ul>
 * <p>
 * This is analogous to React Fiber or Flutter Element, but simplified for our use case.
 */
public final class ComponentNode {

    private final Component component;
    private final @Nullable Object key;
    private final @Nullable ComponentNode parent;
    private final List<ComponentNode> children = new ArrayList<>();
    private final Context context;

    private State state = State.CREATED;
    private @Nullable RenderNode cachedRender;
    private boolean needsRebuild = true;

    // State management
    private final List<Signal<?>> signals = new ArrayList<>();
    private final List<Computed<?>> computeds = new ArrayList<>();
    private final List<EffectEntry> effects = new ArrayList<>();
    private final List<Runnable> mountCallbacks = new ArrayList<>();
    private final List<Runnable> unmountCallbacks = new ArrayList<>();
    private final Map<String, Object> refs = new HashMap<>();

    private int stateIndex = 0;
    private int computedIndex = 0;
    private int effectIndex = 0;
    private int memoIndex = 0;

    /**
     * Component lifecycle states.
     */
    public enum State {
        CREATED,
        MOUNTING,
        MOUNTED,
        UPDATING,
        UNMOUNTING,
        UNMOUNTED
    }

    /**
     * Creates a new component node.
     *
     * @param component the component
     * @param key       optional key for reconciliation
     * @param parent    parent node, null for root
     */
    public ComponentNode(Component component, @Nullable Object key, @Nullable ComponentNode parent) {
        this.component = component;
        this.key = key;
        this.parent = parent;
        this.context = new Context(this);
    }

    /**
     * Gets the component.
     *
     * @return the component
     */
    public Component component() {
        return component;
    }

    /**
     * Gets the key.
     *
     * @return the key or null
     */
    public @Nullable Object key() {
        return key;
    }

    /**
     * Gets the lifecycle state.
     *
     * @return current state
     */
    public State state() {
        return state;
    }

    /**
     * Gets the parent node.
     *
     * @return parent or null if root
     */
    public @Nullable ComponentNode parent() {
        return parent;
    }

    /**
     * Gets the children.
     *
     * @return unmodifiable list of children
     */
    public List<ComponentNode> children() {
        return List.copyOf(children);
    }

    /**
     * Mounts this node, invoking the component's render for the first time.
     *
     * @return the rendered output
     */
    public RenderNode mount() {
        if (state != State.CREATED) {
            throw new IllegalStateException("Node already mounted or unmounted");
        }

        state = State.MOUNTING;
        resetHookIndices();

        // Render with context
        cachedRender = renderWithContext();

        // Process children (find ComponentRef nodes and mount them)
        mountChildren(cachedRender);

        // Invoke mount callbacks
        for (Runnable callback : mountCallbacks) {
            callback.run();
        }

        // Run effects
        for (EffectEntry effect : effects) {
            effect.run();
        }

        state = State.MOUNTED;
        needsRebuild = false;

        return cachedRender;
    }

    /**
     * Rebuilds this node if needed.
     *
     * @return the rendered output
     */
    public RenderNode rebuild() {
        if (state != State.MOUNTED) {
            throw new IllegalStateException("Can only rebuild mounted nodes");
        }

        if (!needsRebuild) {
            return cachedRender;
        }

        state = State.UPDATING;
        resetHookIndices();

        // Re-render
        RenderNode newRender = renderWithContext();

        // Reconcile children
        reconcileChildren(cachedRender, newRender);

        cachedRender = newRender;

        // Re-run effects that have changed dependencies
        for (EffectEntry effect : effects) {
            if (effect.shouldRerun()) {
                effect.cleanup();
                effect.run();
            }
        }

        state = State.MOUNTED;
        needsRebuild = false;

        return cachedRender;
    }

    /**
     * Unmounts this node and all children.
     */
    public void unmount() {
        if (state == State.UNMOUNTED || state == State.CREATED) {
            return;
        }

        state = State.UNMOUNTING;

        // Unmount children first
        for (ComponentNode child : children) {
            child.unmount();
        }
        children.clear();

        // Cleanup effects
        for (EffectEntry effect : effects) {
            effect.cleanup();
        }

        // Invoke unmount callbacks
        for (Runnable callback : unmountCallbacks) {
            callback.run();
        }

        // Clear state
        signals.clear();
        computeds.clear();
        effects.clear();
        mountCallbacks.clear();
        unmountCallbacks.clear();
        refs.clear();
        cachedRender = null;

        state = State.UNMOUNTED;
    }

    /**
     * Marks this node as needing rebuild.
     */
    public void markNeedsRebuild() {
        if (state == State.MOUNTED && !needsRebuild) {
            needsRebuild = true;
            // Rebuild will be triggered by the reactive system
        }
    }

    // ===== Context Implementation =====

    /**
     * The context implementation passed to components.
     */
    private final class Context implements ComponentContext {
        private final ComponentNode node;

        Context(ComponentNode node) {
            this.node = node;
        }

        @Override
        public <T> Signal<T> signal(T initialValue) {
            if (node.state == State.MOUNTING && stateIndex >= signals.size()) {
                Signal<T> signal = Signal.of(initialValue);
                signal.subscribe(ignored -> node.markNeedsRebuild());
                signals.add(signal);
                stateIndex++;
                return signal;
            }
            @SuppressWarnings("unchecked")
            Signal<T> existing = (Signal<T>) signals.get(stateIndex++);
            return existing;
        }

        @Override
        public <T> Computed<T> computed(Supplier<T> computation) {
            if (node.state == State.MOUNTING && computedIndex >= computeds.size()) {
                Computed<T> computed = Computed.of(computation);
                computed.subscribe(ignored -> node.markNeedsRebuild());
                computeds.add(computed);
                computedIndex++;
                return computed;
            }
            @SuppressWarnings("unchecked")
            Computed<T> existing = (Computed<T>) computeds.get(computedIndex++);
            return existing;
        }

        @Override
        public <T> T memo(Supplier<T> factory, Object... dependencies) {
            // Simple memo implementation using computed
            if (node.state == State.MOUNTING && memoIndex >= computeds.size()) {
                // Create a computed that wraps the factory
                Computed<T> computed = Computed.of(factory);
                computed.subscribe(ignored -> node.markNeedsRebuild());
                computeds.add(computed);
                memoIndex++;
                return computed.get();
            }
            @SuppressWarnings("unchecked")
            Computed<T> existing = (Computed<T>) computeds.get(memoIndex++);
            return existing.get();
        }

        @Override
        public void effect(Runnable effect) {
            effect(() -> {
                effect.run();
                return null;
            });
        }

        @Override
        public void effect(Supplier<Runnable> effect) {
            if (node.state == State.MOUNTING && effectIndex >= effects.size()) {
                effects.add(new EffectEntry(effect, null));
                effectIndex++;
            } else if (effectIndex < effects.size()) {
                effectIndex++;
            }
        }

        @Override
        public void effect(Supplier<Runnable> effect, Object... dependencies) {
            if (node.state == State.MOUNTING && effectIndex >= effects.size()) {
                effects.add(new EffectEntry(effect, dependencies));
                effectIndex++;
            } else if (effectIndex < effects.size()) {
                effects.get(effectIndex).updateDependencies(dependencies);
                effectIndex++;
            }
        }

        @Override
        public void watch(ReactiveState<?> state, Runnable callback) {
            effect(() -> {
                var subscription = state.subscribe(ignored -> callback.run());
                return subscription::unsubscribe;
            });
        }

        @Override
        public <T> void watch(ReactiveState<T> state, Consumer<T> callback) {
            effect(() -> {
                var subscription = state.subscribe(callback);
                return subscription::unsubscribe;
            });
        }

        @Override
        public void onMount(Runnable callback) {
            if (node.state == State.MOUNTING) {
                mountCallbacks.add(callback);
            }
        }

        @Override
        public void onUnmount(Runnable callback) {
            if (node.state == State.MOUNTING) {
                unmountCallbacks.add(callback);
            }
        }
        
        @Override
        public void onTick(Runnable callback) {
            // ComponentNode doesn't directly support tick - it's primarily for 
            // component tree management. Tick handling is done via widget events.
            // For now, just ignore tick registrations in this context.
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Ref<T> ref(String name) {
            return (Ref<T>) refs.computeIfAbsent(name, k -> new RefImpl<>());
        }

        @Override
        public <T> T provide(Providers.Key<T> key) {
            return Providers.get(key);
        }

        @Override
        public void invalidate() {
            node.markNeedsRebuild();
        }
    }

    /**
     * Ref implementation.
     */
    private static final class RefImpl<T> implements ComponentContext.Ref<T> {
        private @Nullable T value;

        @Override
        public @Nullable T get() {
            return value;
        }

        @Override
        public void set(@Nullable T value) {
            this.value = value;
        }
    }

    /**
     * Effect entry for tracking effects and their cleanup.
     */
    private static final class EffectEntry {
        private final Supplier<Runnable> effect;
        private @Nullable Object[] dependencies;
        private @Nullable Object[] previousDeps;
        private @Nullable Runnable cleanup;

        EffectEntry(Supplier<Runnable> effect, @Nullable Object[] dependencies) {
            this.effect = effect;
            this.dependencies = dependencies;
        }

        void run() {
            cleanup = effect.get();
            previousDeps = dependencies != null ? dependencies.clone() : null;
        }

        void cleanup() {
            if (cleanup != null) {
                cleanup.run();
                cleanup = null;
            }
        }

        boolean shouldRerun() {
            if (dependencies == null) {
                return true; // No deps = run every time
            }
            if (previousDeps == null || previousDeps.length != dependencies.length) {
                return true;
            }
            for (int i = 0; i < dependencies.length; i++) {
                if (!java.util.Objects.equals(previousDeps[i], dependencies[i])) {
                    return true;
                }
            }
            return false;
        }

        void updateDependencies(@Nullable Object[] newDeps) {
            this.dependencies = newDeps;
        }
    }

    // ===== Private Methods =====

    private void resetHookIndices() {
        stateIndex = 0;
        computedIndex = 0;
        effectIndex = 0;
        memoIndex = 0;
    }

    private RenderNode renderWithContext() {
        return switch (component) {
            case Component.Pure pure -> pure.render(context);
            case Component.Stateless stateless -> stateless.render(context);
            case Component.Stateful stateful -> stateful.render(context);
            case Component.Styled styled -> {
                // For styled, we need to render the inner component first
                ComponentNode innerNode = new ComponentNode(styled.inner(), null, this);
                children.add(innerNode);
                RenderNode innerRender = innerNode.mount();
                // Apply style to the rendered node
                yield applyStyleToRenderNode(innerRender, styled.style());
            }
        };
    }

    private RenderNode applyStyleToRenderNode(RenderNode node, Style style) {
        if (style == null || style == Style.NONE) return node;
        return switch (node) {
            case RenderNode.Leaf leaf -> new RenderNode.Leaf(
                leaf.type(), 
                leaf.content(), 
                leaf.style() != null ? leaf.style().with(style) : style
            );
            case RenderNode.Group group -> new RenderNode.Group(
                group.layout(), 
                group.children(), 
                group.style() != null ? group.style().with(style) : style
            );
            default -> node;
        };
    }

    private void mountChildren(RenderNode node) {
        if (node == null) return;

        switch (node) {
            case RenderNode.ComponentRef ref -> {
                ComponentNode childNode = new ComponentNode(ref.component(), ref.key(), this);
                children.add(childNode);
                childNode.mount();
            }
            case RenderNode.Group group -> {
                for (RenderNode child : group.children()) {
                    mountChildren(child);
                }
            }
            case RenderNode.Conditional cond -> {
                if (cond.condition().get()) {
                    mountChildren(cond.whenTrue());
                } else if (cond.whenFalse() != null) {
                    mountChildren(cond.whenFalse());
                }
            }
            case RenderNode.Dynamic dynamic -> mountChildren(dynamic.supplier().get());
            case RenderNode.ForEach<?> forEach -> {
                int index = 0;
                for (Object item : forEach.items().get()) {
                    @SuppressWarnings("unchecked")
                    RenderNode.ItemRenderer<Object> renderer = (RenderNode.ItemRenderer<Object>) forEach.renderer();
                    mountChildren(renderer.render(item, index++));
                }
            }
            default -> {
                // Leaf, Empty, Slot - no children to mount
            }
        }
    }

    private void reconcileChildren(RenderNode oldNode, RenderNode newNode) {
        // Simplified reconciliation - in a full implementation, this would do
        // proper diffing and keyed reconciliation
        List<ComponentNode> oldChildren = new ArrayList<>(children);
        children.clear();

        // Collect new component refs
        List<RenderNode.ComponentRef> newRefs = new ArrayList<>();
        collectComponentRefs(newNode, newRefs);

        Map<Object, ComponentNode> keyedOld = new HashMap<>();
        List<ComponentNode> unkeyedOld = new ArrayList<>();

        for (ComponentNode child : oldChildren) {
            if (child.key != null) {
                keyedOld.put(child.key, child);
            } else {
                unkeyedOld.add(child);
            }
        }

        int unkeyedIndex = 0;
        for (RenderNode.ComponentRef ref : newRefs) {
            ComponentNode match = null;

            if (ref.key() != null) {
                match = keyedOld.remove(ref.key());
            } else if (unkeyedIndex < unkeyedOld.size()) {
                ComponentNode candidate = unkeyedOld.get(unkeyedIndex++);
                if (candidate.component.getClass() == ref.component().getClass()) {
                    match = candidate;
                }
            }

            if (match != null && canReuse(match.component, ref.component())) {
                // Reuse existing node
                children.add(match);
                match.markNeedsRebuild();
            } else {
                // Unmount old and create new
                if (match != null) {
                    match.unmount();
                }
                ComponentNode newChild = new ComponentNode(ref.component(), ref.key(), this);
                children.add(newChild);
                newChild.mount();
            }
        }

        // Unmount remaining old nodes
        for (ComponentNode child : keyedOld.values()) {
            child.unmount();
        }
        for (int i = unkeyedIndex; i < unkeyedOld.size(); i++) {
            if (!children.contains(unkeyedOld.get(i))) {
                unkeyedOld.get(i).unmount();
            }
        }
    }

    private void collectComponentRefs(RenderNode node, List<RenderNode.ComponentRef> refs) {
        if (node == null) return;

        switch (node) {
            case RenderNode.ComponentRef ref -> refs.add(ref);
            case RenderNode.Group group -> {
                for (RenderNode child : group.children()) {
                    collectComponentRefs(child, refs);
                }
            }
            case RenderNode.Dynamic dynamic -> collectComponentRefs(dynamic.supplier().get(), refs);
            default -> {}
        }
    }

    private boolean canReuse(Component oldComp, Component newComp) {
        return oldComp.getClass() == newComp.getClass();
    }
}
