package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Computed;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The logic and internal state for a {@link StatefulComponent}.
 * <p>
 * ComponentState is the class-based equivalent of React/Flutter's state management.
 * It contains the mutable state for a StatefulComponent and provides lifecycle methods.
 * <p>
 * State is created by the component's {@link StatefulComponent#createState()} method
 * and persists for the lifetime of the element. Unlike functional hooks, state in
 * ComponentState is defined as class fields with explicit types.
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>{@link #initState()} - Called once when the state is first created</li>
 *   <li>{@link #build(BuildContext)} - Called when the element needs to be built/rebuilt</li>
 *   <li>{@link #didUpdateComponent(StatefulComponent)} - Called when the component configuration changes</li>
 *   <li>{@link #dispose()} - Called when the element is permanently removed</li>
 * </ol>
 * <p>
 * <b>State Management:</b>
 * <p>
 * You can use reactive state (Signal/Computed) as class fields, or use the convenience
 * methods like {@link #createSignal}, {@link #createComputed}, and {@link #setState}.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * public class CounterState extends ComponentState<Counter> {
 *     // Reactive state as class fields
 *     private final Signal<Integer> count = Signal.of(0);
 *     private final Signal<String> label = Signal.of("Count");
 *     
 *     // Or use createSignal for explicit initialization
 *     private Signal<Boolean> enabled;
 *     
 *     @Override
 *     protected void initState() {
 *         enabled = createSignal(true);
 *         
 *         // Access component props
 *         int initialCount = getComponent().getInitialCount();
 *         count.set(initialCount);
 *     }
 *     
 *     @Override
 *     protected Blueprint build(BuildContext context) {
 *         return Column(() -> {
 *             Text(label.get() + ": " + count.get());
 *             Row(() -> {
 *                 Button("-", () -> count.update(n -> n - 1));
 *                 Button("+", () -> count.update(n -> n + 1));
 *             });
 *         });
 *     }
 *     
 *     @Override
 *     protected void didUpdateComponent(Counter oldComponent) {
 *         // React to prop changes
 *         if (!oldComponent.getLabel().equals(getComponent().getLabel())) {
 *             label.set(getComponent().getLabel());
 *         }
 *     }
 *     
 *     @Override
 *     protected void dispose() {
 *         // Clean up resources
 *     }
 * }
 * }</pre>
 *
 * @param <C> the component type this state belongs to
 * @see StatefulComponent
 */
@ApiStatus.Experimental
public abstract class ComponentState<C extends StatefulComponent<?>> {

    @Nullable
    private C component;

    @Nullable
    private StatefulComponentElement<C, ?> element;

    /**
     * Whether this state has been initialized.
     */
    private boolean initialized = false;

    /**
     * Gets the component that owns this state.
     * <p>
     * Can be used to access component properties (props).
     *
     * @return the component
     * @throws IllegalStateException if called before state is attached
     */
    protected C getComponent() {
        if (component == null) {
            throw new IllegalStateException("State not yet attached to a component");
        }
        return component;
    }

    /**
     * Gets the element that hosts this state.
     *
     * @return the element, or null if not mounted
     */
    @Nullable
    protected StatefulComponentElement<C, ?> getElement() {
        return element;
    }

    /**
     * Gets the build context.
     *
     * @return the build context
     * @throws IllegalStateException if not mounted
     */
    protected BuildContext getContext() {
        if (element == null) {
            throw new IllegalStateException("State not yet mounted");
        }
        return new BuildContext(element);
    }

    /**
     * Checks if this state's element is mounted.
     *
     * @return true if mounted
     */
    protected boolean isMounted() {
        return element != null && element.isMounted();
    }

    // ==================== Lifecycle Methods ====================

    /**
     * Called when this state is first created.
     * <p>
     * Override this to initialize state that depends on the component's
     * configuration (props). This is called before the first {@link #build}.
     * <p>
     * The {@link #getComponent()} method can be used here to access props.
     */
    protected void initState() {
        // Default: no initialization
    }

    /**
     * Describes the part of the user interface represented by this state's component.
     * <p>
     * This method is called whenever the component needs to be rebuilt.
     * It should return a blueprint that describes the component's current appearance.
     *
     * @param context the build context
     * @return the blueprint describing this component's UI
     */
    protected abstract Blueprint build(BuildContext context);

    /**
     * Called when the component's configuration (props) changes.
     * <p>
     * Use this to respond to changes in the component's constructor parameters.
     * The old component is provided for comparison.
     *
     * @param oldComponent the previous component
     */
    protected void didUpdateComponent(C oldComponent) {
        // Default: no reaction to updates
    }

    /**
     * Called when this state is permanently removed from the tree.
     * <p>
     * Override this to clean up resources like subscriptions, timers, etc.
     */
    protected void dispose() {
        // Default: no cleanup
    }

    // ==================== State Management ====================

    /**
     * Requests that the component rebuild.
     * <p>
     * Use this when you've made changes to non-reactive state that require a rebuild.
     * For reactive state (Signals), changes automatically trigger rebuilds.
     */
    protected void rebuild() {
        if (element != null) {
            element.markNeedsBuild();
        }
    }

    /**
     * Updates state and triggers a rebuild.
     * <p>
     * This is a convenience method similar to React's setState. It calls the
     * updater function and then requests a rebuild.
     * <p>
     * Example:
     * <pre>{@code
     * setState(() -> {
     *     this.count++;
     *     this.label = "Updated";
     * });
     * }</pre>
     *
     * @param updater the function that updates state
     */
    protected void setState(Runnable updater) {
        updater.run();
        rebuild();
    }

    /**
     * Creates a new Signal with the given initial value.
     * <p>
     * Signals automatically trigger rebuilds when their value changes.
     * This is the recommended way to define reactive state.
     *
     * @param initialValue the initial value
     * @param <T>          the value type
     * @return the new signal
     */
    protected <T> Signal<T> createSignal(T initialValue) {
        Signal<T> signal = Signal.of(initialValue);
        // Auto-subscribe to trigger rebuilds
        if (element != null) {
            signal.subscribe(v -> rebuild());
        }
        return signal;
    }

    /**
     * Creates a new Signal with lazy initialization.
     *
     * @param initializer the initializer function
     * @param <T>         the value type
     * @return the new signal
     */
    protected <T> Signal<T> createSignal(Supplier<T> initializer) {
        return createSignal(initializer.get());
    }

    /**
     * Creates a computed value derived from other reactive state.
     *
     * @param computation the computation function
     * @param <T>         the result type
     * @return the computed value
     */
    protected <T> Computed<T> createComputed(Supplier<T> computation) {
        return Computed.of(computation);
    }

    /**
     * Creates a reducer-style state management pattern.
     * <p>
     * Example:
     * <pre>{@code
     * record State(int count, String message) {}
     * enum Action { INCREMENT, DECREMENT }
     * 
     * var reducer = createReducer(
     *     new State(0, "Initial"),
     *     (state, action) -> switch (action) {
     *         case INCREMENT -> new State(state.count() + 1, "Incremented");
     *         case DECREMENT -> new State(state.count() - 1, "Decremented");
     *     }
     * );
     * 
     * // Get state: reducer.state().get()
     * // Dispatch: reducer.dispatch().accept(Action.INCREMENT)
     * }</pre>
     *
     * @param initialState the initial state
     * @param reducer      the reducer function
     * @param <S>          the state type
     * @param <A>          the action type
     * @return a record containing the state signal and dispatch function
     */
    protected <S, A> ReducerResult<S, A> createReducer(S initialState, BiFunction<S, A, S> reducer) {
        Signal<S> state = createSignal(initialState);
        Consumer<A> dispatch = action -> {
            S currentState = state.peek();
            S newState = reducer.apply(currentState, action);
            state.set(newState);
        };
        return new ReducerResult<>(state, dispatch);
    }

    /**
     * Result of createReducer.
     *
     * @param state    the state signal
     * @param dispatch the dispatch function
     * @param <S>      the state type
     * @param <A>      the action type
     */
    public record ReducerResult<S, A>(Signal<S> state, Consumer<A> dispatch) {}

    // ==================== Internal Methods ====================

    /**
     * Called by the element to attach this state.
     */
    final void attach(C component, StatefulComponentElement<C, ?> element) {
        this.component = component;
        this.element = element;
    }

    /**
     * Called by the element to update the component reference.
     */
    @SuppressWarnings("unchecked")
    final void updateComponent(StatefulComponent<?> newComponent) {
        C oldComponent = this.component;
        this.component = (C) newComponent;
        if (initialized && oldComponent != null) {
            didUpdateComponent(oldComponent);
        }
    }

    /**
     * Called by the element to initialize this state.
     */
    final void performInitState() {
        if (!initialized) {
            initState();
            initialized = true;
        }
    }

    /**
     * Called by the element when disposed.
     */
    final void performDispose() {
        dispose();
        element = null;
        component = null;
    }
}
