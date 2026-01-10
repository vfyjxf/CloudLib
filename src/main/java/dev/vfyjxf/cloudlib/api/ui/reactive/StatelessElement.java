package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Element for stateless blueprints that do NOT track reactive dependencies.
 * <p>
 * StatelessElement differs from {@link StatefulElement} in that it does NOT
 * set up a {@link dev.vfyjxf.cloudlib.api.ui.reactive.state.Tracker} during build.
 * This means:
 * <ul>
 *   <li>Reading from a Signal during build will NOT cause automatic rebuilds</li>
 *   <li>The component only rebuilds when its parent explicitly updates it</li>
 *   <li>Better performance for pure presentational components</li>
 * </ul>
 * <p>
 * Use this for components that:
 * <ul>
 *   <li>Don't depend on any reactive state</li>
 *   <li>Are pure functions of their props (configuration)</li>
 *   <li>Need to read signals without subscribing to changes</li>
 * </ul>
 *
 * @param <B> the type of StatelessBlueprint
 * @see StatelessBlueprint
 * @see StatefulElement for components that track state
 */
@ApiStatus.Experimental
public class StatelessElement<B extends StatelessBlueprint> extends AbstractElement<B> {

    @Nullable
    private UIElement<?> child;

    public StatelessElement(B blueprint) {
        super(blueprint);
    }

    /**
     * Performs the build WITHOUT tracking reactive state.
     * <p>
     * Unlike {@link AbstractElement#performRebuild()}, this method does not
     * set up a Tracker, so Signal reads during build are not subscribed to.
     */
    @Override
    public void performRebuild() {
        if (!isMounted()) {
            return;
        }

        // Clear old subscriptions (there shouldn't be any, but be safe)
        clearSubscriptions();

        // Execute build WITHOUT tracker - no reactive tracking!
        build();

        // Mark as clean
        dirty = false;
        hasBuilt = true;
        if (lifecycle == ElementLifecycle.DIRTY) {
            lifecycle = ElementLifecycle.MOUNTED;
        }
    }

    @Override
    protected void build() {
        // Execute the build function to get the child blueprint
        // NO state tracking happens here - this is the key difference from StatefulElement
        Supplier<Blueprint> builder = blueprint.getBuilder();
        Blueprint childBlueprint = builder.get();

        if (childBlueprint == null) {
            // Builder returned null - unmount existing child
            if (child != null) {
                child.unmount();
                child = null;
            }
            return;
        }

        if (child != null) {
            // Try to update existing child using reconciliation
            child = updateChild(child, childBlueprint);
        } else {
            // First build - create the child
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
     * @return the child element, or null if not yet built
     */
    @Nullable
    public UIElement<?> getChild() {
        return child;
    }
}
