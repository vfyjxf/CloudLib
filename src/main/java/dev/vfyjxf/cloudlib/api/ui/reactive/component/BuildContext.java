package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.ElementContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A handle to the location of a component in the element tree.
 * <p>
 * BuildContext is similar to Flutter's BuildContext. It provides access to
 * information about the component's location in the tree and allows you to
 * find ancestor components.
 * <p>
 * The BuildContext is passed to {@link StatelessComponent#build} and
 * {@link ComponentState#build} methods.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * @Override
 * protected Blueprint build(BuildContext context) {
 *     // Find ancestor data
 *     ThemeData theme = context.findAncestorState(ThemeProvider.class);
 *     
 *     // Access element information
 *     boolean mounted = context.isMounted();
 *     
 *     return Container(style -> style.background(theme.primaryColor()));
 * }
 * }</pre>
 *
 * @see StatelessComponent
 * @see StatefulComponent
 * @see ComponentState
 */
@ApiStatus.Experimental
public final class BuildContext {

    private final UIElement<?> element;

    /**
     * Creates a new build context for the given element.
     *
     * @param element the element this context represents
     */
    public BuildContext(UIElement<?> element) {
        this.element = element;
    }

    /**
     * Gets the element associated with this context.
     *
     * @return the element
     */
    public UIElement<?> getElement() {
        return element;
    }

    /**
     * Gets the element context for lower-level access.
     *
     * @return the element context
     */
    public ElementContext getElementContext() {
        return element.getContext();
    }

    /**
     * Checks if the associated element is still mounted.
     *
     * @return true if mounted
     */
    public boolean isMounted() {
        return element.isMounted();
    }

    /**
     * Finds the nearest ancestor element of a specific type.
     *
     * @param elementType the element type to find
     * @param <T>         the type parameter
     * @return the ancestor element, or null if not found
     */
    @Nullable
    public <T extends UIElement<?>> T findAncestorElement(Class<T> elementType) {
        return element.getContext().findAncestorElement(elementType);
    }

    /**
     * Finds the nearest ancestor blueprint of a specific type.
     *
     * @param blueprintType the blueprint type to find
     * @param <T>           the type parameter
     * @return the ancestor blueprint, or null if not found
     */
    @Nullable
    public <T extends Blueprint> T findAncestorBlueprint(Class<T> blueprintType) {
        return element.getContext().findAncestorBlueprint(blueprintType);
    }

    /**
     * Finds the nearest ancestor StatefulComponent's state of a specific type.
     *
     * @param componentType the component class to find
     * @param <S>           the state type
     * @param <C>           the component type
     * @return the state, or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <S extends ComponentState<C>, C extends StatefulComponent<S>> S findAncestorState(Class<C> componentType) {
        UIElement<?> current = element.getParent();
        while (current != null) {
            if (current instanceof StatefulComponentElement<?, ?> statefulElement) {
                if (componentType.isInstance(statefulElement.getBlueprint())) {
                    return (S) statefulElement.getState();
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Finds the nearest ancestor StatefulComponentElement and retrieves its state.
     *
     * @param stateType the state class to find
     * @param <S>       the state type
     * @return the state, or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <S extends ComponentState<?>> S findAncestorStateOfType(Class<S> stateType) {
        UIElement<?> current = element.getParent();
        while (current != null) {
            if (current instanceof StatefulComponentElement<?, ?> statefulElement) {
                ComponentState<?> state = statefulElement.getState();
                if (stateType.isInstance(state)) {
                    return (S) state;
                }
            }
            current = current.getParent();
        }
        return null;
    }
}
