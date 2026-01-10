package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.state.ReactiveState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * UIElement is the mutable, long-lived object that manages a Blueprint's presence in the tree.
 * <p>
 * This follows a React-like model where:
 * <ul>
 *   <li>Blueprints are recreated every frame (high-frequency, lightweight objects)</li>
 *   <li>Elements are long-lived and perform reconciliation</li>
 *   <li>Elements compare new vs old blueprints to decide what to update</li>
 * </ul>
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>{@link #mount} - Element is first added to the tree</li>
 *   <li>{@link #performRebuild} - Called each frame to rebuild with new blueprint</li>
 *   <li>{@link #update} - Called when blueprint changes during rebuild</li>
 *   <li>{@link #unmount} - Element is removed from the tree</li>
 * </ol>
 * <p>
 * <b>React-style Reconciliation:</b>
 * <p>
 * Each frame, the parent element calls {@link #performRebuild} which:
 * <ol>
 *   <li>Re-executes the build function to get new child blueprints</li>
 *   <li>Compares new blueprints with existing child elements</li>
 *   <li>Updates elements if blueprint type matches (via {@link Blueprint#canUpdate})</li>
 *   <li>Unmounts old elements and creates new ones if types don't match</li>
 * </ol>
 *
 * @param <B> the type of Blueprint this element manages
 * @see Blueprint
 * @see BuildOwner
 */
@ApiStatus.Experimental
public interface UIElement<B extends Blueprint> {

    /**
     * Gets the current blueprint configuration.
     *
     * @return the blueprint
     */
    B getBlueprint();

    /**
     * Gets the parent element in the tree.
     *
     * @return the parent element, or null if this is the root
     */
    @Nullable
    UIElement<?> getParent();

    /**
     * Gets the build context for this element.
     *
     * @return the build context
     */
    ElementContext getContext();

    /**
     * Gets the BuildOwner that manages this element's rebuilds.
     *
     * @return the build owner, or null if not attached
     */
    @Nullable
    BuildOwner getBuildOwner();

    /**
     * Checks if this element is currently mounted in the tree.
     *
     * @return true if mounted
     */
    boolean isMounted();

    /**
     * Gets the current lifecycle state.
     *
     * @return the lifecycle state
     */
    ElementLifecycle getLifecycle();

    /**
     * Mounts this element into the tree.
     * <p>
     * Called when the element is first added to the tree. This is where
     * the element should:
     * <ul>
     *   <li>Set up parent reference and build owner</li>
     *   <li>Perform initial build</li>
     *   <li>Create child elements</li>
     * </ul>
     *
     * @param parent the parent element (null for root)
     * @param owner  the build owner that manages rebuilds
     */
    void mount(@Nullable UIElement<?> parent, @Nullable BuildOwner owner);

    /**
     * Updates this element with a new blueprint.
     * <p>
     * Called during reconciliation when the parent determines this element
     * should be reused with a new blueprint configuration.
     * <p>
     * The element should:
     * <ul>
     *   <li>Store the new blueprint</li>
     *   <li>Mark itself for rebuild if necessary</li>
     * </ul>
     *
     * @param newBlueprint the new blueprint configuration
     */
    void update(B newBlueprint);

    /**
     * Marks this element as needing a rebuild.
     * <p>
     * This schedules the element with the BuildOwner for rebuild
     * in the next build phase. Called when:
     * <ul>
     *   <li>A subscribed reactive state changes</li>
     *   <li>The blueprint is updated</li>
     * </ul>
     */
    void markNeedsBuild();

    /**
     * Performs the rebuild for this element.
     * <p>
     * This is the core of the React-like reconciliation:
     * <ol>
     *   <li>Re-execute the build function to get new blueprints</li>
     *   <li>Compare new blueprints with current child elements</li>
     *   <li>Update, create, or remove child elements as needed</li>
     * </ol>
     * <p>
     * Called by BuildOwner during the build phase.
     */
    void performRebuild();

    /**
     * @deprecated Use {@link #performRebuild()} instead.
     * This method now delegates to performRebuild.
     */
    @Deprecated
    default void rebuild() {
        performRebuild();
    }

    /**
     * Unmounts this element from the tree.
     * <p>
     * Called when the element is removed from the tree. This is where
     * the element should:
     * <ul>
     *   <li>Unsubscribe from all reactive states</li>
     *   <li>Unmount child elements</li>
     *   <li>Release resources</li>
     * </ul>
     */
    void unmount();

    /**
     * Visits all child elements.
     *
     * @param visitor the visitor function
     */
    void visitChildren(ElementVisitor visitor);

    /**
     * Gets the reactive states this element is subscribed to.
     *
     * @return set of subscribed states
     */
    Set<ReactiveState<?>> getDependencies();

    /**
     * Gets all active subscriptions.
     *
     * @return set of active subscriptions
     */
    Set<ReactiveState.Subscription> getSubscriptions();

    /**
     * Functional interface for visiting elements.
     */
    @FunctionalInterface
    interface ElementVisitor {
        void visit(UIElement<?> element);
    }

    /**
     * Lifecycle states of a UIElement.
     */
    enum ElementLifecycle {
        /**
         * Element is being created but not yet mounted.
         */
        CREATED,

        /**
         * Element is mounted and active in the tree.
         */
        MOUNTED,

        /**
         * Element needs to rebuild its children.
         */
        DIRTY,

        /**
         * Element is being unmounted.
         */
        UNMOUNTING,

        /**
         * Element has been unmounted and should not be used.
         */
        UNMOUNTED
    }
}
