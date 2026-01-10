package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.state.HookContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Hooks;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for hooks in StatefulElement.
 */
@DisplayName("StatefulElement Hooks Integration")
class StatefulElementHooksTest {

    @Test
    @DisplayName("hooks should be available in StatefulBlueprint builder")
    void hooksAvailableInBuilder() {
        // Track if hooks were called
        AtomicInteger hookCallCount = new AtomicInteger(0);
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            // Should be able to call hooks inside builder
            assertTrue(Hooks.isInHookScope(), "Should be in hook scope");
            Signal<Integer> count = Hooks.useState(0);
            hookCallCount.incrementAndGet();
            return new EmptyBlueprint();
        });
        
        // Create element and mount it (mount triggers first build)
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        element.mount(null, null);
        
        assertEquals(1, hookCallCount.get(), "Hook should be called during build");
    }
    
    @Test
    @DisplayName("useState should persist across rebuilds")
    void useStatePersistsAcrossRebuilds() {
        List<Signal<Integer>> capturedSignals = new ArrayList<>();
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            Signal<Integer> count = Hooks.useState(0);
            capturedSignals.add(count);
            return new EmptyBlueprint();
        });
        
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        
        // First build (through mount)
        element.mount(null, null);
        assertEquals(1, capturedSignals.size());
        Signal<Integer> firstSignal = capturedSignals.get(0);
        assertEquals(0, firstSignal.get());
        
        // Modify state
        firstSignal.set(42);
        
        // Second build (force rebuild)
        element.performRebuild();
        assertEquals(2, capturedSignals.size());
        
        // Should be the same signal instance
        assertSame(firstSignal, capturedSignals.get(1), 
            "useState should return the same Signal instance across rebuilds");
        assertEquals(42, capturedSignals.get(1).get(), 
            "State value should persist");
    }
    
    @Test
    @DisplayName("useMemo should memoize values")
    void useMemoMemoizesValues() {
        AtomicInteger computeCount = new AtomicInteger(0);
        Signal<Integer> input = Signal.of(5);
        List<String> results = new ArrayList<>();
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            String memoized = Hooks.useMemo(() -> {
                computeCount.incrementAndGet();
                return "Result: " + input.peek();
            }, input.peek()); // Use peek to not track input changes automatically
            results.add(memoized);
            return new EmptyBlueprint();
        });
        
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        
        // First build
        element.mount(null, null);
        assertEquals(1, computeCount.get());
        assertEquals("Result: 5", results.get(0));
        
        // Second build with same input value
        element.performRebuild();
        assertEquals(1, computeCount.get(), "Should not recompute with same deps");
        
        // Change input and rebuild
        input.set(10);
        element.performRebuild();
        assertEquals(2, computeCount.get(), "Should recompute with changed deps");
        assertEquals("Result: 10", results.get(2));
    }
    
    @Test
    @DisplayName("useEffect should run after build")
    void useEffectRunsAfterBuild() {
        List<String> events = new ArrayList<>();
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            events.add("build");
            Hooks.useEffectSimple(() -> events.add("effect"), "dep");
            return new EmptyBlueprint();
        });
        
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        element.mount(null, null);
        
        // Effect should run after build
        assertEquals(List.of("build", "effect"), events);
    }
    
    @Test
    @DisplayName("useEffect cleanup should run on unmount")
    void useEffectCleanupRunsOnUnmount() {
        List<String> events = new ArrayList<>();
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            Hooks.useEffect(() -> {
                events.add("effect");
                return () -> events.add("cleanup");
            });
            return new EmptyBlueprint();
        });
        
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        element.mount(null, null);
        
        assertEquals(List.of("effect"), events);
        
        // Unmount should run cleanup
        element.unmount();
        assertEquals(List.of("effect", "cleanup"), events);
    }
    
    @Test
    @DisplayName("useRef should persist without triggering rebuild")
    void useRefPersists() {
        List<Integer> values = new ArrayList<>();
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            HookContext.Ref<Integer> ref = Hooks.useRef(0);
            values.add(ref.get());
            ref.set(ref.get() + 1);
            return new EmptyBlueprint();
        });
        
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        
        // First build
        element.mount(null, null);
        assertEquals(List.of(0), values);
        
        // Second build
        element.performRebuild();
        assertEquals(List.of(0, 1), values);
        
        // Third build
        element.performRebuild();
        assertEquals(List.of(0, 1, 2), values);
    }
    
    @Test
    @DisplayName("multiple hooks should work together")
    void multipleHooksWorkTogether() {
        List<String> events = new ArrayList<>();
        
        StatefulBlueprint blueprint = StatefulBlueprint.of(() -> {
            Signal<Integer> count = Hooks.useState(0);
            String display = Hooks.useMemo(() -> "Count: " + count.peek(), count.peek());
            HookContext.Ref<Integer> renderCount = Hooks.useRef(0);
            
            renderCount.set(renderCount.get() + 1);
            events.add("render#" + renderCount.get() + " " + display);
            
            Hooks.useEffect(() -> {
                events.add("effect#" + renderCount.get());
                return () -> events.add("cleanup#" + renderCount.get());
            }, count.peek());
            
            return new EmptyBlueprint();
        });
        
        StatefulElement<?> element = (StatefulElement<?>) blueprint.createElement();
        element.mount(null, null);
        
        assertEquals(List.of("render#1 Count: 0", "effect#1"), events);
        
        // Force rebuild
        element.performRebuild();
        
        // Same deps, so effect doesn't re-run
        assertEquals(List.of("render#1 Count: 0", "effect#1", "render#2 Count: 0"), events);
    }
    
    // Simple blueprint for testing
    private static class EmptyBlueprint implements Blueprint {
        @Override
        public UIElement<?> createElement() {
            return new EmptyElement(this);
        }

        @Override
        public boolean canUpdate(Blueprint other) {
            return other instanceof EmptyBlueprint;
        }
    }
    
    private static class EmptyElement extends AbstractElement<EmptyBlueprint> {
        public EmptyElement(EmptyBlueprint blueprint) {
            super(blueprint);
        }

        @Override
        protected void build() {}

        @Override
        protected void unmountChildren() {}

        @Override
        public void visitChildren(ElementVisitor visitor) {}
    }
}
