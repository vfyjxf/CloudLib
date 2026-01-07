package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the core reactive system.
 */
public class ReactiveSystemTest {
    
    // ===== Signal Tests =====
    
    @Nested
    @DisplayName("Signal")
    class SignalTests {
        
        @Test
        @DisplayName("Signal holds initial value")
        void signalHoldsInitialValue() {
            Signal<Integer> count = Signal.of(42);
            assertEquals(42, count.get());
        }
        
        @Test
        @DisplayName("Signal updates value")
        void signalUpdatesValue() {
            Signal<String> name = Signal.of("Alice");
            name.set("Bob");
            assertEquals("Bob", name.get());
        }
        
        @Test
        @DisplayName("Signal update function")
        void signalUpdateFunction() {
            Signal<Integer> count = Signal.of(10);
            count.update(n -> n * 2);
            assertEquals(20, count.get());
        }
        
        @Test
        @DisplayName("Signal notifies subscribers")
        void signalNotifiesSubscribers() {
            Signal<Integer> count = Signal.of(0);
            int[] notified = {0};
            
            count.subscribe(value -> notified[0] = value);
            count.set(5);
            
            assertEquals(5, notified[0]);
        }
        
        @Test
        @DisplayName("Signal subscription can be cancelled")
        void signalSubscriptionCancel() {
            Signal<Integer> count = Signal.of(0);
            int[] notified = {0};
            
            var sub = count.subscribe(value -> notified[0] = value);
            count.set(5);
            assertEquals(5, notified[0]);
            
            sub.unsubscribe();
            count.set(10);
            assertEquals(5, notified[0]); // Still 5, not notified
        }
        
        @Test
        @DisplayName("peek() does not trigger tracking")
        void peekDoesNotTrack() {
            Signal<Integer> count = Signal.of(0);
            
            try (var scope = Tracker.start()) {
                count.peek(); // Should not track
                assertTrue(scope.captured().isEmpty());
            }
            
            try (var scope = Tracker.start()) {
                count.get(); // Should track
                assertTrue(scope.captured().contains(count));
            }
        }
    }
    
    // ===== Computed Tests =====
    
    @Nested
    @DisplayName("Computed")
    class ComputedTests {
        
        @Test
        @DisplayName("Computed derives from Signal")
        void computedDerivesFromSignal() {
            Signal<Integer> count = Signal.of(5);
            Computed<String> countText = Computed.of(() -> "Count: " + count.get());
            
            assertEquals("Count: 5", countText.get());
        }
        
        @Test
        @DisplayName("Computed updates when Signal changes")
        void computedUpdatesOnChange() {
            Signal<Integer> count = Signal.of(5);
            Computed<Integer> doubled = Computed.of(() -> count.get() * 2);
            
            assertEquals(10, doubled.get());
            
            count.set(10);
            assertEquals(20, doubled.get());
        }
        
        @Test
        @DisplayName("Computed tracks multiple signals")
        void computedTracksMultipleSignals() {
            Signal<String> firstName = Signal.of("John");
            Signal<String> lastName = Signal.of("Doe");
            Computed<String> fullName = Computed.of(() -> 
                firstName.get() + " " + lastName.get()
            );
            
            assertEquals("John Doe", fullName.get());
            
            firstName.set("Jane");
            assertEquals("Jane Doe", fullName.get());
            
            lastName.set("Smith");
            assertEquals("Jane Smith", fullName.get());
        }
        
        @Test
        @DisplayName("Computed caches value")
        void computedCachesValue() {
            Signal<Integer> count = Signal.of(5);
            int[] computeCount = {0};
            
            Computed<Integer> doubled = Computed.of(() -> {
                computeCount[0]++;
                return count.get() * 2;
            });
            
            // First access computes
            assertEquals(10, doubled.get());
            assertEquals(1, computeCount[0]);
            
            // Subsequent access uses cache
            assertEquals(10, doubled.get());
            assertEquals(1, computeCount[0]);
            
            // After change, recomputes
            count.set(10);
            assertEquals(20, doubled.get());
            assertEquals(2, computeCount[0]);
        }
        
        @Test
        @DisplayName("Computed chain works")
        void computedChainWorks() {
            Signal<Integer> base = Signal.of(1);
            Computed<Integer> doubled = Computed.of(() -> base.get() * 2);
            Computed<Integer> quadrupled = Computed.of(() -> doubled.get() * 2);
            
            // First access
            assertEquals(4, quadrupled.get());
            assertEquals(2, doubled.get());
            
            // Now change base
            base.set(5);
            
            // doubled should be invalidated and recompute to 10
            assertEquals(10, doubled.get());
            
            // quadrupled should also be invalidated and recompute to 20
            assertEquals(20, quadrupled.get());
        }
    }
    
    // ===== Tracker Tests =====
    
    @Nested
    @DisplayName("Tracker")
    class TrackerTests {
        
        @Test
        @DisplayName("Tracker captures Signal reads")
        void trackerCapturesSignalReads() {
            Signal<Integer> a = Signal.of(1);
            Signal<String> b = Signal.of("hello");
            
            try (var scope = Tracker.start()) {
                a.get();
                b.get();
                
                var captured = scope.captured();
                assertEquals(2, captured.size());
                assertTrue(captured.contains(a));
                assertTrue(captured.contains(b));
            }
        }
        
        @Test
        @DisplayName("Tracker nesting works")
        void trackerNestingWorks() {
            Signal<Integer> a = Signal.of(1);
            Signal<Integer> b = Signal.of(2);
            
            try (var outer = Tracker.start()) {
                a.get();
                
                try (var inner = Tracker.start()) {
                    b.get();
                    
                    // Inner only has b
                    assertEquals(1, inner.captured().size());
                    assertTrue(inner.captured().contains(b));
                }
                
                // Outer only has a
                assertEquals(1, outer.captured().size());
                assertTrue(outer.captured().contains(a));
            }
        }
        
        @Test
        @DisplayName("Tracker.isTracking returns correct state")
        void trackerIsTrackingState() {
            assertFalse(Tracker.isTracking());
            
            try (var scope = Tracker.start()) {
                assertTrue(Tracker.isTracking());
            }
            
            assertFalse(Tracker.isTracking());
        }
    }
    
    // ===== Component Tests =====
    
    @Nested
    @DisplayName("Component")
    class ComponentTests {
        
        @Test
        @DisplayName("Pure component renders static content")
        void pureComponentRendersStatic() {
            var content = Render.text("Hello");
            Component pure = Component.pure(content);
            
            assertFalse(pure.isStateful());
            assertSame(content, pure.render(null));
        }
        
        @Test
        @DisplayName("Stateless component depends on context")
        void statelessComponentDependsOnContext() {
            Component greeting = Component.stateless(ctx -> 
                Render.text("Hello, World!")
            );
            
            assertFalse(greeting.isStateful());
            RenderNode node = greeting.render(null);
            assertNotNull(node);
        }
        
        @Test
        @DisplayName("Stateful component has state")
        void statefulComponentHasState() {
            Component counter = Component.stateful(ctx -> {
                var count = ctx.signal(0);
                return Render.text("Count: " + count.get());
            });
            
            assertTrue(counter.isStateful());
        }
    }
}
