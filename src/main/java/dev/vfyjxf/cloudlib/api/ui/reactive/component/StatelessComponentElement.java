package dev.vfyjxf.cloudlib.api.ui.reactive.component;

import dev.vfyjxf.cloudlib.api.ui.reactive.AbstractElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Element for {@link StatelessComponent} blueprints.
 * <p>
 * This element manages the lifecycle of a stateless component, calling
 * its build method when needed and managing the child element.
 */
@ApiStatus.Experimental
public class StatelessComponentElement extends AbstractElement<StatelessComponent> {

    @Nullable
    private UIElement<?> child;

    public StatelessComponentElement(StatelessComponent blueprint) {
        super(blueprint);
    }

    @Override
    protected void build() {
        // Build the component
        BuildContext buildContext = new BuildContext(this);
        Blueprint childBlueprint = blueprint.build(buildContext);

        if (childBlueprint == null) {
            if (child != null) {
                child.unmount();
                child = null;
            }
            return;
        }

        if (child != null) {
            child = updateChild(child, childBlueprint);
        } else {
            child = inflateBlueprint(childBlueprint);
        }
    }

    @Override
    protected void unmountChildren() {
        if (child != null) {
            child.unmount();
            child = null;
        }
    }

    @Override
    public void visitChildren(ElementVisitor visitor) {
        if (child != null) {
            visitor.visit(child);
        }
    }

    /**
     * Gets the child element.
     *
     * @return the child element, or null if not built
     */
    @Nullable
    public UIElement<?> getChild() {
        return child;
    }
}
