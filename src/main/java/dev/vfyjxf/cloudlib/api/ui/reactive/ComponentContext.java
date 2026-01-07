package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Context available during component rendering.
 * <p>
 * ComponentContext provides:
 * <ul>
 *   <li>State creation and management (signals, computed values)</li>
 *   <li>Lifecycle hooks (onMount, onUnmount, effects)</li>
 *   <li>Memoization utilities</li>
 *   <li>Access to providers (dependency injection)</li>
 * </ul>
 * <p>
 * The context uses a hook-style API where order matters:
 * <pre>{@code
 * Component.stateful(ctx -> {
 *     // These must be called in the same order every render
 *     var count = ctx.signal(0);           // Hook 1
 *     var name = ctx.signal("Alice");      // Hook 2
 *     var doubled = ctx.computed(() -> count.get() * 2);  // Hook 3
 *     
 *     ctx.effect(() -> {                   // Hook 4
 *         log("Count changed: " + count.get());
 *     });
 *     
 *     return Render.text(...);
 * });
 * }</pre>
 */
public interface ComponentContext {

    // ===== State Hooks =====

    /**
     * Creates or retrieves a signal (mutable state).
     * <p>
     * Signals trigger component rebuilds when their value changes.
     *
     * @param initialValue the initial value
     * @param <T>          the value type
     * @return the signal
     */
    <T> Signal<T> signal(T initialValue);

    /**
     * Creates or retrieves a computed value.
     * <p>
     * Computed values automatically track dependencies and recompute when needed.
     *
     * @param computation the computation
     * @param <T>         the value type
     * @return the computed state
     */
    <T> Computed<T> computed(Supplier<T> computation);

    /**
     * Memoizes a value with explicit dependencies.
     * <p>
     * The value is only recomputed when dependencies change.
     *
     * @param factory      the value factory
     * @param dependencies the dependency values
     * @param <T>          the value type
     * @return the memoized value
     */
    <T> T memo(Supplier<T> factory, Object... dependencies);

    // ===== Effect Hooks =====

    /**
     * Registers a side effect that runs after render.
     *
     * @param effect the effect function
     */
    void effect(Runnable effect);

    /**
     * Registers an effect with cleanup.
     * <p>
     * The returned runnable is called when the component unmounts or before the effect re-runs.
     *
     * @param effect the effect that returns a cleanup function
     */
    void effect(Supplier<Runnable> effect);

    /**
     * Registers an effect with explicit dependencies.
     * <p>
     * The effect only re-runs when dependencies change.
     *
     * @param effect       the effect that returns a cleanup function
     * @param dependencies the dependency values
     */
    void effect(Supplier<Runnable> effect, Object... dependencies);

    /**
     * Watches a state and runs a callback when it changes.
     *
     * @param state    the state to watch
     * @param callback the callback
     */
    void watch(ReactiveState<?> state, Runnable callback);

    /**
     * Watches a state and runs a callback with the new value when it changes.
     *
     * @param state    the state to watch
     * @param callback the callback
     * @param <T>      the value type
     */
    <T> void watch(ReactiveState<T> state, Consumer<T> callback);

    // ===== Lifecycle Hooks =====

    /**
     * Registers a callback to run when mounted.
     *
     * @param callback the callback
     */
    void onMount(Runnable callback);

    /**
     * Registers a callback to run when unmounted.
     *
     * @param callback the callback
     */
    void onUnmount(Runnable callback);
    
    /**
     * Registers a callback to run on every game tick (20 times per second).
     * Useful for animations and time-based updates.
     *
     * @param callback the tick callback
     */
    void onTick(Runnable callback);

    // ===== Ref Hook =====

    /**
     * Creates or retrieves a mutable reference that doesn't trigger rebuilds.
     * <p>
     * Refs are useful for holding mutable values that don't affect rendering.
     *
     * @param name the ref name
     * @param <T>  the value type
     * @return the ref
     */
    <T> Ref<T> ref(String name);

    // ===== Provider Access =====

    /**
     * Gets a provided value by key.
     *
     * @param key the provider key
     * @param <T> the value type
     * @return the value
     */
    <T> T provide(Providers.Key<T> key);

    // ===== Utilities =====

    /**
     * Marks this component as needing rebuild.
     * <p>
     * Call this when external state changes that should trigger a re-render.
     */
    void invalidate();

    // ===== Nested Types =====

    /**
     * Mutable reference container (doesn't trigger rebuilds).
     *
     * @param <T> the value type
     */
    interface Ref<T> {
        /**
         * Gets the current value.
         *
         * @return the value or null
         */
        @Nullable T get();

        /**
         * Sets the value.
         *
         * @param value the new value
         */
        void set(@Nullable T value);
    }
}
