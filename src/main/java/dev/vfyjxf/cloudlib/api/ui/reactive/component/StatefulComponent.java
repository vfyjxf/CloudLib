package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A component that has mutable state.
 * <p>
 * StatefulComponent is the class-based equivalent of Flutter's StatefulWidget.
 * It contains configuration (props) and creates a {@link ComponentState} that
 * manages the mutable state and contains the build logic.
 * <p>
 * The component itself is immutable - all mutable state lives in the State object.
 * When a parent rebuilds, a new StatefulComponent instance may be created with
 * updated props, but the State object persists and receives the new props via
 * {@link ComponentState#didUpdateComponent(StatefulComponent)}.
 * <p>
 * <b>Structure:</b>
 * <ul>
 *   <li>The Component class holds configuration (constructor parameters/props)</li>
 *   <li>The State class holds mutable state and the build logic</li>
 *   <li>State persists across parent rebuilds, component instances don't</li>
 * </ul>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * // The component (holds props, creates state)
 * public class Counter extends StatefulComponent<CounterState> {
 *     private final int initialCount;
 *     private final String label;
 *     
 *     public Counter(int initialCount, String label) {
 *         this.initialCount = initialCount;
 *         this.label = label;
 *     }
 *     
 *     public int getInitialCount() { return initialCount; }
 *     public String getLabel() { return label; }
 *     
 *     @Override
 *     protected CounterState createState() {
 *         return new CounterState();
 *     }
 * }
 * 
 * // The state (holds mutable state, contains build logic)
 * public class CounterState extends ComponentState<Counter> {
 *     private final Signal<Integer> count = Signal.of(0);
 *     
 *     @Override
 *     protected void initState() {
 *         count.set(getComponent().getInitialCount());
 *     }
 *     
 *     @Override
 *     protected Blueprint build(BuildContext context) {
 *         return Column(() -> {
 *             Text(getComponent().getLabel() + ": " + count.get());
 *             Button("+", () -> count.update(n -> n + 1));
 *         });
 *     }
 * }
 * 
 * // Usage
 * Column(() -> {
 *     new Counter(0, "Click count");
 *     new Counter(100, "Score");
 * });
 * }</pre>
 * <p>
 * <b>When to use StatefulComponent vs StatelessComponent:</b>
 * <ul>
 *   <li>Use StatelessComponent when the UI depends only on props</li>
 *   <li>Use StatefulComponent when you need internal mutable state</li>
 *   <li>Use StatefulComponent when you need lifecycle callbacks</li>
 * </ul>
 *
 * @param <S> the type of state for this component
 * @see ComponentState
 * @see StatelessComponent
 */
@ApiStatus.Experimental
public abstract class StatefulComponent<S extends ComponentState<?>> implements Blueprint {

    @Nullable
    private Key key;

    /**
     * Creates a stateful component.
     */
    protected StatefulComponent() {
    }

    /**
     * Creates a stateful component with a key.
     *
     * @param key the key for reconciliation
     */
    protected StatefulComponent(@Nullable Key key) {
        this.key = key;
    }

    /**
     * Creates the mutable state for this component.
     * <p>
     * This method is called exactly once when the element is first created.
     * The returned state object persists for the lifetime of the element.
     *
     * @return the state object for this component
     */
    protected abstract S createState();

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    /**
     * Sets the key for this component.
     *
     * @param key the key
     * @return this component for chaining
     */
    public StatefulComponent<S> key(@Nullable Key key) {
        this.key = key;
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public UIElement<?> createElement() {
        return new StatefulComponentElement(this);
    }

    @Override
    public boolean canUpdate(Blueprint other) {
        // Only update if same class type
        return other.getClass() == this.getClass();
    }
}
