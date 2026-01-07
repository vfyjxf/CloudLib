package dev.vfyjxf.cloudlib.api.ui.element;

import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import dev.vfyjxf.cloudlib.api.ui.reactive.ReactiveState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Element for Dynamic nodes (reactive content).
 * <p>
 * Dynamic elements automatically track their dependencies and
 * rebuild when those dependencies change.
 */
public class DynamicElement extends Element {
    
    /** Child element */
    private @Nullable Element child;
    
    /** Dependencies tracked during last build */
    private Set<ReactiveState<?>> dependencies = new HashSet<>();
    
    /** Subscriptions to dependencies */
    private final java.util.List<ReactiveState.Subscription> subscriptions = new java.util.ArrayList<>();
    
    public DynamicElement(RenderNode.Dynamic dynamic) {
        super(dynamic);
    }
    
    @Override
    protected void rebuild() {
        if (!(widget instanceof RenderNode.Dynamic dynamic)) {
            return;
        }
        
        // Unsubscribe from old dependencies
        for (ReactiveState.Subscription sub : subscriptions) {
            sub.unsubscribe();
        }
        subscriptions.clear();
        
        // Track new dependencies
        Set<ReactiveState<?>> newDependencies = new HashSet<>();
        RenderNode childWidget;
        
        try (var tracker = new DependencyCollector(newDependencies)) {
            childWidget = dynamic.supplier().get();
        }
        
        // Subscribe to new dependencies
        for (ReactiveState<?> dep : newDependencies) {
            ReactiveState.Subscription sub = dep.subscribe(v -> markNeedsBuild());
            subscriptions.add(sub);
        }
        dependencies = newDependencies;
        
        // Update child
        child = updateChild(child, childWidget);
        
        // Update children list
        children.clear();
        if (child != null) {
            children.add(child);
        }
    }
    
    @Override
    protected void onUnmount() {
        // Unsubscribe from all dependencies
        for (ReactiveState.Subscription sub : subscriptions) {
            sub.unsubscribe();
        }
        subscriptions.clear();
        dependencies.clear();
        
        if (child != null) {
            child.unmount();
            child = null;
        }
        
        super.onUnmount();
    }
    
    public Set<ReactiveState<?>> getDependencies() {
        return Set.copyOf(dependencies);
    }
    
    /**
     * Internal dependency collector.
     */
    private static class DependencyCollector implements AutoCloseable {
        private static final ThreadLocal<DependencyCollector> CURRENT = new ThreadLocal<>();
        
        private final Set<ReactiveState<?>> dependencies;
        private final DependencyCollector previous;
        
        DependencyCollector(Set<ReactiveState<?>> dependencies) {
            this.dependencies = dependencies;
            this.previous = CURRENT.get();
            CURRENT.set(this);
        }
        
        static void capture(ReactiveState<?> state) {
            DependencyCollector current = CURRENT.get();
            if (current != null) {
                current.dependencies.add(state);
            }
        }
        
        @Override
        public void close() {
            CURRENT.set(previous);
        }
    }
}
