package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.signal;

/**
 * Tests for the StateCapture utility.
 */
@ExtendWith(EphemeralTestServerProvider.class)
public class StateCaptureTest {

    @Nested
    @DisplayName("Basic Capture Tests")
    class BasicCaptureTests {

        @Test
        @DisplayName("Should capture single signal creation")
        void testCaptureSingleSignal(MinecraftServer server) {
            try (StateCapture capture = StateCapture.start()) {
                Signal<Integer> count = signal(0);

                List<Signal<?>> captured = capture.capturedSignals();
                Assertions.assertEquals(1, captured.size(), "Should capture exactly one signal");
                Assertions.assertSame(count, captured.get(0), "Captured signal should be the created one");
            }
        }

        @Test
        @DisplayName("Should capture multiple signals in order")
        void testCaptureMultipleSignals(MinecraftServer server) {
            try (StateCapture capture = StateCapture.start()) {
                Signal<Integer> count = signal(0);
                Signal<String> name = signal("Hello");
                Signal<Boolean> flag = signal(true);

                List<Signal<?>> captured = capture.capturedSignals();
                Assertions.assertEquals(3, captured.size(), "Should capture all three signals");
                Assertions.assertSame(count, captured.get(0), "First signal should be count");
                Assertions.assertSame(name, captured.get(1), "Second signal should be name");
                Assertions.assertSame(flag, captured.get(2), "Third signal should be flag");
            }
        }

        @Test
        @DisplayName("Should not capture signals outside scope")
        void testNoCaptureOutsideScope(MinecraftServer server) {
            Signal<Integer> before = signal(0);

            List<Signal<?>> captured;
            try (StateCapture capture = StateCapture.start()) {
                Signal<Integer> inside = signal(1);
                captured = new ArrayList<>(capture.capturedSignals());
            }

            Signal<Integer> after = signal(2);

            Assertions.assertEquals(1, captured.size(), "Should only capture signal created inside scope");
        }

        @Test
        @DisplayName("Should return empty list when no signals created")
        void testEmptyCapture(MinecraftServer server) {
            try (StateCapture capture = StateCapture.start()) {
                // No signals created

                Assertions.assertTrue(capture.captured().isEmpty(), "Should have no captures");
                Assertions.assertFalse(capture.hasCaptures(), "hasCaptures should be false");
                Assertions.assertEquals(0, capture.count(), "count should be 0");
            }
        }
    }

    @Nested
    @DisplayName("Callback Tests")
    class CallbackTests {

        @Test
        @DisplayName("Should invoke callback for each signal")
        void testCaptureCallback(MinecraftServer server) {
            AtomicInteger callCount = new AtomicInteger(0);
            List<Signal<?>> callbackSignals = new ArrayList<>();

            try (StateCapture capture = StateCapture.startForSignals(signal -> {
                callCount.incrementAndGet();
                callbackSignals.add(signal);
            })) {
                Signal<Integer> a = signal(1);
                Signal<Integer> b = signal(2);
                Signal<Integer> c = signal(3);
            }

            Assertions.assertEquals(3, callCount.get(), "Callback should be invoked 3 times");
            Assertions.assertEquals(3, callbackSignals.size(), "All signals should be passed to callback");
        }
    }

    @Nested
    @DisplayName("Nesting Tests")
    class NestingTests {

        @Test
        @DisplayName("Should support nested capture scopes")
        void testNestedCapture(MinecraftServer server) {
            try (StateCapture outer = StateCapture.start()) {
                Signal<Integer> outerSignal1 = signal(1);

                try (StateCapture inner = StateCapture.start()) {
                    Signal<Integer> innerSignal = signal(2);

                    // Inner scope only captures its own signals
                    Assertions.assertEquals(1, inner.count(), "Inner scope should have 1 signal");
                    Assertions.assertSame(innerSignal, inner.captured().get(0));
                }

                Signal<Integer> outerSignal2 = signal(3);

                // Outer scope captures signals created directly in outer scope (not inner)
                // Each scope is independent - signals go to the active (innermost) scope
                Assertions.assertEquals(2, outer.count(), 
                    "Outer scope should have 2 signals (created when outer was active)");
            }
        }

        @Test
        @DisplayName("Should restore previous scope after close")
        void testScopeRestoration(MinecraftServer server) {
            Assertions.assertFalse(StateCapture.isCapturing(), "Should not be capturing initially");

            try (StateCapture outer = StateCapture.start()) {
                Assertions.assertTrue(StateCapture.isCapturing(), "Should be capturing in outer");

                try (StateCapture inner = StateCapture.start()) {
                    Assertions.assertTrue(StateCapture.isCapturing(), "Should be capturing in inner");
                }

                Assertions.assertTrue(StateCapture.isCapturing(), "Should still be capturing in outer after inner closes");
            }

            Assertions.assertFalse(StateCapture.isCapturing(), "Should not be capturing after all scopes close");
        }
    }

    @Nested
    @DisplayName("Convenience Method Tests")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("captureFrom(Runnable) should return captured states")
        void testCaptureFromRunnable(MinecraftServer server) {
            List<ReactiveState<?>> captured = StateCapture.captureFrom(() -> {
                signal(1);
                signal("hello");
                signal(true);
            });

            Assertions.assertEquals(3, captured.size(), "Should capture 3 states");
        }

        @Test
        @DisplayName("captureSignalsFrom(Runnable) should return captured signals")
        void testCaptureSignalsFromRunnable(MinecraftServer server) {
            List<Signal<?>> captured = StateCapture.captureSignalsFrom(() -> {
                signal(1);
                signal("hello");
                signal(true);
            });

            Assertions.assertEquals(3, captured.size(), "Should capture 3 signals");
        }

        @Test
        @DisplayName("captureFrom(Supplier) should return result and captured signals")
        void testCaptureFromSupplier(MinecraftServer server) {
            StateCapture.CaptureResult<String> result = StateCapture.captureFrom(() -> {
                Signal<Integer> count = signal(0);
                Signal<String> name = signal("test");
                return "result";
            });

            Assertions.assertEquals("result", result.result(), "Should return supplier result");
            Assertions.assertEquals(2, result.signalCount(), "Should capture 2 signals");
        }
    }

    @Nested
    @DisplayName("Summary Tests")
    class SummaryTests {

        @Test
        @DisplayName("Should generate readable summary")
        void testSummary(MinecraftServer server) {
            try (StateCapture capture = StateCapture.start()) {
                signal(42);
                signal("Hello World");
                signal(true);

                String summary = capture.getSummary();

                System.out.println(summary);

                Assertions.assertTrue(summary.contains("Total states created: 3"),
                        "Summary should contain state count");
                Assertions.assertTrue(summary.contains("42"),
                        "Summary should contain integer value");
                Assertions.assertTrue(summary.contains("Hello World"),
                        "Summary should contain string value");
            }
        }

        @Test
        @DisplayName("Should capture stack traces when enabled")
        void testStackTraceCapture(MinecraftServer server) {
            try (StateCapture capture = StateCapture.startWithStackTraces()) {
                signal(100);

                Assertions.assertFalse(capture.getCreationInfos().isEmpty(),
                        "Should have creation info");

                StateCapture.CreationInfo info = capture.getCreationInfos().get(0);
                Assertions.assertNotNull(info.stackTrace(), "Should have stack trace");

                String formatted = info.formatStackTrace();
                System.out.println("Stack trace:\n" + formatted);

                Assertions.assertTrue(formatted.contains("Signal<"),
                        "Formatted trace should contain signal type info");
            }
        }
    }

    @Nested
    @DisplayName("Signal.empty() Tests")
    class EmptySignalTests {

        @Test
        @DisplayName("Should capture Signal.empty() creation")
        void testCaptureEmptySignal(MinecraftServer server) {
            try (StateCapture capture = StateCapture.start()) {
                Signal<String> empty = Signal.empty();

                Assertions.assertEquals(1, capture.signalCount(), "Should capture empty signal");
                Assertions.assertSame(empty, capture.capturedSignals().get(0));
            }
        }
    }
}
