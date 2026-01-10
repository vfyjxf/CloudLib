package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A component that does not have mutable state.
 * <p>
 * StatelessComponent is the class-based equivalent of Flutter's StatelessWidget.
 * It describes part of the user interface which can depend on configuration
 * (constructor parameters) but does not have internal state that changes over time.
 * <p>
 * Unlike functional {@link dev.vfyjxf.cloudlib.api.ui.reactive.StatelessBlueprint},
 * a StatelessComponent is defined by subclassing and overriding the {@link #build} method.
 * This approach is beneficial when:
 * <ul>
 *   <li>You need clear component boundaries with explicit APIs</li>
 *   <li>Components have multiple constructor parameters</li>
 *   <li>You want IDE support for component hierarchy</li>
 *   <li>You prefer Flutter-style component definition</li>
 * </ul>
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * public class Greeting extends StatelessComponent {
 *     private final String name;
 *     private final int fontSize;
 *     
 *     public Greeting(String name, int fontSize) {
 *         this.name = name;
 *         this.fontSize = fontSize;
 *     }
 *     
 *     @Override
 *     protected Blueprint build(BuildContext context) {
 *         return Text("Hello, " + name + "!", style -> style.fontSize(fontSize));
 *     }
 * }
 * 
 * // Usage (can mix with functional blueprints)
 * Column(() -> {
 *     new Greeting("Alice", 16);
 *     Text("Welcome!");
 *     new UserCard(user);
 * });
 * }</pre>
 * <p>
 * StatelessComponents can be freely mixed with functional blueprints like
 * {@link dev.vfyjxf.cloudlib.api.ui.reactive.StatefulBlueprint} and
 * {@link dev.vfyjxf.cloudlib.api.ui.reactive.StatelessBlueprint}.
 *
 * @see StatefulComponent for components with mutable state
 * @see dev.vfyjxf.cloudlib.api.ui.reactive.StatelessBlueprint for functional equivalent
 */
@ApiStatus.Experimental
public abstract class StatelessComponent implements Blueprint {

    @Nullable
    private Key key;

    /**
     * Creates a stateless component.
     */
    protected StatelessComponent() {
    }

    /**
     * Creates a stateless component with a key.
     *
     * @param key the key for reconciliation
     */
    protected StatelessComponent(@Nullable Key key) {
        this.key = key;
    }

    /**
     * Describes the part of the user interface represented by this component.
     * <p>
     * This method is called when this component is built. The returned blueprint
     * describes the component's visual appearance.
     *
     * @param context the build context providing access to the element tree
     * @return the blueprint describing this component's UI
     */
    protected abstract Blueprint build(BuildContext context);

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
    public StatelessComponent key(@Nullable Key key) {
        this.key = key;
        return this;
    }

    @Override
    public UIElement<?> createElement() {
        return new StatelessComponentElement(this);
    }

    @Override
    public boolean canUpdate(Blueprint other) {
        // Only update if same class type
        return other.getClass() == this.getClass();
    }
}
