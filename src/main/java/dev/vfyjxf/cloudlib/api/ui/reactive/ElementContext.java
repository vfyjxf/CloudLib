package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed through the element tree during build operations.
 * <p>
 * ElementContext provides:
 * <ul>
 *   <li>Access to inherited data from ancestors</li>
 *   <li>Methods to find ancestor elements</li>
 *   <li>Access to the root of the tree</li>
 * </ul>
 * <p>
 * This is similar to Flutter's BuildContext but adapted for our architecture.
 */
@ApiStatus.Experimental
public class ElementContext {

    private final UIElement<?> element;
    private final Map<Class<?>, Object> inheritedData = new HashMap<>();

    public ElementContext(UIElement<?> element) {
        this.element = element;
    }

    /**
     * Gets the element this context belongs to.
     *
     * @return the element
     */
    public UIElement<?> getElement() {
        return element;
    }

    /**
     * Finds the nearest ancestor element of a specific type.
     *
     * @param type the element type to find
     * @param <T>  the type parameter
     * @return the ancestor element, or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends UIElement<?>> T findAncestorElement(Class<T> type) {
        UIElement<?> current = element.getParent();
        while (current != null) {
            if (type.isInstance(current)) {
                return (T) current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Finds the nearest ancestor blueprint of a specific type.
     *
     * @param type the blueprint type to find
     * @param <T>  the type parameter
     * @return the ancestor blueprint, or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Blueprint> T findAncestorBlueprint(Class<T> type) {
        UIElement<?> current = element.getParent();
        while (current != null) {
            if (type.isInstance(current.getBlueprint())) {
                return (T) current.getBlueprint();
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Gets inherited data of a specific type.
     * <p>
     * Inherited data flows down the tree from ancestors.
     *
     * @param type the type of data to get
     * @param <T>  the type parameter
     * @return the inherited data, or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getInherited(Class<T> type) {
        // First check local inherited data
        Object local = inheritedData.get(type);
        if (local != null) {
            return (T) local;
        }
        
        // Then search ancestors
        UIElement<?> current = element.getParent();
        while (current != null) {
            ElementContext parentContext = current.getContext();
            if (parentContext != null) {
                Object data = parentContext.inheritedData.get(type);
                if (data != null) {
                    return (T) data;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Provides inherited data for descendants.
     *
     * @param type the type key
     * @param data the data to provide
     * @param <T>  the type parameter
     */
    public <T> void provide(Class<T> type, T data) {
        inheritedData.put(type, data);
    }

    /**
     * Checks if this context is still valid (element is mounted).
     *
     * @return true if valid
     */
    public boolean isValid() {
        return element.isMounted();
    }

    /**
     * Gets the root element of the tree.
     *
     * @return the root element
     */
    public UIElement<?> getRoot() {
        UIElement<?> current = element;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    /**
     * Visits all ancestors from this element to the root.
     *
     * @param visitor the visitor function
     */
    public void visitAncestors(UIElement.ElementVisitor visitor) {
        UIElement<?> current = element.getParent();
        while (current != null) {
            visitor.visit(current);
            current = current.getParent();
        }
    }
}
