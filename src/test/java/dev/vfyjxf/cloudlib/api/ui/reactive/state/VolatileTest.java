package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Volatile state type.
 */
@DisplayName("Volatile State Tests")
class VolatileTest {

    @Nested
    @DisplayName("Basic Volatile")
    class BasicVolatileTests {

        @Test
        @DisplayName("of() creates always-fresh volatile")
        void ofCreatesAlwaysFresh() {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.of(counter::incrementAndGet);

            // Each get() returns a fresh value
            assertEquals(1, vol.get());
            assertEquals(2, vol.get());
            assertEquals(3, vol.get());
        }

        @Test
        @DisplayName("Volatile is not stable")
        void volatileIsNotStable() {
            Volatile<Long> vol = Volatile.of(System::currentTimeMillis);
            assertFalse(vol.isStable());
            assertTrue(vol.isVolatile());
        }

        @Test
        @DisplayName("Volatile is always dirty")
        void volatileIsAlwaysDirty() {
            Volatile<Long> vol = Volatile.of(System::currentTimeMillis);
            assertTrue(vol.isDirty());
            vol.clearDirty();
            assertTrue(vol.isDirty()); // Still dirty
        }

        @Test
        @DisplayName("peek() also returns fresh value for always-dirty")
        void peekReturnsFreshValue() {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.of(counter::incrementAndGet);

            assertEquals(1, vol.peek());
            assertEquals(2, vol.peek());
        }
    }

    @Nested
    @DisplayName("Cached Volatile")
    class CachedVolatileTests {

        @Test
        @DisplayName("cached() caches value until invalidated")
        void cachedCachesValue() {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.cached(counter::incrementAndGet);

            // First access computes
            assertEquals(1, vol.get());
            // Subsequent accesses use cache
            assertEquals(1, vol.get());
            assertEquals(1, vol.get());

            // Invalidate and get fresh value
            vol.invalidate();
            assertEquals(2, vol.get());
            assertEquals(2, vol.get());
        }

        @Test
        @DisplayName("cached volatile is still not stable")
        void cachedIsNotStable() {
            Volatile<Integer> vol = Volatile.cached(() -> 42);
            assertFalse(vol.isStable());
        }
    }

    @Nested
    @DisplayName("Polled Volatile")
    class PolledVolatileTests {

        @Test
        @DisplayName("polled() refreshes after interval")
        void polledRefreshesAfterInterval() throws InterruptedException {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.polled(counter::incrementAndGet, Duration.ofMillis(50));

            // First access computes
            assertEquals(1, vol.get());
            // Within interval, uses cache
            assertEquals(1, vol.get());

            // Wait for interval
            Thread.sleep(60);

            // After interval, refreshes
            assertEquals(2, vol.get());
        }
    }

    @Nested
    @DisplayName("Convenience Factory Methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("currentTimeMillis() returns current time")
        void currentTimeMillis() {
            Volatile<Long> time = Volatile.currentTimeMillis();
            long before = System.currentTimeMillis();
            long value = time.get();
            long after = System.currentTimeMillis();

            assertTrue(value >= before && value <= after);
        }

        @Test
        @DisplayName("nanoTime() returns nano time")
        void nanoTime() {
            Volatile<Long> time = Volatile.nanoTime();
            long before = System.nanoTime();
            long value = time.get();
            long after = System.nanoTime();

            assertTrue(value >= before && value <= after);
        }

        @Test
        @DisplayName("random() returns different values")
        void random() {
            Volatile<Double> rand = Volatile.random();

            // Should be different (extremely unlikely to be same)
            double v1 = rand.get();
            double v2 = rand.get();
            assertNotEquals(v1, v2);

            // Should be in range [0, 1)
            assertTrue(v1 >= 0 && v1 < 1);
        }
    }

    @Nested
    @DisplayName("Subscriptions")
    class SubscriptionTests {

        @Test
        @DisplayName("subscribe() receives notifications on invalidate")
        void subscribeReceivesNotifications() {
            AtomicInteger counter = new AtomicInteger(0);
            AtomicInteger notifications = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.cached(counter::incrementAndGet);

            vol.subscribe(v -> notifications.incrementAndGet());

            // First access
            vol.get();
            // Invalidate should trigger notification if value changed
            vol.invalidate();

            assertTrue(notifications.get() >= 1);
        }

        @Test
        @DisplayName("pulse() forces notification")
        void pulseNotifies() {
            AtomicInteger notifications = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.cached(() -> 42);

            vol.subscribe(v -> notifications.incrementAndGet());

            // pulse() should notify even without get()
            vol.pulse();
            assertTrue(notifications.get() >= 1, "pulse() should trigger notification");
        }
    }

    @Nested
    @DisplayName("Computed with Volatile Dependencies")
    class ComputedWithVolatileTests {

        @Test
        @DisplayName("Computed depending on Volatile is not stable")
        void computedWithVolatileNotStable() {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.of(counter::incrementAndGet);

            Computed<Integer> doubled = Computed.of(() -> vol.get() * 2);

            assertFalse(doubled.isStable());
        }

        @Test
        @DisplayName("Computed recomputes when Volatile accessed")
        void computedRecomputes() {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.of(counter::incrementAndGet);

            Computed<Integer> doubled = Computed.of(() -> vol.get() * 2);

            // Each access triggers recomputation since Volatile is always fresh
            assertEquals(2, doubled.get());  // 1 * 2
            assertEquals(4, doubled.get());  // 2 * 2
            assertEquals(6, doubled.get());  // 3 * 2
        }

        @Test
        @DisplayName("Computed with Signal is stable")
        void computedWithSignalIsStable() {
            Signal<Integer> signal = Signal.of(10);
            Computed<Integer> doubled = Computed.of(() -> signal.get() * 2);

            assertTrue(doubled.isStable());
        }

        @Test
        @DisplayName("Computed with mixed dependencies inherits volatility")
        void computedWithMixedDependencies() {
            Signal<Integer> signal = Signal.of(10);
            Volatile<Integer> vol = Volatile.of(() -> 5);

            Computed<Integer> sum = Computed.of(() -> signal.get() + vol.get());

            // Mixed: has volatile dependency, so not stable
            assertFalse(sum.isStable());
        }
    }

    @Nested
    @DisplayName("Signal Stability")
    class SignalStabilityTests {

        @Test
        @DisplayName("Signal is stable")
        void signalIsStable() {
            Signal<Integer> signal = Signal.of(42);
            assertTrue(signal.isStable());
            assertFalse(signal.isVolatile());
        }
    }

    @Nested
    @DisplayName("Version Tracking")
    class VersionTests {

        @Test
        @DisplayName("Volatile version increments on each access (always-dirty)")
        void versionIncrementsOnAccess() {
            Volatile<Long> vol = Volatile.of(System::currentTimeMillis);

            long v1 = vol.getVersion();
            vol.get();
            long v2 = vol.getVersion();

            assertTrue(v2 > v1);
        }

        @Test
        @DisplayName("Cached volatile version increments on value change")
        void cachedVersionIncrementsOnChange() {
            AtomicInteger counter = new AtomicInteger(0);
            Volatile<Integer> vol = Volatile.cached(counter::incrementAndGet);

            vol.get(); // Initial computation
            long v1 = vol.getVersion();

            vol.invalidate();
            vol.get(); // Recomputation with new value

            long v2 = vol.getVersion();
            assertTrue(v2 > v1);
        }
    }

    @Nested
    @DisplayName("StateCapture Integration")
    class StateCaptureTests {

        @Test
        @DisplayName("StateCapture captures Volatile creation")
        void stateCapturesCapturesVolatile() {
            try (StateCapture capture = StateCapture.start()) {
                Volatile<Long> time = Volatile.currentTimeMillis();
                Volatile<Double> rand = Volatile.random();

                assertEquals(2, capture.volatileCount());
                assertEquals(0, capture.signalCount());
                assertEquals(2, capture.count());

                List<Volatile<?>> volatiles = capture.capturedVolatiles();
                assertSame(time, volatiles.get(0));
                assertSame(rand, volatiles.get(1));
            }
        }

        @Test
        @DisplayName("StateCapture captures mixed Signal and Volatile")
        void stateCapturesMixed() {
            try (StateCapture capture = StateCapture.start()) {
                Signal<Integer> signal = Signal.of(42);
                Volatile<Long> time = Volatile.currentTimeMillis();
                Signal<String> name = Signal.of("test");

                assertEquals(3, capture.count());
                assertEquals(2, capture.signalCount());
                assertEquals(1, capture.volatileCount());
            }
        }

        @Test
        @DisplayName("Summary includes volatile info")
        void summaryIncludesVolatile() {
            try (StateCapture capture = StateCapture.start()) {
                Volatile.cached(() -> 100);

                String summary = capture.getSummary();
                assertTrue(summary.contains("Volatile"), "Summary should mention Volatile");
                assertTrue(summary.contains("[volatile]"), "Summary should mark volatile states");
            }
        }
    }
}
