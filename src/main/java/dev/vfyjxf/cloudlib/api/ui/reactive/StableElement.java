package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Element for stable blueprints that NEVER rebuild after the first build.
 * <p>
 * StableElement is an optimization that completely skips rebuilding.
 * After the initial build:
 * <ul>
 *   <li>{@link #markNeedsBuild()} does nothing</li>
 *   <li>{@link #update(Blueprint)} does nothing (blueprint changes are ignored)</li>
 *   <li>{@link #performRebuild()} does nothing after first build</li>
 * </ul>
 * <p>
 * This is the most aggressive optimization - use only when the content
 * is truly static for the component's entire lifetime.
 * <p>
 * <b>WARNING:</b> This element ignores ALL updates. If wrapped content
 * needs to change, those changes will be silently dropped.
 *
 * @param <B> the type of StableBlueprint
 * @see StableBlueprint
 * @see StatelessElement for components that can be updated by parent
 * @see StatefulElement for components that track reactive state
 */
@ApiStatus.Experimental
public class StableElement<B extends StableBlueprint> extends AbstractElement<B> {

    @Nullable
    private UIElement<?> child;

    /**
     * Whether this element has been built at least once.
     * After the first build, all rebuild requests are ignored.
     */
    private boolean stable = false;

    public StableElement(B blueprint) {
        super(blueprint);
    }

    /**
     * Stable elements ignore markNeedsBuild after first build.
     */
    @Override
    public void markNeedsBuild() {
        if (stable) {
            // Ignore - we're stable and never rebuild
            return;
        }
        super.markNeedsBuild();
    }

    /**
     * Stable elements ignore blueprint updates after first build.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void update(B newBlueprint) {
        if (stable) {
            // Ignore - we're stable and never update
            return;
        }
        super.update(newBlueprint);
    }

    /**
     * Performs rebuild only once - subsequent calls are no-ops.
     */
    @Override
    public void performRebuild() {
        if (stable) {
            // Already built and stable - ignore all rebuilds
            return;
        }

        if (!isMounted()) {
            return;
        }

        // Build once WITHOUT tracker (stable content doesn't need reactive tracking)
        build();

        // Mark as stable - no more rebuilds ever
        stable = true;
        dirty = false;
        hasBuilt = true;
        if (lifecycle == ElementLifecycle.DIRTY) {
            lifecycle = ElementLifecycle.MOUNTED;
        }
    }

    @Override
    protected void build() {
        // Execute the build function to get the child blueprint
        // NO state tracking - stable content is built once
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

    /**
     * Checks if this element is in stable state (has been built and will never rebuild).
     *
     * @return true if stable, false if not yet built
     */
    public boolean isStable() {
        return stable;
    }
}
