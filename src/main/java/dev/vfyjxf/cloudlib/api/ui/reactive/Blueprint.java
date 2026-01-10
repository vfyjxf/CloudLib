package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Blueprint is the immutable description of a UI component.
 * <p>
 * Similar to Flutter's Widget, a Blueprint is a lightweight, immutable configuration
 * object that describes what the UI should look like. Blueprints are designed to be
 * created frequently (high-frequency execution) and compared for differences.
 * <p>
 * The three-layer architecture:
 * <ul>
 *   <li><b>Blueprint</b> (this interface) - The immutable configuration/description (Flutter: Widget)</li>
 *   <li><b>UIElement</b> - Manages the lifecycle and state (Flutter: Element)</li>
 *   <li><b>RenderWidget</b> - The actual renderable object (Flutter: RenderObject)</li>
 * </ul>
 * <p>
 * Blueprints are meant to be:
 * <ul>
 *   <li>Immutable - once created, their properties don't change</li>
 *   <li>Lightweight - cheap to create and discard</li>
 *   <li>Declarative - describe what to render, not how</li>
 *   <li>Composable - built from other blueprints</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * Blueprint ui = Column(() -> {
 *     Text("Hello");
 *     Text("World");
 *     Row(() -> {
 *         Button("Click me", () -> count.update(n -> n + 1));
 *         Text(count.map(n -> "Count: " + n));
 *     });
 * });
 * }</pre>
 *
 * @see UIElement
 * @see RenderWidget
 */
@ApiStatus.Experimental
public interface Blueprint {

    /**
     * Gets the unique key for this blueprint.
     * <p>
     * Keys are used to match blueprints across rebuilds. When a parent rebuilds,
     * children with matching keys are updated rather than recreated.
     *
     * @return the key, or null for automatic keying
     */
    @Nullable
    default Key key() {
        return null;
    }

    /**
     * Creates a new UIElement for this blueprint.
     * <p>
     * This is called when the blueprint is first mounted into the tree.
     * The element manages the blueprint's lifecycle and owns any associated state.
     *
     * @return a new UIElement instance
     */
    UIElement<?> createElement();

    /**
     * Checks if this blueprint can update an existing element.
     * <p>
     * By default, blueprints can update elements created from blueprints of the same type.
     * Override this for more complex update logic.
     *
     * @param oldBlueprint the previous blueprint
     * @return true if this blueprint can update the element
     */
    default boolean canUpdate(Blueprint oldBlueprint) {
        return this.getClass() == oldBlueprint.getClass();
    }

}
