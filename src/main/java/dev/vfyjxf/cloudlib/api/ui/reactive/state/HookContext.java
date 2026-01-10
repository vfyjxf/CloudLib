package dev.vfyjxf.cloudlib.api.ui.reactive.state;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Context for managing component-local hooks state.
 * <p>
 * HookContext stores all hook state (useState, useMemo, useEffect, etc.) for a single
 * component instance. The state persists across re-renders, ensuring that hooks
 * always return the same state objects.
 * <p>
 * This implements a React-like hooks system where:
 * <ul>
 *   <li>Hooks must be called in the same order every render</li>
 *   <li>State created via hooks persists across re-renders</li>
 *   <li>Effects can have cleanup functions</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * StatefulBlueprint counter = Stateful(() -> {
 *     // This Signal persists across rebuilds
 *     Signal<Integer> count = Hooks.useState(0);
 *     
 *     // This computed value is memoized
 *     String display = Hooks.useMemo(() -> "Count: " + count.get(), count);
 *     
 *     return Column(() -> {
 *         Text(display);
 *         Button("Increment", () -> count.update(n -> n + 1));
 *     });
 * });
 * }</pre>
 *
 * @see Hooks
 */
@ApiStatus.Experimental
public final class HookContext {

    /**
     * Current active hook context (thread-local for build scope)
     */
    private static final ThreadLocal<HookContext> CURRENT = new ThreadLocal<>();

    /**
     * All stored hook states in order
     */
    private final List<HookState<?>> hooks = new ArrayList<>();

    /**
     * Current hook index during a build pass
     */
    private int hookIndex = 0;

    /**
     * Whether this is the first build
     */
    private boolean firstBuild = true;

    /**
     * Effects that need to run after build
     */
    private final List<EffectState> pendingEffects = new ArrayList<>();

    /**
     * Gets the current hook context.
     *
     * @return the current context, or null if not in a hook scope
     */
    @Nullable
    public static HookContext current() {
        return CURRENT.get();
    }

    /**
     * Gets the current hook context, throwing if not available.
     *
     * @return the current context
     * @throws IllegalStateException if not in a hook scope
     */
    public static HookContext require() {
        HookContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "Hooks can only be called during StatefulBlueprint build. " +
                "Make sure you're calling hooks inside a Stateful(() -> ...) block."
            );
        }
        return ctx;
    }

    /**
     * Enters a hook scope for building.
     *
     * @param context the hook context to activate
     */
    public static void enter(HookContext context) {
        CURRENT.set(context);
        context.beginBuild();
    }

    /**
     * Exits the current hook scope.
     */
    public static void exit() {
        HookContext ctx = CURRENT.get();
        if (ctx != null) {
            ctx.endBuild();
        }
        CURRENT.set(null);
    }

    /**
     * Checks if currently in a hook scope.
     *
     * @return true if hooks can be called
     */
    public static boolean isInHookScope() {
        return CURRENT.get() != null;
    }

    /**
     * Prepares for a new build pass.
     */
    private void beginBuild() {
        hookIndex = 0;
    }

    /**
     * Finishes a build pass.
     */
    private void endBuild() {
        // Verify hook count consistency (only after first build)
        if (!firstBuild && hookIndex != hooks.size()) {
            throw new IllegalStateException(
                "Hook count changed between renders. Expected " + hooks.size() + 
                " hooks but got " + hookIndex + ". " +
                "Hooks must be called in the same order every render. " +
                "Don't call hooks inside conditions, loops, or nested functions."
            );
        }
        firstBuild = false;

        // Run pending effects
        runEffects();
    }

    // ==================== Hook Implementations ====================

    /**
     * Gets or creates a state hook.
     *
     * @param initialValue the initial value supplier (only called on first render)
     * @param <T>          the state type
     * @return the signal (same instance across renders)
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> useState(Supplier<T> initialValue) {
        if (firstBuild || hookIndex >= hooks.size()) {
            // First render - create new state
            Signal<T> signal = Signal.of(initialValue.get());
            hooks.add(new HookState<>(HookType.STATE, signal));
            hookIndex++;
            return signal;
        } else {
            // Subsequent render - return existing state
            HookState<?> hook = hooks.get(hookIndex++);
            validateHookType(hook, HookType.STATE);
            return (Signal<T>) hook.value;
        }
    }

    /**
     * Gets or creates a memoized value.
     *
     * @param compute      the computation function
     * @param dependencies the dependencies (recompute if any change)
     * @param <T>          the value type
     * @return the memoized value
     */
    @SuppressWarnings("unchecked")
    public <T> T useMemo(Supplier<T> compute, Object... dependencies) {
        if (firstBuild || hookIndex >= hooks.size()) {
            // First render - compute and store
            T value = compute.get();
            hooks.add(new MemoState<>(value, dependencies));
            hookIndex++;
            return value;
        } else {
            // Check if dependencies changed
            HookState<?> hook = hooks.get(hookIndex++);
            validateHookType(hook, HookType.MEMO);
            MemoState<T> memo = (MemoState<T>) hook;

            if (dependenciesChanged(memo.dependencies, dependencies)) {
                // Recompute
                T newValue = compute.get();
                memo.value = newValue;
                memo.dependencies = dependencies.clone();
                return newValue;
            }
            return memo.value;
        }
    }

    /**
     * Gets or creates a memoized callback.
     *
     * @param callback     the callback
     * @param dependencies the dependencies (recreate if any change)
     * @param <T>          the callback type
     * @return the memoized callback
     */
    @SuppressWarnings("unchecked")
    public <T> T useCallback(T callback, Object... dependencies) {
        return useMemo(() -> callback, dependencies);
    }

    /**
     * Registers an effect to run after render.
     *
     * @param effect       the effect function (optionally returns cleanup)
     * @param dependencies the dependencies (re-run if any change)
     */
    @SuppressWarnings("unchecked")
    public void useEffect(Supplier<Runnable> effect, Object... dependencies) {
        useEffectInternal(effect, dependencies, false);
    }

    /**
     * Registers an effect to run after every render (no dependencies).
     *
     * @param effect the effect function (optionally returns cleanup)
     */
    public void useEffect(Supplier<Runnable> effect) {
        // null dependencies means always run
        useEffectInternal(effect, null, true);
    }

    private void useEffectInternal(Supplier<Runnable> effect, @Nullable Object[] dependencies, boolean alwaysRun) {
        if (firstBuild || hookIndex >= hooks.size()) {
            // First render - schedule effect
            EffectState state = new EffectState(effect, dependencies, alwaysRun);
            hooks.add(state);
            hookIndex++;
            pendingEffects.add(state);
        } else {
            // Check if dependencies changed
            HookState<?> hook = hooks.get(hookIndex++);
            validateHookType(hook, HookType.EFFECT);
            EffectState state = (EffectState) hook;

            // Determine whether to re-run the effect:
            // - alwaysRun (no deps specified) = run every render
            // - empty deps array = only mount (never re-run)
            // - non-empty deps = run if deps changed
            boolean shouldRun;
            if (state.alwaysRun) {
                shouldRun = true;
            } else if (dependencies != null && dependencies.length == 0) {
                // Empty dependency array means "only on mount" - never re-run
                shouldRun = false;
            } else if (dependencies != null) {
                // Check if dependencies changed
                shouldRun = dependenciesChanged(state.dependencies, dependencies);
            } else {
                shouldRun = false;
            }

            if (shouldRun) {
                // Run cleanup and schedule new effect
                state.runCleanup();
                state.effect = effect;
                if (dependencies != null) {
                    state.dependencies = dependencies.clone();
                }
                pendingEffects.add(state);
            }
        }
    }

    /**
     * Creates a ref that persists across renders.
     *
     * @param initialValue the initial value
     * @param <T>          the ref value type
     * @return the ref
     */
    @SuppressWarnings("unchecked")
    public <T> Ref<T> useRef(T initialValue) {
        if (firstBuild || hookIndex >= hooks.size()) {
            Ref<T> ref = new Ref<>(initialValue);
            hooks.add(new HookState<>(HookType.REF, ref));
            hookIndex++;
            return ref;
        } else {
            HookState<?> hook = hooks.get(hookIndex++);
            validateHookType(hook, HookType.REF);
            return (Ref<T>) hook.value;
        }
    }

    /**
     * Creates a reducer-style state management hook.
     *
     * @param reducer      the reducer function (state, action) -> newState
     * @param initialState the initial state
     * @param <S>          the state type
     * @param <A>          the action type
     * @return a pair of [state, dispatch]
     */
    public <S, A> ReducerResult<S, A> useReducer(
            Function<ReducerArgs<S, A>, S> reducer,
            S initialState
    ) {
        Signal<S> state = useState(() -> initialState);
        Consumer<A> dispatch = action -> {
            S currentState = state.peek();
            S newState = reducer.apply(new ReducerArgs<>(currentState, action));
            state.set(newState);
        };
        return new ReducerResult<>(state, dispatch);
    }

    // ==================== Internal Methods ====================

    private void validateHookType(HookState<?> hook, HookType expected) {
        if (hook.type != expected) {
            throw new IllegalStateException(
                "Hook type mismatch. Expected " + expected + " but found " + hook.type + ". " +
                "This usually means hooks are being called in a different order than the previous render."
            );
        }
    }

    private boolean dependenciesChanged(@Nullable Object[] oldDeps, Object[] newDeps) {
        if (oldDeps == null || oldDeps.length != newDeps.length) {
            return true;
        }
        for (int i = 0; i < oldDeps.length; i++) {
            if (!Objects.equals(oldDeps[i], newDeps[i])) {
                return true;
            }
        }
        return false;
    }

    private void runEffects() {
        for (EffectState effect : pendingEffects) {
            effect.run();
        }
        pendingEffects.clear();
    }

    /**
     * Cleans up all effects (called on unmount).
     */
    public void dispose() {
        for (HookState<?> hook : hooks) {
            if (hook instanceof EffectState effect) {
                effect.runCleanup();
            }
        }
        hooks.clear();
    }

    // ==================== Inner Classes ====================

    private enum HookType {
        STATE, MEMO, EFFECT, REF
    }

    private static class HookState<T> {
        final HookType type;
        T value;

        HookState(HookType type, T value) {
            this.type = type;
            this.value = value;
        }
    }

    private static class MemoState<T> extends HookState<T> {
        Object[] dependencies;

        MemoState(T value, Object[] dependencies) {
            super(HookType.MEMO, value);
            this.dependencies = dependencies.clone();
        }
    }

    private static class EffectState extends HookState<Void> {
        Supplier<Runnable> effect;
        @Nullable
        Object[] dependencies;
        @Nullable
        Runnable cleanup;
        final boolean alwaysRun;

        EffectState(Supplier<Runnable> effect, @Nullable Object[] dependencies, boolean alwaysRun) {
            super(HookType.EFFECT, null);
            this.effect = effect;
            this.dependencies = dependencies != null ? dependencies.clone() : null;
            this.alwaysRun = alwaysRun;
        }

        void run() {
            cleanup = effect.get();
        }

        void runCleanup() {
            if (cleanup != null) {
                cleanup.run();
                cleanup = null;
            }
        }
    }

    /**
     * A mutable reference that persists across renders.
     *
     * @param <T> the value type
     */
    public static class Ref<T> {
        private T current;

        public Ref(T initial) {
            this.current = initial;
        }

        public T get() {
            return current;
        }

        public void set(T value) {
            this.current = value;
        }
    }

    /**
     * Arguments passed to a reducer function.
     *
     * @param state  the current state
     * @param action the action to process
     * @param <S>    the state type
     * @param <A>    the action type
     */
    public record ReducerArgs<S, A>(S state, A action) {}

    /**
     * Result of useReducer hook.
     *
     * @param state    the current state signal
     * @param dispatch the dispatch function
     * @param <S>      the state type
     * @param <A>      the action type
     */
    public record ReducerResult<S, A>(Signal<S> state, Consumer<A> dispatch) {}
}
