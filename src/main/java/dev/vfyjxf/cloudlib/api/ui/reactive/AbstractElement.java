package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.state.ReactiveState;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.ReactiveState.Subscription;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Tracker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract base implementation of UIElement providing common functionality.
 * <p>
 * This class implements the React-like lifecycle:
 * <ul>
 *   <li>mount - First time element is added to tree</li>
 *   <li>performRebuild - Called each frame to reconcile blueprints</li>
 *   <li>update - Called when blueprint changes</li>
 *   <li>unmount - Element is removed from tree</li>
 * </ul>
 * <p>
 * Blueprints are high-frequency objects that are recreated every frame.
 * Elements compare new blueprints with old ones to determine what changed.
 *
 * @param <B> the type of Blueprint this element manages
 */
@ApiStatus.Experimental
public abstract class AbstractElement<B extends Blueprint> implements UIElement<B> {

    protected B blueprint;
    @Nullable
    protected UIElement<?> parent;
    @Nullable
    protected BuildOwner buildOwner;
    @Nullable
    protected ElementContext context;
    protected ElementLifecycle lifecycle = ElementLifecycle.CREATED;

    protected final Set<ReactiveState<?>> dependencies = new HashSet<>();
    protected final Set<Subscription> subscriptions = new HashSet<>();

    /**
     * Whether this element needs to rebuild in the next build phase.
     */
    protected boolean dirty = true;

    /**
     * Whether this element has completed its first build.
     */
    protected boolean hasBuilt = false;

    protected AbstractElement(B blueprint) {
        this.blueprint = blueprint;
    }

    @Override
    public B getBlueprint() {
        return blueprint;
    }

    @Override
    @Nullable
    public UIElement<?> getParent() {
        return parent;
    }

    @Override
    @Nullable
    public BuildOwner getBuildOwner() {
        return buildOwner;
    }

    @Override
    public ElementContext getContext() {
        if (context == null) {
            context = new ElementContext(this);
        }
        return context;
    }

    @Override
    public boolean isMounted() {
        return lifecycle == ElementLifecycle.MOUNTED || lifecycle == ElementLifecycle.DIRTY;
    }

    @Override
    public ElementLifecycle getLifecycle() {
        return lifecycle;
    }

    // ==================== Lifecycle Methods ====================

    @Override
    public void mount(@Nullable UIElement<?> parent, @Nullable BuildOwner owner) {
        this.parent = parent;
        this.buildOwner = owner != null ? owner : (parent != null ? parent.getBuildOwner() : null);
        this.context = new ElementContext(this);
        lifecycle = ElementLifecycle.MOUNTED;

        // First build
        firstBuild();
    }

    /**
     * Performs the initial build when element is first mounted.
     */
    protected void firstBuild() {
        performRebuild();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update(B newBlueprint) {
        B oldBlueprint = this.blueprint;
        this.blueprint = newBlueprint;
        didUpdateBlueprint(oldBlueprint);
    }

    /**
     * Called after the blueprint has been updated.
     * <p>
     * Default implementation marks the element for rebuild.
     * Subclasses can override to add custom update logic.
     *
     * @param oldBlueprint the previous blueprint
     */
    protected void didUpdateBlueprint(B oldBlueprint) {
        markNeedsBuild();
    }

    @Override
    public void markNeedsBuild() {
        if (!isMounted()) {
            return;
        }

        if (!dirty) {
            dirty = true;
            lifecycle = ElementLifecycle.DIRTY;

            // Schedule with build owner
            if (buildOwner != null) {
                buildOwner.scheduleBuildFor(this);
            }
        }
    }

    @Override
    public void performRebuild() {
        if (!isMounted()) {
            return;
        }

        // Clear old state subscriptions before rebuild
        clearSubscriptions();

        // Track state dependencies during build
        try (Tracker tracker = Tracker.start()) {
            // Execute the build
            build();

            // Subscribe to captured dependencies
            for (ReactiveState<?> state : tracker.captured()) {
                dependencies.add(state);
                Subscription sub = state.subscribe(v -> markNeedsBuild());
                subscriptions.add(sub);
            }
        }

        // Mark as clean
        dirty = false;
        hasBuilt = true;
        if (lifecycle == ElementLifecycle.DIRTY) {
            lifecycle = ElementLifecycle.MOUNTED;
        }
    }

    /**
     * The core build method that subclasses must implement.
     * <p>
     * This is called every time the element needs to rebuild.
     * The implementation should:
     * <ol>
     *   <li>Get the new child blueprints (by re-executing build functions)</li>
     *   <li>Reconcile with existing child elements</li>
     *   <li>Create/update/remove child elements as needed</li>
     * </ol>
     */
    protected abstract void build();

    @Override
    public void unmount() {
        lifecycle = ElementLifecycle.UNMOUNTING;
        clearSubscriptions();
        unmountChildren();
        lifecycle = ElementLifecycle.UNMOUNTED;
        parent = null;
        buildOwner = null;
        context = null;
    }

    /**
     * Unmounts all child elements.
     */
    protected void unmountChildren() {
        visitChildren(UIElement::unmount);
    }

    /**
     * Clears all state subscriptions.
     */
    protected void clearSubscriptions() {
        for (Subscription sub : subscriptions) {
            sub.unsubscribe();
        }
        subscriptions.clear();
        dependencies.clear();
    }

    // ==================== Reconciliation Helpers ====================

    /**
     * Inflates a new element from a blueprint and mounts it.
     *
     * @param newBlueprint the blueprint to inflate
     * @return the newly created and mounted element
     */
    protected UIElement<?> inflateBlueprint(Blueprint newBlueprint) {
        UIElement<?> newElement = newBlueprint.createElement();
        newElement.mount(this, buildOwner);
        return newElement;
    }

    /**
     * Updates an existing child element with a new blueprint, or replaces it if incompatible.
     * <p>
     * If the element can be updated (same type, canUpdate returns true), it will be updated in place.
     * Otherwise, the old element is unmounted and a new one is created.
     *
     * @param child        the child element to update
     * @param newBlueprint the new blueprint
     * @return the updated or new element
     */
    @SuppressWarnings("unchecked")
    protected UIElement<?> updateChild(@Nullable UIElement<?> child, Blueprint newBlueprint) {
        if (child != null && canUpdateElement(child, newBlueprint)) {
            // Update in place
            ((UIElement<Blueprint>) child).update(newBlueprint);
            return child;
        } else {
            // Can't update - unmount old and create new
            if (child != null) {
                child.unmount();
            }
            return inflateBlueprint(newBlueprint);
        }
    }

    /**
     * Checks if an element can be updated with a new blueprint.
     *
     * @param element      the existing element
     * @param newBlueprint the new blueprint
     * @return true if the element can be updated
     */
    protected boolean canUpdateElement(UIElement<?> element, Blueprint newBlueprint) {
        Blueprint oldBlueprint = element.getBlueprint();
        // Same key and canUpdate returns true
        Key oldKey = oldBlueprint.key();
        Key newKey = newBlueprint.key();

        if (oldKey != null || newKey != null) {
            if (!Objects.equals(oldKey, newKey)) {
                return false;
            }
        }

        return newBlueprint.canUpdate(oldBlueprint);
    }

    // ==================== Getters ====================

    @Override
    public Set<ReactiveState<?>> getDependencies() {
        return Set.copyOf(dependencies);
    }

    @Override
    public Set<Subscription> getSubscriptions() {
        return Set.copyOf(subscriptions);
    }

    /**
     * Checks if this element is dirty and needs rebuild.
     *
     * @return true if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + blueprint.getClass().getSimpleName() + "]";
    }
}
