package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Element for Component nodes.
 * <p>
 * ComponentElement is the stateful element that manages:
 * <ul>
 *   <li>Component state (signals, computed, effects)</li>
 *   <li>Dependency tracking for fine-grained reactivity</li>
 *   <li>Lifecycle callbacks (mount/unmount)</li>
 * </ul>
 * <p>
 * <b>Fine-grained recomposition:</b> When a Signal changes, only ComponentElements
 * that actually read that Signal during their last build will be marked dirty and
 * rebuilt. Other elements in the tree remain untouched.
 */
public class ComponentElement extends Element {
    
    /** The component being rendered */
    private final Component component;
    
    /** Single child element */
    private @Nullable Element child;
    
    // ===== State Storage (slot-based, React Hooks style) =====
    
    private final List<Signal<?>> signals = new ArrayList<>();
    private final List<Computed<?>> computeds = new ArrayList<>();
    private final List<MemoSlot<?>> memos = new ArrayList<>();
    private final List<EffectSlot> effects = new ArrayList<>();
    private final Map<String, RefImpl<?>> refs = new HashMap<>();
    
    private int signalIndex = 0;
    private int computedIndex = 0;
    private int memoIndex = 0;
    private int effectIndex = 0;
    
    // ===== Dependency Tracking =====
    
    private Set<ReactiveState<?>> trackedDependencies = new HashSet<>();
    private final List<ReactiveState.Subscription> trackingSubscriptions = new ArrayList<>();
    
    // ===== Lifecycle =====
    
    private final List<Runnable> mountCallbacks = new ArrayList<>();
    private final List<Runnable> unmountCallbacks = new ArrayList<>();
    private boolean mounted = false;
    
    public ComponentElement(RenderNode.ComponentRef ref) {
        super(ref);
        this.component = ref.component();
    }
    
    @Override
    protected void onMount() {
        super.onMount();
        if (!mounted) {
            mounted = true;
            for (Runnable callback : mountCallbacks) {
                callback.run();
            }
        }
    }
    
    @Override
    protected void onUnmount() {
        // Clear tracking subscriptions
        clearTrackingSubscriptions();
        
        // Run unmount callbacks
        if (mounted) {
            mounted = false;
            for (Runnable callback : unmountCallbacks) {
                callback.run();
            }
        }
        
        // Cleanup effects
        for (EffectSlot effect : effects) {
            if (effect.cleanup != null) {
                effect.cleanup.run();
            }
        }
        
        // Clear state
        signals.clear();
        computeds.clear();
        memos.clear();
        effects.clear();
        refs.clear();
        mountCallbacks.clear();
        unmountCallbacks.clear();
        trackedDependencies.clear();
        
        // Unmount child
        if (child != null) {
            child.unmount();
            child = null;
        }
        
        super.onUnmount();
    }
    
    @Override
    protected void rebuild() {
        // Reset slot indices for rebuild
        signalIndex = 0;
        computedIndex = 0;
        memoIndex = 0;
        effectIndex = 0;
        
        // Clear old tracking
        clearTrackingSubscriptions();
        trackedDependencies.clear();
        
        // Build with dependency tracking
        RenderNode newChild;
        try (var scope = Tracker.start(this::trackDependency)) {
            // Build the component
            ComponentContext ctx = component.isStateful() ? new StatefulContext() : new StatelessContext();
            newChild = component.render(ctx);
        }
        
        // Setup subscriptions for dependencies
        for (ReactiveState<?> dep : trackedDependencies) {
            var sub = dep.subscribe(value -> markNeedsBuild());
            trackingSubscriptions.add(sub);
        }
        
        // Run effects
        runEffects();
        
        // Update child element
        child = updateChild(child, newChild);
        
        // Update children list for tree traversal
        children.clear();
        if (child != null) {
            children.add(child);
        }
    }
    
    private void clearTrackingSubscriptions() {
        for (var sub : trackingSubscriptions) {
            sub.unsubscribe();
        }
        trackingSubscriptions.clear();
    }
    
    private void trackDependency(ReactiveState<?> state) {
        trackedDependencies.add(state);
    }
    
    private void runEffects() {
        for (EffectSlot effect : effects) {
            if (effect.shouldRun) {
                // Cleanup previous
                if (effect.cleanup != null) {
                    effect.cleanup.run();
                }
                // Run effect and capture cleanup
                effect.cleanup = effect.effect.get();
                effect.shouldRun = false;
                effect.previousDependencies = effect.currentDependencies;
            }
        }
    }
    
    @Override
    protected void onUpdate(RenderNode oldWidget, RenderNode newWidget) {
        super.onUpdate(oldWidget, newWidget);
        markNeedsBuild();
    }
    
    /**
     * Checks if this element should rebuild.
     */
    public boolean shouldRebuild() {
        return dirty;
    }
    
    public Component getComponent() {
        return component;
    }
    
    /**
     * Gets tracked dependencies from last build.
     */
    public Set<ReactiveState<?>> getTrackedDependencies() {
        return Set.copyOf(trackedDependencies);
    }
    
    /**
     * Gets total signal count.
     */
    public int getSignalCount() {
        return signals.size();
    }
    
    // ===== Internal Context Classes =====
    
    /**
     * Stateless context - throws on state operations.
     */
    private class StatelessContext implements ComponentContext {
        @Override
        public <T> Signal<T> signal(T initialValue) {
            throw new UnsupportedOperationException(
                "Cannot create signal in stateless component. Use Component.stateful() instead.");
        }
        
        @Override
        public <T> Computed<T> computed(Supplier<T> computation) {
            return Computed.of(computation);
        }
        
        @Override
        public <T> T memo(Supplier<T> factory, Object... dependencies) {
            return factory.get();
        }
        
        @Override
        public void effect(Runnable effect) { }
        
        @Override
        public void effect(Supplier<Runnable> effect) { }
        
        @Override
        public void effect(Supplier<Runnable> effect, Object... dependencies) { }
        
        @Override
        public void watch(ReactiveState<?> state, Runnable callback) { }
        
        @Override
        public <T> void watch(ReactiveState<T> state, Consumer<T> callback) { }
        
        @Override
        public void onMount(Runnable callback) { }
        
        @Override
        public void onUnmount(Runnable callback) { }
        
        @Override
        public void onTick(Runnable callback) { }
        
        @Override
        public <T> Ref<T> ref(String name) {
            return refInternal(name);
        }
        
        @Override
        public <T> T provide(Providers.Key<T> key) {
            return Providers.get(key);
        }
        
        @Override
        public void invalidate() {
            ComponentElement.this.markNeedsBuild();
        }
    }
    
    /**
     * Stateful context - full state management.
     */
    private class StatefulContext implements ComponentContext {
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Signal<T> signal(T initialValue) {
            Signal<T> signal;
            if (signalIndex < signals.size()) {
                signal = (Signal<T>) signals.get(signalIndex);
            } else {
                signal = Signal.of(initialValue);
                signals.add(signal);
                // Auto-subscribe for rebuild
                signal.subscribe(value -> markNeedsBuild());
            }
            signalIndex++;
            return signal;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Computed<T> computed(Supplier<T> computation) {
            Computed<T> computed;
            if (computedIndex < computeds.size()) {
                computed = (Computed<T>) computeds.get(computedIndex);
            } else {
                computed = Computed.of(computation);
                computeds.add(computed);
            }
            computedIndex++;
            return computed;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T memo(Supplier<T> factory, Object... dependencies) {
            MemoSlot<T> slot;
            if (memoIndex < memos.size()) {
                slot = (MemoSlot<T>) memos.get(memoIndex);
                if (!Arrays.equals(slot.dependencies, dependencies)) {
                    slot.value = factory.get();
                    slot.dependencies = dependencies.clone();
                }
            } else {
                slot = new MemoSlot<>(factory.get(), dependencies.clone());
                memos.add(slot);
            }
            memoIndex++;
            return slot.value;
        }
        
        @Override
        public void effect(Runnable effect) {
            effect(() -> { effect.run(); return null; });
        }
        
        @Override
        public void effect(Supplier<Runnable> effect) {
            effect(effect, new Object[0]);
        }
        
        @Override
        public void effect(Supplier<Runnable> effect, Object... dependencies) {
            EffectSlot slot;
            if (effectIndex < effects.size()) {
                slot = effects.get(effectIndex);
                slot.effect = effect;
                slot.currentDependencies = dependencies.clone();
                // Check if dependencies changed
                slot.shouldRun = !Arrays.equals(slot.previousDependencies, dependencies);
            } else {
                slot = new EffectSlot(effect, dependencies.clone());
                slot.shouldRun = true;
                effects.add(slot);
            }
            effectIndex++;
        }
        
        @Override
        public void watch(ReactiveState<?> state, Runnable callback) {
            effect(() -> {
                var sub = state.subscribe(v -> callback.run());
                return sub::unsubscribe;
            });
        }
        
        @Override
        public <T> void watch(ReactiveState<T> state, Consumer<T> callback) {
            effect(() -> {
                var sub = state.subscribe(callback);
                return sub::unsubscribe;
            });
        }
        
        @Override
        public void onMount(Runnable callback) {
            if (!mounted) {
                mountCallbacks.add(callback);
            } else {
                callback.run();
            }
        }
        
        @Override
        public void onUnmount(Runnable callback) {
            unmountCallbacks.add(callback);
        }
        
        @Override
        public void onTick(Runnable callback) {
            // ElementTree doesn't directly support tick events
            // Tick handling is done via widget-level events
        }
        
        @Override
        public <T> Ref<T> ref(String name) {
            return refInternal(name);
        }
        
        @Override
        public <T> T provide(Providers.Key<T> key) {
            return Providers.get(key);
        }
        
        @Override
        public void invalidate() {
            ComponentElement.this.markNeedsBuild();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> ComponentContext.Ref<T> refInternal(String name) {
        return (ComponentContext.Ref<T>) refs.computeIfAbsent(name, k -> new RefImpl<>());
    }
    
    // ===== Internal Data Classes =====
    
    private static class MemoSlot<T> {
        T value;
        Object[] dependencies;
        
        MemoSlot(T value, Object[] dependencies) {
            this.value = value;
            this.dependencies = dependencies;
        }
    }
    
    private static class EffectSlot {
        Supplier<Runnable> effect;
        @Nullable Runnable cleanup;
        Object[] currentDependencies;
        Object[] previousDependencies;
        boolean shouldRun;
        
        EffectSlot(Supplier<Runnable> effect, Object[] dependencies) {
            this.effect = effect;
            this.currentDependencies = dependencies;
            this.previousDependencies = null;
        }
    }
    
    private static class RefImpl<T> implements ComponentContext.Ref<T> {
        private T value;
        
        @Override
        public T get() {
            return value;
        }
        
        @Override
        public void set(T value) {
            this.value = value;
        }
    }
}
