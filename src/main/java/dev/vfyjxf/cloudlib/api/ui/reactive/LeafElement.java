package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;

/**
 * Element for leaf blueprints that have no children.
 * <p>
 * LeafElement is used for blueprints that represent atomic UI components
 * without any child blueprints (e.g., Text, Image, etc.).
 * <p>
 * Since leaf elements have no children, their build phase is minimal.
 *
 * @param <B> the type of Blueprint this element manages
 */
@ApiStatus.Experimental
public class LeafElement<B extends Blueprint> extends AbstractElement<B> {

    public LeafElement(B blueprint) {
        super(blueprint);
    }

    @Override
    protected void build() {
        // Leaf elements have no children to reconcile
        // The blueprint contains all the configuration needed
    }

    @Override
    public void visitChildren(ElementVisitor visitor) {
        // No children to visit
    }
}
