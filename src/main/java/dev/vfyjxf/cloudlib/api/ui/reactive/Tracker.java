package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Unified dependency tracker for the reactive system.
 * <p>
 * This is the single source of truth for dependency tracking in the entire system.
 * Both {@link Computed} (for auto-recomputation) and ComponentElement (for fine-grained
 * recomposition) use this tracker.
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Start tracking
 * try (var scope = Tracker.start()) {
 *     // Any Signal.get() or Computed.get() calls will be captured
 *     var value = signal.get();
 *     
 *     // Get captured dependencies
 *     Set<ReactiveState<?>> deps = scope.captured();
 * }
 * }</pre>
 */
public final class Tracker implements AutoCloseable {
    
    /** Current active tracker (thread-local for nested tracking support) */
    private static final ThreadLocal<Tracker> CURRENT = new ThreadLocal<>();
    
    /** Captured dependencies in this scope */
    private final Set<ReactiveState<?>> captured = new HashSet<>();
    
    /** Previous tracker (for nesting) */
    private final @Nullable Tracker previous;
    
    /** Whether this scope is closed */
    private boolean closed = false;
    
    /** Optional callback when element needs rebuild */
    private final @Nullable Consumer<ReactiveState<?>> onCapture;
    
    private Tracker(@Nullable Consumer<ReactiveState<?>> onCapture) {
        this.previous = CURRENT.get();
        this.onCapture = onCapture;
        CURRENT.set(this);
    }
    
    /**
     * Starts a new tracking scope.
     *
     * @return the tracking scope (use with try-with-resources)
     */
    public static Tracker start() {
        return new Tracker(null);
    }
    
    /**
     * Starts a new tracking scope with a capture callback.
     *
     * @param onCapture callback invoked for each captured dependency
     * @return the tracking scope
     */
    public static Tracker start(Consumer<ReactiveState<?>> onCapture) {
        return new Tracker(onCapture);
    }
    
    /**
     * Captures a reactive state as a dependency.
     * Called from {@code ReactiveState.get()} methods.
     *
     * @param state the state being read
     */
    public static void capture(ReactiveState<?> state) {
        Tracker current = CURRENT.get();
        if (current != null && !current.closed) {
            current.captured.add(state);
            if (current.onCapture != null) {
                current.onCapture.accept(state);
            }
        }
    }
    
    /**
     * Checks if currently tracking.
     *
     * @return true if inside a tracking scope
     */
    public static boolean isTracking() {
        Tracker current = CURRENT.get();
        return current != null && !current.closed;
    }
    
    /**
     * Gets captured dependencies.
     *
     * @return immutable set of captured dependencies
     */
    public Set<ReactiveState<?>> captured() {
        return Set.copyOf(captured);
    }
    
    /**
     * Gets captured dependencies as mutable set.
     *
     * @return mutable set of captured dependencies
     */
    public Set<ReactiveState<?>> capturedMutable() {
        return new HashSet<>(captured);
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            CURRENT.set(previous);
        }
    }
}
