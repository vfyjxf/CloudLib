package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Element for composite blueprints that contain child blueprints.
 * <p>
 * CompositeElement implements React-style reconciliation:
 * <ul>
 *   <li>Each frame, getChildren() is called on the blueprint to get new child blueprints</li>
 *   <li>New blueprints are compared with existing child elements</li>
 *   <li>Matching elements are updated, new elements are created, old ones are unmounted</li>
 * </ul>
 * <p>
 * The reconciliation uses keys for efficient matching when provided.
 *
 * @param <B> the type of Blueprint this element manages
 */
@ApiStatus.Experimental
public class CompositeElement<B extends CompositeBlueprint> extends AbstractElement<B> {

    protected final List<UIElement<?>> children = new ArrayList<>();

    public CompositeElement(B blueprint) {
        super(blueprint);
    }

    @Override
    protected void build() {
        // Get new child blueprints (this may re-execute build functions)
        List<Blueprint> newChildBlueprints = blueprint.getChildren();
        // Reconcile children
        reconcileChildren(newChildBlueprints);
    }

    /**
     * Reconciles the current child elements with a new list of child blueprints.
     * <p>
     * This implements React-style reconciliation:
     * <ol>
     *   <li>Build a map of old children by key</li>
     *   <li>For each new blueprint, find matching old element or create new</li>
     *   <li>Update or create elements as needed</li>
     *   <li>Unmount any unmatched old elements</li>
     * </ol>
     *
     * @param newChildBlueprints the new list of child blueprints
     */
    protected void reconcileChildren(List<Blueprint> newChildBlueprints) {
        // Index old children by key for O(1) lookup
        Map<Key, UIElement<?>> oldKeyedChildren = new HashMap<>();
        List<UIElement<?>> oldUnkeyedChildren = new ArrayList<>();

        for (UIElement<?> child : children) {
            Key key = child.getBlueprint().key();
            if (key != null) {
                oldKeyedChildren.put(key, child);
            } else {
                oldUnkeyedChildren.add(child);
            }
        }

        List<UIElement<?>> newChildren = new ArrayList<>();
        int unkeyedIndex = 0;

        for (Blueprint newBlueprint : newChildBlueprints) {
            Key newKey = newBlueprint.key();
            UIElement<?> oldElement = null;

            if (newKey != null) {
                // Try to find by key
                oldElement = oldKeyedChildren.remove(newKey);
            } else {
                // Try to match unkeyed element by type
                while (unkeyedIndex < oldUnkeyedChildren.size()) {
                    UIElement<?> candidate = oldUnkeyedChildren.get(unkeyedIndex);
                    unkeyedIndex++;
                    if (canUpdateElement(candidate, newBlueprint)) {
                        oldElement = candidate;
                        break;
                    } else {
                        // Can't reuse, will be unmounted
                    }
                }
            }

            // Use updateChild which handles both update and create cases
            UIElement<?> resultElement = updateChild(oldElement, newBlueprint);
            newChildren.add(resultElement);
        }

        // Unmount remaining keyed children that weren't matched
        for (UIElement<?> oldChild : oldKeyedChildren.values()) {
            oldChild.unmount();
        }

        // Unmount remaining unkeyed children
        for (int i = unkeyedIndex; i < oldUnkeyedChildren.size(); i++) {
            UIElement<?> oldChild = oldUnkeyedChildren.get(i);
            // Only unmount if not already in newChildren
            if (!newChildren.contains(oldChild)) {
                oldChild.unmount();
            }
        }

        // Update children list
        this.children.clear();
        this.children.addAll(newChildren);
    }

    @Override
    protected void unmountChildren() {
        for (UIElement<?> child : children) {
            child.unmount();
        }
        children.clear();
    }

    @Override
    public void visitChildren(ElementVisitor visitor) {
        for (UIElement<?> child : children) {
            visitor.visit(child);
        }
    }

    /**
     * Gets the list of child elements.
     *
     * @return the child elements (immutable copy)
     */
    public List<UIElement<?>> getChildren() {
        return List.copyOf(children);
    }

    /**
     * Gets the number of children.
     *
     * @return the child count
     */
    public int getChildCount() {
        return children.size();
    }
}
