package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * React-like hooks API for StatefulBlueprint components.
 * <p>
 * Hooks allow you to use state and other features inside stateful blueprints
 * without creating Signal instances manually. The key benefit is that hook
 * state persists across re-renders - the same Signal/value is returned each time.
 * <p>
 * <b>Rules of Hooks:</b>
 * <ol>
 *   <li>Only call hooks inside StatefulBlueprint builder functions</li>
 *   <li>Don't call hooks inside conditions, loops, or nested functions</li>
 *   <li>Hooks must be called in the same order every render</li>
 * </ol>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.state.Hooks.*;
 * 
 * StatefulBlueprint counter = Stateful(() -> {
 *     // State persists across rebuilds
 *     Signal<Integer> count = useState(0);
 *     
 *     // Memoized computation
 *     String display = useMemo(() -> "Count: " + count.get(), count);
 *     
 *     // Effect runs after render
 *     useEffect(() -> {
 *         System.out.println("Count changed to: " + count.peek());
 *         return () -> System.out.println("Cleanup");
 *     }, count);
 *     
 *     return Column(() -> {
 *         Text(display);
 *         Button("Increment", () -> count.update(n -> n + 1));
 *     });
 * });
 * }</pre>
 *
 * @see HookContext
 */
@ApiStatus.Experimental
public final class Hooks {

    private Hooks() {}

    // ==================== State Hooks ====================

    /**
     * Creates a state that persists across re-renders.
     * <p>
     * Unlike creating a Signal directly inside a builder, useState ensures
     * the same Signal instance is returned on every render.
     * <p>
     * Example:
     * <pre>{@code
     * Signal<Integer> count = useState(0);
     * // count is the same Signal instance every render
     * }</pre>
     *
     * @param initialValue the initial value (only used on first render)
     * @param <T>          the state type
     * @return the state signal
     */
    public static <T> Signal<T> useState(T initialValue) {
        return HookContext.require().useState(() -> initialValue);
    }

    /**
     * Creates a state with lazy initialization.
     * <p>
     * The supplier is only called on the first render.
     *
     * @param initialValue supplier for the initial value
     * @param <T>          the state type
     * @return the state signal
     */
    public static <T> Signal<T> useState(Supplier<T> initialValue) {
        return HookContext.require().useState(initialValue);
    }

    // ==================== Memoization Hooks ====================

    /**
     * Memoizes a computed value.
     * <p>
     * The computation is only re-executed when dependencies change.
     * This is useful for expensive calculations.
     * <p>
     * Example:
     * <pre>{@code
     * // Only recomputes when items changes
     * int total = useMemo(() -> items.stream().mapToInt(Item::price).sum(), items);
     * }</pre>
     *
     * @param compute      the computation function
     * @param dependencies values to watch for changes
     * @param <T>          the result type
     * @return the memoized value
     */
    public static <T> T useMemo(Supplier<T> compute, Object... dependencies) {
        return HookContext.require().useMemo(compute, dependencies);
    }

    /**
     * Memoizes a callback function.
     * <p>
     * Returns the same function instance unless dependencies change.
     * Useful for passing callbacks to child components that rely on reference equality.
     * <p>
     * Example:
     * <pre>{@code
     * Runnable onClick = useCallback(
     *     () -> doSomething(id),
     *     id  // Only recreate if id changes
     * );
     * }</pre>
     *
     * @param callback     the callback to memoize
     * @param dependencies values to watch for changes
     * @param <T>          the callback type
     * @return the memoized callback
     */
    public static <T> T useCallback(T callback, Object... dependencies) {
        return HookContext.require().useCallback(callback, dependencies);
    }

    // ==================== Effect Hooks ====================

    /**
     * Runs an effect after render.
     * <p>
     * Effects are useful for side effects like logging, subscriptions, or
     * manual DOM manipulation. The effect can optionally return a cleanup function.
     * <p>
     * Example:
     * <pre>{@code
     * // Runs after every render
     * useEffect(() -> {
     *     System.out.println("Rendered!");
     *     return null;
     * });
     * 
     * // Runs only when count changes
     * useEffect(() -> {
     *     System.out.println("Count: " + count.peek());
     *     return () -> System.out.println("Cleanup");
     * }, count);
     * }</pre>
     *
     * @param effect       the effect function (returns optional cleanup)
     * @param dependencies values to watch for changes (empty = run every render)
     */
    public static void useEffect(Supplier<Runnable> effect, Object... dependencies) {
        HookContext.require().useEffect(effect, dependencies);
    }

    /**
     * Runs an effect that doesn't need cleanup.
     *
     * @param effect       the effect function
     * @param dependencies values to watch for changes
     */
    public static void useEffectSimple(Runnable effect, Object... dependencies) {
        useEffect(() -> {
            effect.run();
            return null;
        }, dependencies);
    }

    /**
     * Runs an effect only once on mount.
     *
     * @param effect the effect function (returns optional cleanup)
     */
    public static void useMount(Supplier<Runnable> effect) {
        // Empty dependency array = only run on mount
        useEffect(effect, new Object[]{});
    }

    /**
     * Runs an effect on every render (no dependencies).
     *
     * @param effect the effect function (returns optional cleanup)
     */
    public static void useRender(Supplier<Runnable> effect) {
        // No dependencies passed = always run
        HookContext.require().useEffect(effect);
    }

    // ==================== Ref Hooks ====================

    /**
     * Creates a mutable ref that persists across renders.
     * <p>
     * Unlike state, changing a ref doesn't trigger a re-render.
     * Useful for storing values that you want to persist but don't affect rendering.
     * <p>
     * Example:
     * <pre>{@code
     * Ref<Integer> renderCount = useRef(0);
     * renderCount.set(renderCount.get() + 1);
     * // Doesn't cause re-render, just stores the value
     * }</pre>
     *
     * @param initialValue the initial value
     * @param <T>          the ref value type
     * @return the ref
     */
    public static <T> HookContext.Ref<T> useRef(T initialValue) {
        return HookContext.require().useRef(initialValue);
    }

    /**
     * Creates a ref initialized to null.
     *
     * @param <T> the ref value type
     * @return the ref
     */
    public static <T> HookContext.Ref<T> useRef() {
        return useRef(null);
    }

    // ==================== Reducer Hooks ====================

    /**
     * Creates a reducer-based state management hook.
     * <p>
     * Useful for complex state logic that involves multiple sub-values
     * or when the next state depends on the previous one.
     * <p>
     * Example:
     * <pre>{@code
     * record State(int count, String message) {}
     * enum Action { INCREMENT, DECREMENT, RESET }
     * 
     * var result = useReducer(args -> {
     *     State state = args.state();
     *     return switch (args.action()) {
     *         case INCREMENT -> new State(state.count() + 1, "Incremented");
     *         case DECREMENT -> new State(state.count() - 1, "Decremented");
     *         case RESET -> new State(0, "Reset");
     *     };
     * }, new State(0, "Initial"));
     * 
     * Signal<State> state = result.state();
     * Consumer<Action> dispatch = result.dispatch();
     * 
     * // Later: dispatch.accept(Action.INCREMENT);
     * }</pre>
     *
     * @param reducer      the reducer function
     * @param initialState the initial state
     * @param <S>          the state type
     * @param <A>          the action type
     * @return the reducer result with state and dispatch
     */
    public static <S, A> HookContext.ReducerResult<S, A> useReducer(
            Function<HookContext.ReducerArgs<S, A>, S> reducer,
            S initialState
    ) {
        return HookContext.require().useReducer(reducer, initialState);
    }

    // ==================== Derived State Hooks ====================

    /**
     * Creates a derived state from a computation.
     * <p>
     * Similar to {@link Computed#of(Supplier)}, but managed by the hook system
     * to ensure proper lifecycle management.
     *
     * @param compute the computation
     * @param <T>     the result type
     * @return the computed value (recomputed each render if dependencies changed)
     */
    public static <T> Computed<T> useComputed(Supplier<T> compute) {
        return HookContext.require().useMemo(() -> Computed.of(compute));
    }

    // ==================== Context Hooks ====================

    /**
     * Checks if currently inside a hook scope.
     * <p>
     * Useful for libraries that want to conditionally use hooks.
     *
     * @return true if hooks can be called
     */
    public static boolean isInHookScope() {
        return HookContext.isInHookScope();
    }
}
