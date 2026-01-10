package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the React-like Hooks system.
 */
@DisplayName("Hooks System")
class HooksTest {

    private HookContext context;

    @BeforeEach
    void setUp() {
        context = new HookContext();
    }

    @AfterEach
    void tearDown() {
        // Ensure we're not in hook scope
        if (Hooks.isInHookScope()) {
            HookContext.exit();
        }
        context.dispose();
    }

    // ==================== useState Tests ====================

    @Nested
    @DisplayName("useState")
    class UseStateTests {

        @Test
        @DisplayName("should create new signal on first render")
        void createSignalOnFirstRender() {
            HookContext.enter(context);
            try {
                Signal<Integer> signal = Hooks.useState(42);
                assertNotNull(signal);
                assertEquals(42, signal.get());
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should return same signal on subsequent renders")
        void returnSameSignalOnSubsequentRenders() {
            Signal<Integer> first;
            Signal<Integer> second;

            // First render
            HookContext.enter(context);
            try {
                first = Hooks.useState(42);
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                second = Hooks.useState(100); // Different initial value is ignored
            } finally {
                HookContext.exit();
            }

            assertSame(first, second, "Should return the same Signal instance");
            assertEquals(42, second.get(), "Initial value should be from first render");
        }

        @Test
        @DisplayName("should persist state changes across renders")
        void persistStateChanges() {
            // First render
            HookContext.enter(context);
            try {
                Signal<Integer> count = Hooks.useState(0);
                count.set(5);
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                Signal<Integer> count = Hooks.useState(0);
                assertEquals(5, count.get(), "State change should persist");
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should support lazy initialization")
        void lazyInitialization() {
            AtomicInteger callCount = new AtomicInteger(0);

            // First render
            HookContext.enter(context);
            try {
                Signal<Integer> signal = Hooks.useState(() -> {
                    callCount.incrementAndGet();
                    return 42;
                });
                assertEquals(42, signal.get());
            } finally {
                HookContext.exit();
            }

            assertEquals(1, callCount.get());

            // Second render - supplier should NOT be called
            HookContext.enter(context);
            try {
                Signal<Integer> signal = Hooks.useState(() -> {
                    callCount.incrementAndGet();
                    return 100;
                });
                assertEquals(42, signal.get());
            } finally {
                HookContext.exit();
            }

            assertEquals(1, callCount.get(), "Supplier should only be called once");
        }

        @Test
        @DisplayName("should throw when called outside hook scope")
        void throwOutsideHookScope() {
            assertThrows(IllegalStateException.class, () -> Hooks.useState(42));
        }
    }

    // ==================== useMemo Tests ====================

    @Nested
    @DisplayName("useMemo")
    class UseMemoTests {

        @Test
        @DisplayName("should compute value on first render")
        void computeOnFirstRender() {
            HookContext.enter(context);
            try {
                int result = Hooks.useMemo(() -> 2 + 2);
                assertEquals(4, result);
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should reuse cached value when dependencies unchanged")
        void reuseCachedValue() {
            AtomicInteger computeCount = new AtomicInteger(0);
            Object dep = "constant";

            // First render
            HookContext.enter(context);
            try {
                Hooks.useMemo(() -> {
                    computeCount.incrementAndGet();
                    return "computed";
                }, dep);
            } finally {
                HookContext.exit();
            }

            assertEquals(1, computeCount.get());

            // Second render with same dependency
            HookContext.enter(context);
            try {
                Hooks.useMemo(() -> {
                    computeCount.incrementAndGet();
                    return "computed";
                }, dep);
            } finally {
                HookContext.exit();
            }

            assertEquals(1, computeCount.get(), "Should not recompute when deps unchanged");
        }

        @Test
        @DisplayName("should recompute when dependencies change")
        void recomputeOnDepChange() {
            AtomicInteger computeCount = new AtomicInteger(0);

            // First render
            HookContext.enter(context);
            try {
                Hooks.useMemo(() -> {
                    computeCount.incrementAndGet();
                    return "computed";
                }, "dep1");
            } finally {
                HookContext.exit();
            }

            assertEquals(1, computeCount.get());

            // Second render with changed dependency
            HookContext.enter(context);
            try {
                Hooks.useMemo(() -> {
                    computeCount.incrementAndGet();
                    return "computed";
                }, "dep2");
            } finally {
                HookContext.exit();
            }

            assertEquals(2, computeCount.get(), "Should recompute when deps change");
        }

        @Test
        @DisplayName("should work without dependencies")
        void noDependencies() {
            AtomicInteger computeCount = new AtomicInteger(0);

            // First render
            HookContext.enter(context);
            try {
                Hooks.useMemo(() -> {
                    computeCount.incrementAndGet();
                    return "computed";
                });
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                Hooks.useMemo(() -> {
                    computeCount.incrementAndGet();
                    return "computed";
                });
            } finally {
                HookContext.exit();
            }

            assertEquals(1, computeCount.get(), "Should cache value with no deps");
        }
    }

    // ==================== useCallback Tests ====================

    @Nested
    @DisplayName("useCallback")
    class UseCallbackTests {

        @Test
        @DisplayName("should return same callback when dependencies unchanged")
        void samCallbackWhenDepsUnchanged() {
            Runnable first;
            Runnable second;
            Object dep = "constant";

            // First render
            HookContext.enter(context);
            try {
                first = Hooks.useCallback((Runnable) () -> {}, dep);
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                second = Hooks.useCallback((Runnable) () -> {}, dep);
            } finally {
                HookContext.exit();
            }

            assertSame(first, second, "Should return same callback");
        }

        @Test
        @DisplayName("should return new callback when dependencies change")
        void newCallbackWhenDepsChange() {
            Runnable first;
            Runnable second;

            // First render
            HookContext.enter(context);
            try {
                first = Hooks.useCallback((Runnable) () -> {}, "dep1");
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                second = Hooks.useCallback((Runnable) () -> {}, "dep2");
            } finally {
                HookContext.exit();
            }

            assertNotSame(first, second, "Should return new callback when deps change");
        }
    }

    // ==================== useEffect Tests ====================

    @Nested
    @DisplayName("useEffect")
    class UseEffectTests {

        @Test
        @DisplayName("should run effect after build")
        void runEffectAfterBuild() {
            List<String> events = new ArrayList<>();

            HookContext.enter(context);
            try {
                events.add("before");
                Hooks.useEffectSimple(() -> events.add("effect"), "dep");
                events.add("after");
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("before", "after", "effect"), events);
        }

        @Test
        @DisplayName("should run cleanup on dependency change")
        void runCleanupOnDepChange() {
            List<String> events = new ArrayList<>();

            // First render
            HookContext.enter(context);
            try {
                Hooks.useEffect(() -> {
                    events.add("effect1");
                    return () -> events.add("cleanup1");
                }, "dep1");
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("effect1"), events);

            // Second render with changed dependency
            HookContext.enter(context);
            try {
                Hooks.useEffect(() -> {
                    events.add("effect2");
                    return () -> events.add("cleanup2");
                }, "dep2");
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("effect1", "cleanup1", "effect2"), events);
        }

        @Test
        @DisplayName("should not run effect when dependencies unchanged")
        void skipEffectWhenDepsUnchanged() {
            List<String> events = new ArrayList<>();
            Object dep = "constant";

            // First render
            HookContext.enter(context);
            try {
                Hooks.useEffectSimple(() -> events.add("effect"), dep);
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("effect"), events);

            // Second render with same dependency
            HookContext.enter(context);
            try {
                Hooks.useEffectSimple(() -> events.add("effect"), dep);
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("effect"), events, "Effect should not re-run");
        }

        @Test
        @DisplayName("useMount should only run on first render")
        void useMountOnlyOnce() {
            List<String> events = new ArrayList<>();

            // First render
            HookContext.enter(context);
            try {
                Hooks.useMount(() -> {
                    events.add("mount");
                    return () -> events.add("unmount");
                });
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("mount"), events);

            // Second render
            HookContext.enter(context);
            try {
                Hooks.useMount(() -> {
                    events.add("mount");
                    return () -> events.add("unmount");
                });
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("mount"), events, "Mount effect should only run once");
        }

        @Test
        @DisplayName("useRender should run on every render")
        void useRenderEveryTime() {
            List<String> events = new ArrayList<>();

            // First render
            HookContext.enter(context);
            try {
                Hooks.useRender(() -> {
                    events.add("render");
                    return null;
                });
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("render"), events);

            // Second render
            HookContext.enter(context);
            try {
                Hooks.useRender(() -> {
                    events.add("render");
                    return null;
                });
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("render", "render"), events);
        }

        @Test
        @DisplayName("should run all cleanup on dispose")
        void runCleanupOnDispose() {
            List<String> events = new ArrayList<>();

            HookContext.enter(context);
            try {
                Hooks.useEffect(() -> {
                    events.add("effect1");
                    return () -> events.add("cleanup1");
                }, "dep1");
                Hooks.useEffect(() -> {
                    events.add("effect2");
                    return () -> events.add("cleanup2");
                }, "dep2");
            } finally {
                HookContext.exit();
            }

            assertEquals(List.of("effect1", "effect2"), events);

            // Dispose (simulate unmount)
            context.dispose();

            assertEquals(List.of("effect1", "effect2", "cleanup1", "cleanup2"), events);
        }
    }

    // ==================== useRef Tests ====================

    @Nested
    @DisplayName("useRef")
    class UseRefTests {

        @Test
        @DisplayName("should create ref with initial value")
        void createWithInitialValue() {
            HookContext.enter(context);
            try {
                HookContext.Ref<Integer> ref = Hooks.useRef(42);
                assertEquals(42, ref.get());
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should return same ref on subsequent renders")
        void sameRefOnSubsequentRenders() {
            HookContext.Ref<Integer> first;
            HookContext.Ref<Integer> second;

            // First render
            HookContext.enter(context);
            try {
                first = Hooks.useRef(42);
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                second = Hooks.useRef(100); // Different initial value ignored
            } finally {
                HookContext.exit();
            }

            assertSame(first, second, "Should return same Ref instance");
        }

        @Test
        @DisplayName("should persist value changes across renders")
        void persistValueChanges() {
            // First render
            HookContext.enter(context);
            try {
                HookContext.Ref<Integer> ref = Hooks.useRef(0);
                ref.set(42);
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                HookContext.Ref<Integer> ref = Hooks.useRef(0);
                assertEquals(42, ref.get());
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("useRef() should create ref with null")
        void useRefNoArg() {
            HookContext.enter(context);
            try {
                HookContext.Ref<String> ref = Hooks.useRef();
                assertNull(ref.get());
            } finally {
                HookContext.exit();
            }
        }
    }

    // ==================== useReducer Tests ====================

    @Nested
    @DisplayName("useReducer")
    class UseReducerTests {

        record State(int count, String message) {}
        enum Action { INCREMENT, DECREMENT, RESET }

        @Test
        @DisplayName("should create reducer with initial state")
        void createWithInitialState() {
            HookContext.enter(context);
            try {
                var result = Hooks.useReducer(
                    args -> args.state(),
                    new State(0, "Initial")
                );
                assertEquals(0, result.state().get().count());
                assertEquals("Initial", result.state().get().message());
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should dispatch actions to update state")
        void dispatchActions() {
            HookContext.enter(context);
            try {
                var result = Hooks.<State, Action>useReducer(args -> {
                    State state = args.state();
                    return switch (args.action()) {
                        case INCREMENT -> new State(state.count() + 1, "Incremented");
                        case DECREMENT -> new State(state.count() - 1, "Decremented");
                        case RESET -> new State(0, "Reset");
                    };
                }, new State(0, "Initial"));

                result.dispatch().accept(Action.INCREMENT);
                assertEquals(1, result.state().get().count());
                assertEquals("Incremented", result.state().get().message());

                result.dispatch().accept(Action.INCREMENT);
                assertEquals(2, result.state().get().count());

                result.dispatch().accept(Action.RESET);
                assertEquals(0, result.state().get().count());
                assertEquals("Reset", result.state().get().message());
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should return same state signal across renders")
        void sameStateAcrossRenders() {
            HookContext.ReducerResult<State, Action> first;
            HookContext.ReducerResult<State, Action> second;

            // First render
            HookContext.enter(context);
            try {
                first = Hooks.useReducer(
                    args -> args.state(),
                    new State(0, "Initial")
                );
                first.dispatch().accept(Action.INCREMENT);
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                second = Hooks.useReducer(
                    args -> args.state(),
                    new State(99, "Different") // Ignored
                );
            } finally {
                HookContext.exit();
            }

            assertSame(first.state(), second.state(), "Should return same Signal");
        }
    }

    // ==================== Hook Ordering Tests ====================

    @Nested
    @DisplayName("Hook Ordering")
    class HookOrderingTests {

        @Test
        @DisplayName("should enforce consistent hook order")
        void enforceConsistentOrder() {
            // First render: useState then useMemo
            HookContext ctx1 = new HookContext();
            HookContext.enter(ctx1);
            try {
                Hooks.useState(1);
                Hooks.useMemo(() -> "memo");
            } finally {
                HookContext.exit();
            }

            // Second render: try useMemo then useState (wrong order)
            // This should throw because the first hook was useState but we're calling useMemo
            HookContext.enter(ctx1);
            IllegalStateException ex = null;
            try {
                Hooks.useMemo(() -> "memo"); // First hook was useState, not useMemo
            } catch (IllegalStateException e) {
                ex = e;
            }
            // Force clear hook scope to clean up test state  
            // Enter a new context then exit to clear thread local
            HookContext cleanupCtx = new HookContext();
            HookContext.enter(cleanupCtx);
            HookContext.exit();
            
            assertNotNull(ex, "Should have thrown IllegalStateException for wrong hook type");
            assertTrue(ex.getMessage().contains("Hook type mismatch") || ex.getMessage().contains("Expected"), 
                "Exception message should mention hook type mismatch");
        }

        @Test
        @DisplayName("should detect missing hooks")
        void detectMissingHooks() {
            // First render: two hooks
            HookContext ctx = new HookContext();
            HookContext.enter(ctx);
            try {
                Hooks.useState(1);
                Hooks.useState(2);
            } finally {
                HookContext.exit();
            }

            // Second render: only one hook (simulates conditional hook)
            // Should throw on exit because hook count doesn't match
            HookContext.enter(ctx);
            IllegalStateException ex = null;
            try {
                Hooks.useState(1);
                // Missing the second useState - call exit to trigger validation
                HookContext.exit();
            } catch (IllegalStateException e) {
                ex = e;
                // Enter a new context then exit to clear thread local
                HookContext cleanupCtx = new HookContext();
                HookContext.enter(cleanupCtx);
                HookContext.exit();
            }
            assertNotNull(ex, "Should have thrown IllegalStateException for missing hooks");
            assertTrue(ex.getMessage().contains("Hook count changed"), 
                "Exception message should mention hook count changed");
        }
    }

    // ==================== isInHookScope Tests ====================

    @Nested
    @DisplayName("isInHookScope")
    class IsInHookScopeTests {

        @Test
        @DisplayName("should return false when not in scope")
        void falseWhenNotInScope() {
            assertFalse(Hooks.isInHookScope());
        }

        @Test
        @DisplayName("should return true when in scope")
        void trueWhenInScope() {
            HookContext.enter(context);
            try {
                assertTrue(Hooks.isInHookScope());
            } finally {
                HookContext.exit();
            }
        }
    }

    // ==================== useComputed Tests ====================

    @Nested
    @DisplayName("useComputed")
    class UseComputedTests {

        @Test
        @DisplayName("should create computed that tracks dependencies")
        void createComputed() {
            HookContext.enter(context);
            try {
                Signal<Integer> signal = Hooks.useState(5);
                Computed<String> computed = Hooks.useComputed(
                    () -> "Value: " + signal.get()
                );
                assertEquals("Value: 5", computed.get());

                signal.set(10);
                assertEquals("Value: 10", computed.get());
            } finally {
                HookContext.exit();
            }
        }

        @Test
        @DisplayName("should return same computed across renders")
        void sameComputedAcrossRenders() {
            Computed<String> first;
            Computed<String> second;

            // First render
            HookContext.enter(context);
            try {
                Hooks.useState(5); // Need to maintain hook order
                first = Hooks.useComputed(() -> "computed");
            } finally {
                HookContext.exit();
            }

            // Second render
            HookContext.enter(context);
            try {
                Hooks.useState(5);
                second = Hooks.useComputed(() -> "computed");
            } finally {
                HookContext.exit();
            }

            assertSame(first, second, "Should return same Computed instance");
        }
    }
}
