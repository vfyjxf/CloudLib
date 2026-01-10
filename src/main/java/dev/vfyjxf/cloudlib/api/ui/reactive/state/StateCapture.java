package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Captures all reactive state instances (Signal, Volatile) created within a scope.
 * <p>
 * This is useful for debugging and testing to track what states are being created
 * during component initialization or in a specific code block.
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Start capturing state creation
 * try (var capture = StateCapture.start()) {
 *     Signal<Integer> count = signal(0);
 *     Signal<String> name = signal("Hello");
 *     Volatile<Long> time = Volatile.currentTimeMillis();
 *
 *     // Get all created states
 *     List<ReactiveState<?>> states = capture.captured();
 *     // states contains [count, name, time]
 *
 *     // Get only signals
 *     List<Signal<?>> signals = capture.capturedSignals();
 *
 *     // Get only volatiles
 *     List<Volatile<?>> volatiles = capture.capturedVolatiles();
 * }
 *
 * // With callback
 * try (var capture = StateCapture.start(state ->
 *     System.out.println("Created: " + state))) {
 *     Signal<Integer> count = signal(0); // prints "Created: Signal{...}"
 * }
 * }</pre>
 *
 * @see Tracker for dependency tracking (reads)
 */
@ApiStatus.Experimental
public final class StateCapture implements AutoCloseable {

    /**
     * Current active capture scope (thread-local for nested support)
     */
    private static final ThreadLocal<StateCapture> CURRENT = new ThreadLocal<>();

    /**
     * Captured reactive states in this scope (signals, volatiles, etc.)
     */
    private final List<ReactiveState<?>> captured = new ArrayList<>();

    /**
     * Creation info for debugging
     */
    private final List<CreationInfo> creationInfos = new ArrayList<>();

    /**
     * Previous capture (for nesting)
     */
    private final @Nullable StateCapture previous;

    /**
     * Whether this scope is closed
     */
    private boolean closed = false;

    /**
     * Optional callback when a state is created
     */
    private final @Nullable Consumer<ReactiveState<?>> onCapture;

    /**
     * Whether to capture stack traces for debugging
     */
    private final boolean captureStackTraces;

    private StateCapture(@Nullable Consumer<ReactiveState<?>> onCapture, boolean captureStackTraces) {
        this.previous = CURRENT.get();
        this.onCapture = onCapture;
        this.captureStackTraces = captureStackTraces;
        CURRENT.set(this);
    }

    /**
     * Starts a new capture scope.
     *
     * @return the capture scope (use with try-with-resources)
     */
    public static StateCapture start() {
        return new StateCapture(null, false);
    }

    /**
     * Starts a new capture scope with a callback.
     *
     * @param onCapture callback invoked for each created state
     * @return the capture scope
     */
    public static StateCapture start(Consumer<ReactiveState<?>> onCapture) {
        return new StateCapture(onCapture, false);
    }

    /**
     * Starts a new capture scope with a Signal-specific callback.
     *
     * @param onCapture callback invoked for each created signal
     * @return the capture scope
     */
    public static StateCapture startForSignals(Consumer<Signal<?>> onCapture) {
        return new StateCapture(state -> {
            if (state instanceof Signal<?> signal) {
                onCapture.accept(signal);
            }
        }, false);
    }

    /**
     * Starts a new capture scope with stack trace capturing enabled.
     * <p>
     * This is useful for debugging to see where each signal was created.
     *
     * @return the capture scope
     */
    public static StateCapture startWithStackTraces() {
        return new StateCapture(null, true);
    }

    /**
     * Starts a new capture scope with all options.
     *
     * @param onCapture         callback invoked for each created state
     * @param captureStackTraces whether to capture creation stack traces
     * @return the capture scope
     */
    public static StateCapture start(@Nullable Consumer<ReactiveState<?>> onCapture, boolean captureStackTraces) {
        return new StateCapture(onCapture, captureStackTraces);
    }

    /**
     * Registers a reactive state as being created.
     * Called from state factory methods.
     *
     * @param state the state being created
     */
    public static void register(ReactiveState<?> state) {
        StateCapture current = CURRENT.get();
        if (current != null && !current.closed) {
            current.captured.add(state);

            if (current.captureStackTraces) {
                current.creationInfos.add(new CreationInfo(
                        state,
                        Thread.currentThread().getStackTrace(),
                        System.currentTimeMillis()
                ));
            }

            if (current.onCapture != null) {
                current.onCapture.accept(state);
            }
        }
    }

    /**
     * Registers a signal as being created.
     * Called from Signal factory methods.
     *
     * @param signal the signal being created
     * @deprecated Use {@link #register(ReactiveState)} instead
     */
    @Deprecated
    public static void register(Signal<?> signal) {
        register((ReactiveState<?>) signal);
    }

    /**
     * Checks if currently capturing.
     *
     * @return true if inside a capture scope
     */
    public static boolean isCapturing() {
        StateCapture current = CURRENT.get();
        return current != null && !current.closed;
    }

    /**
     * Gets all captured reactive states.
     *
     * @return immutable list of captured states (in creation order)
     */
    public List<ReactiveState<?>> captured() {
        return Collections.unmodifiableList(captured);
    }

    /**
     * Gets captured states as mutable list.
     *
     * @return mutable copy of captured states
     */
    public List<ReactiveState<?>> capturedMutable() {
        return new ArrayList<>(captured);
    }

    /**
     * Gets only captured signals.
     *
     * @return immutable list of captured signals
     */
    @SuppressWarnings("unchecked")
    public List<Signal<?>> capturedSignals() {
        List<Signal<?>> result = new ArrayList<>();
        for (ReactiveState<?> state : captured) {
            if (state instanceof Signal<?>) {
                result.add((Signal<?>) state);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Gets only captured volatiles.
     *
     * @return immutable list of captured volatiles
     */
    @SuppressWarnings("unchecked")
    public List<Volatile<?>> capturedVolatiles() {
        List<Volatile<?>> result = new ArrayList<>();
        for (ReactiveState<?> state : captured) {
            if (state instanceof Volatile<?>) {
                result.add((Volatile<?>) state);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Gets the number of captured states.
     *
     * @return count of captured states
     */
    public int count() {
        return captured.size();
    }

    /**
     * Gets the number of captured signals.
     *
     * @return count of captured signals
     */
    public int signalCount() {
        return (int) captured.stream().filter(s -> s instanceof Signal<?>).count();
    }

    /**
     * Gets the number of captured volatiles.
     *
     * @return count of captured volatiles
     */
    public int volatileCount() {
        return (int) captured.stream().filter(s -> s instanceof Volatile<?>).count();
    }

    /**
     * Checks if any states were captured.
     *
     * @return true if at least one state was captured
     */
    public boolean hasCaptures() {
        return !captured.isEmpty();
    }

    /**
     * Gets creation info for debugging (only available if captureStackTraces was enabled).
     *
     * @return list of creation info records
     */
    public List<CreationInfo> getCreationInfos() {
        return Collections.unmodifiableList(creationInfos);
    }

    /**
     * Gets a summary of captured states for debugging.
     *
     * @return a summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== StateCapture Summary ===\n");
        sb.append("Total states created: ").append(captured.size());
        sb.append(" (").append(signalCount()).append(" signals, ");
        sb.append(volatileCount()).append(" volatiles)\n");

        if (!captured.isEmpty()) {
            sb.append("\nCaptured states:\n");
            for (int i = 0; i < captured.size(); i++) {
                ReactiveState<?> state = captured.get(i);
                String type = getStateTypeName(state);
                sb.append(String.format("  [%d] %s<%s> = %s%s%n",
                        i,
                        type,
                        getValueTypeName(state),
                        formatValue(peekValue(state)),
                        state.isStable() ? "" : " [volatile]"
                ));
            }
        }

        if (!creationInfos.isEmpty()) {
            sb.append("\nCreation stack traces:\n");
            for (CreationInfo info : creationInfos) {
                sb.append(info.formatStackTrace()).append("\n");
            }
        }

        return sb.toString();
    }

    private String getStateTypeName(ReactiveState<?> state) {
        if (state instanceof Signal<?>) return "Signal";
        if (state instanceof Volatile<?>) return "Volatile";
        if (state instanceof Computed<?>) return "Computed";
        return state.getClass().getSimpleName();
    }

    private String getValueTypeName(ReactiveState<?> state) {
        Object value = peekValue(state);
        if (value == null) {
            return "?";
        }
        return value.getClass().getSimpleName();
    }

    @Nullable
    private Object peekValue(ReactiveState<?> state) {
        try {
            return state.peek();
        } catch (Exception e) {
            return "<error>";
        }
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        String str = value.toString();
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        return str;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            CURRENT.set(previous);
        }
    }

    // ==================== Convenience Methods ====================

    /**
     * Runs a block and returns all states created within it.
     *
     * @param block the code block to run
     * @return list of states created during the block
     */
    public static List<ReactiveState<?>> captureFrom(Runnable block) {
        try (StateCapture capture = start()) {
            block.run();
            return capture.capturedMutable();
        }
    }

    /**
     * Runs a block and returns only signals created within it.
     *
     * @param block the code block to run
     * @return list of signals created during the block
     */
    public static List<Signal<?>> captureSignalsFrom(Runnable block) {
        try (StateCapture capture = start()) {
            block.run();
            return capture.capturedSignals();
        }
    }

    /**
     * Runs a supplier and returns the result along with captured states.
     *
     * @param supplier the supplier to run
     * @param <T>      the result type
     * @return result containing the value and captured states
     */
    public static <T> CaptureResult<T> captureFrom(Supplier<T> supplier) {
        try (StateCapture capture = start()) {
            T result = supplier.get();
            return new CaptureResult<>(result, capture.capturedMutable(), capture.capturedSignals(), capture.capturedVolatiles());
        }
    }

    /**
     * Result of a capture operation containing both the result and captured states.
     *
     * @param <T> the result type
     */
    public record CaptureResult<T>(
            T result,
            List<ReactiveState<?>> states,
            List<Signal<?>> signals,
            List<Volatile<?>> volatiles
    ) {
        /**
         * Gets the total count of captured states.
         */
        public int stateCount() {
            return states.size();
        }

        /**
         * Gets the count of captured signals.
         */
        public int signalCount() {
            return signals.size();
        }

        /**
         * Gets the count of captured volatiles.
         */
        public int volatileCount() {
            return volatiles.size();
        }
    }

    /**
     * Information about when and where a state was created.
     */
    public record CreationInfo(
            ReactiveState<?> state,
            StackTraceElement[] stackTrace,
            long timestamp
    ) {
        /**
         * Formats the stack trace for display.
         */
        public String formatStackTrace() {
            StringBuilder sb = new StringBuilder();
            String typeName = state instanceof Signal<?> ? "Signal" :
                             state instanceof Volatile<?> ? "Volatile" :
                             state.getClass().getSimpleName();
            sb.append(typeName).append("<").append(getTypeName()).append("> created at:\n");

            // Skip internal frames and show relevant ones
            boolean foundUserCode = false;
            int shown = 0;
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                // Skip internal tracking classes
                if (className.contains("StateCapture") ||
                    className.contains("Signal") ||
                    className.contains("Volatile") ||
                    className.contains("Blueprints") ||
                    className.equals("java.lang.Thread")) {
                    continue;
                }
                if (!foundUserCode) {
                    foundUserCode = true;
                }
                if (foundUserCode && shown < 5) {
                    sb.append("    at ").append(element).append("\n");
                    shown++;
                }
            }

            return sb.toString();
        }

        private String getTypeName() {
            try {
                Object value = state.peek();
                return value == null ? "?" : value.getClass().getSimpleName();
            } catch (Exception e) {
                return "?";
            }
        }
    }
}
