package dev.vfyjxf.cloudlib.api.ui.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StateSlot - React-like hooks system.
 * Tests useState, useMemo, useEffect and StateContext.
 */
class StateSlotTest {

    private StateSlot.StateContext context;

    @BeforeEach
    void setUp() {
        context = new StateSlot.StateContext();
    }

    // ==================== useState Tests ====================

    @Test
    void testUseStateInitialValue() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(0);
            assertEquals(0, counter.get());
        });
    }

    @Test
    void testUseStateWithSupplier() {
        AtomicInteger initCalls = new AtomicInteger(0);
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<String> state = StateSlot.useState(() -> {
                initCalls.incrementAndGet();
                return "initial";
            });
            assertEquals("initial", state.get());
        });
        assertEquals(1, initCalls.get());
    }

    @Test
    void testUseStateSetValue() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(0);
            counter.set(42);
            assertEquals(42, counter.get());
        });
    }

    @Test
    void testUseStateUpdateFunction() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(10);
            counter.update(v -> v + 5);
            assertEquals(15, counter.get());
        });
    }

    @Test
    void testUseStateMarksDirtyOnChange() {
        assertFalse(context.isDirty());

        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(0);
            counter.set(1);
        });

        assertTrue(context.isDirty());
    }

    @Test
    void testUseStateDoesNotMarkDirtyOnSameValue() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(42);
            // Setting same value should not mark dirty
        });

        context.clearDirty();

        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(42);
            counter.set(42); // Same value
        });

        assertFalse(context.isDirty());
    }

    @Test
    void testMultipleUseStateCallsPreserveOrder() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<String> first = StateSlot.useState("first");
            StateSlot.StateAccessor<String> second = StateSlot.useState("second");
            StateSlot.StateAccessor<String> third = StateSlot.useState("third");

            assertEquals("first", first.get());
            assertEquals("second", second.get());
            assertEquals("third", third.get());
        });
    }

    @Test
    void testUseStatePreservesAcrossRerenders() {
        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(0);
            counter.set(100);
        });

        // Second render - should preserve state
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(0);
            assertEquals(100, counter.get(), "State should be preserved across renders");
        });
    }

    @Test
    void testUseStateOutsideContextThrows() {
        assertThrows(IllegalStateException.class, () -> {
            StateSlot.useState(0);
        });
    }

    @Test
    void testUseStateWithNullValue() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<String> state = StateSlot.useState((String) null);
            assertNull(state.get());

            state.set("not null");
            assertEquals("not null", state.get());

            state.set(null);
            assertNull(state.get());
        });
    }

    @Test
    void testOnDirtyCallback() {
        List<String> callbacks = new ArrayList<>();
        context.setOnDirty(() -> callbacks.add("dirty"));

        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> counter = StateSlot.useState(0);
            counter.set(1);
        });

        assertEquals(1, callbacks.size());
        assertEquals("dirty", callbacks.get(0));
    }

    // ==================== useMemo Tests ====================

    @Test
    void testUseMemoComputesOnFirstRender() {
        AtomicInteger computeCount = new AtomicInteger(0);

        StateSlot.withContext(context, () -> {
            String result = StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            });
            assertEquals("computed", result);
        });

        assertEquals(1, computeCount.get());
    }

    @Test
    void testUseMemoReusesWithSameDependencies() {
        AtomicInteger computeCount = new AtomicInteger(0);
        int dependency = 42;

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            }, dependency);
        });

        // Second render with same dependency
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            }, dependency);
        });

        assertEquals(1, computeCount.get(), "Should not recompute with same dependencies");
    }

    @Test
    void testUseMemoRecomputesWithDifferentDependencies() {
        AtomicInteger computeCount = new AtomicInteger(0);

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "v1";
            }, 1);
        });

        // Second render with different dependency
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "v2";
            }, 2);
        });

        assertEquals(2, computeCount.get(), "Should recompute with different dependencies");
    }

    @Test
    void testUseMemoWithMultipleDependencies() {
        AtomicInteger computeCount = new AtomicInteger(0);

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            }, "a", 1, true);
        });

        // Same dependencies
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            }, "a", 1, true);
        });

        assertEquals(1, computeCount.get());

        // One dependency changed
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            }, "a", 2, true);
        });

        assertEquals(2, computeCount.get());
    }

    @Test
    void testUseMemoOutsideContextComputesDirectly() {
        AtomicInteger computeCount = new AtomicInteger(0);

        String result = StateSlot.useMemo(() -> {
            computeCount.incrementAndGet();
            return "direct";
        }, 1);

        assertEquals("direct", result);
        assertEquals(1, computeCount.get());
    }

    @Test
    void testUseMemoWithEmptyDependencies() {
        AtomicInteger computeCount = new AtomicInteger(0);

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            }); // No dependencies
        });

        // Second render - empty deps should still match
        StateSlot.withContext(context, () -> {
            StateSlot.useMemo(() -> {
                computeCount.incrementAndGet();
                return "computed";
            });
        });

        assertEquals(1, computeCount.get());
    }

    // ==================== useEffect Tests ====================

    @Test
    void testUseEffectRunsAfterMount() {
        List<String> effects = new ArrayList<>();

        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("effect1"));
            StateSlot.useEffect(() -> effects.add("effect2"));
        });

        assertTrue(effects.isEmpty(), "Effects should not run immediately");

        context.runEffects();

        assertEquals(2, effects.size());
        assertEquals("effect1", effects.get(0));
        assertEquals("effect2", effects.get(1));
    }

    @Test
    void testUseEffectDoesNotRerunWithSameDependencies() {
        List<String> effects = new ArrayList<>();
        int dep = 42;

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("effect"), dep);
        });
        context.runEffects();

        // Second render with same dep
        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("effect"), dep);
        });
        context.runEffects();

        assertEquals(1, effects.size(), "Effect should only run once with same dependencies");
    }

    @Test
    void testUseEffectRerunsWithDifferentDependencies() {
        List<String> effects = new ArrayList<>();

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("v1"), 1);
        });
        context.runEffects();

        // Second render with different dep
        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("v2"), 2);
        });
        context.runEffects();

        assertEquals(2, effects.size());
    }

    @Test
    void testUseEffectOutsideContextIgnored() {
        List<String> effects = new ArrayList<>();

        // Should not throw, just ignored
        StateSlot.useEffect(() -> effects.add("ignored"));

        assertTrue(effects.isEmpty());
    }

    @Test
    void testMultipleEffectsWithDifferentDependencies() {
        List<String> effects = new ArrayList<>();

        // First render
        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("effect1"), "dep1");
            StateSlot.useEffect(() -> effects.add("effect2"), "dep2");
        });
        context.runEffects();

        effects.clear();

        // Second render - only second effect has changed deps
        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> effects.add("effect1"), "dep1"); // Same
            StateSlot.useEffect(() -> effects.add("effect2"), "dep2-changed"); // Changed
        });
        context.runEffects();

        assertEquals(1, effects.size());
        assertEquals("effect2", effects.get(0));
    }

    // ==================== StateContext Tests ====================

    @Test
    void testStateContextCurrentContext() {
        assertNull(StateSlot.currentContext());

        StateSlot.withContext(context, () -> {
            assertSame(context, StateSlot.currentContext());
        });

        assertNull(StateSlot.currentContext());
    }

    @Test
    void testStateContextNestedContexts() {
        StateSlot.StateContext outer = new StateSlot.StateContext();
        StateSlot.StateContext inner = new StateSlot.StateContext();

        StateSlot.withContext(outer, () -> {
            assertSame(outer, StateSlot.currentContext());

            StateSlot.withContext(inner, () -> {
                assertSame(inner, StateSlot.currentContext());
            });

            assertSame(outer, StateSlot.currentContext());
        });
    }

    @Test
    void testStateContextClearDirty() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> state = StateSlot.useState(0);
            state.set(1);
        });

        assertTrue(context.isDirty());
        context.clearDirty();
        assertFalse(context.isDirty());
    }

    @Test
    void testStateContextInScope() {
        assertFalse(StateSlot.currentContext() != null);

        StateSlot.withContext(context, () -> {
            assertTrue(StateSlot.currentContext() != null);
        });
    }

    @Test
    void testStateContextWithReturnValue() {
        String result = StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<String> state = StateSlot.useState("hello");
            return state.get().toUpperCase();
        });

        assertEquals("HELLO", result);
    }

    // ==================== Complex Scenarios ====================

    @Test
    void testComplexStateInteraction() {
        // Simulate a counter component with derived state
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> count = StateSlot.useState(0);
            StateSlot.StateAccessor<Integer> multiplier = StateSlot.useState(2);

            int derived = StateSlot.useMemo(
                () -> count.get() * multiplier.get(),
                count.get(), multiplier.get()
            );

            assertEquals(0, derived);

            count.set(5);
        });

        // Re-render should update derived
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<Integer> count = StateSlot.useState(0);
            StateSlot.StateAccessor<Integer> multiplier = StateSlot.useState(2);

            int derived = StateSlot.useMemo(
                () -> count.get() * multiplier.get(),
                count.get(), multiplier.get()
            );

            assertEquals(10, derived);
        });
    }

    @Test
    void testStateAccessorToString() {
        StateSlot.withContext(context, () -> {
            StateSlot.StateAccessor<String> state = StateSlot.useState("test-value");
            String str = state.toString();
            assertTrue(str.contains("test-value"));
        });
    }

    @Test
    void testManyStatesInSingleContext() {
        StateSlot.withContext(context, () -> {
            List<StateSlot.StateAccessor<Integer>> states = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                states.add(StateSlot.useState(i));
            }

            for (int i = 0; i < 100; i++) {
                assertEquals(i, states.get(i).get());
            }
        });
    }

    @Test
    void testEffectExecutionOrder() {
        List<Integer> order = new ArrayList<>();

        StateSlot.withContext(context, () -> {
            StateSlot.useEffect(() -> order.add(1));
            StateSlot.useEffect(() -> order.add(2));
            StateSlot.useEffect(() -> order.add(3));
        });

        context.runEffects();

        assertEquals(List.of(1, 2, 3), order);
    }
}
