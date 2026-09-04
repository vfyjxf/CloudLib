package dev.vfyjxf.cloudlib.api.ui.base;

import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Reconciler implements the core diffing algorithm for updating the Widget tree.
 * <p>
 * Similar to SwiftUI's diff engine, it compares old and new Blueprint trees
 * and reuses existing Widgets when possible, only creating new ones when necessary.
 * <p>
 * The algorithm:
 * <ol>
 *   <li>If Blueprint types match and keys match (or both null), update in place</li>
 *   <li>If keys are provided, use them to match children across reorders</li>
 *   <li>Otherwise, match by position</li>
 *   <li>Unmount removed widgets, mount new ones</li>
 * </ol>
 * <p>
 * Performance characteristics:
 * <ul>
 *   <li>Fast path for unchanged structure: O(n) with no allocations</li>
 *   <li>Full reconciliation with keys: O(n) with HashMap allocation</li>
 *   <li>Optimized for the common case of in-place updates</li>
 * </ul>
 */
final class Reconciler {

    private Reconciler() {
    }

    /**
     * Reconciles a widget with a new blueprint.
     * Returns the same widget if it can be updated, or a new one if replacement is needed.
     *
     * @param oldWidget    the existing widget (may be null)
     * @param newBlueprint the new blueprint
     * @param parent       the parent widget
     * @return the reconciled widget
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Widget> T reconcile(
            Scene scene,
            SceneContext context,
            @Nullable T oldWidget,
            Blueprint<T> newBlueprint,
            @Nullable CompositeWidget<T> parent
    ) {
        //create if there is no existing widget
        if (oldWidget == null) {
            return mount(newBlueprint, parent, scene, context);
        }
        //try to update
        if (canUpdate(oldWidget.blueprint, newBlueprint)) {
            oldWidget.blueprint = newBlueprint;
            oldWidget.key = newBlueprint.key();
            newBlueprint.updateWidget(oldWidget, scene, context);
            if (oldWidget instanceof CompositeWidget<?> group && newBlueprint instanceof Blueprint.Group groupBlueprint) {
                var stateContext = oldWidget.stateContext;
                var children =
                        StateSlot.currentContext() == stateContext ?
                                groupBlueprint.children() :
                                StateSlot.withContext(stateContext, groupBlueprint::children);
                reconcileChildren(group, children, scene, context);
                stateContext.runEffects();
            }
            return oldWidget;
        }

        oldWidget.unmount();
        return mount(newBlueprint, parent, scene, context);
    }

    /**
     * Checks if an existing widget can be updated with a new blueprint.
     */
    public static boolean canUpdate(@Nullable Blueprint<?> oldBlueprint, @Nullable Blueprint<?> newBlueprint) {
        if (oldBlueprint == null || newBlueprint == null || oldBlueprint.getClass() != newBlueprint.getClass()) {
            return false;
        }
        return Objects.equals(oldBlueprint.key(), newBlueprint.key());
    }

    /**
     * Reconciles the children of a widget with new child blueprints.
     */
    public static <T extends Widget> void reconcileChildren(CompositeWidget<T> parent, List<? extends Blueprint<T>> childrenBlueprints, Scene scene, SceneContext context) {

        if (parent.children.isEmpty() && childrenBlueprints.isEmpty()) {
            return;
        }

        if (parent.children.size() == childrenBlueprints.size()) {
            boolean allMatch = true;
            for (int i = 0; i < parent.children.size(); i++) {
                Widget oldChild = parent.children.get(i);
                var newBlueprint = childrenBlueprints.get(i);
                if (!canUpdate(oldChild.blueprint, newBlueprint)) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) {
                for (int i = 0; i < parent.children.size(); i++) {
                    reconcile(scene, context, parent.children.get(i), childrenBlueprints.get(i), parent);
                }
                return;
            }
        }

        reconcileChildrenFull(parent, parent.children, childrenBlueprints, scene, context);
    }

    /**
     * Full reconciliation algorithm with key matching support.
     */
    private static <T extends Widget> void reconcileChildrenFull(
            CompositeWidget<T> parent,
            List<? extends T> oldChildren,
            List<? extends Blueprint<T>> childrenBlueprints,
            Scene scene,
            SceneContext context
    ) {
        List<T> newChildren = new ArrayList<>(childrenBlueprints.size());

        Map<Object, T> keyedOldChildren = null;
        List<T> unkeyedOldChildren = null;
        boolean hasKeys = false;

        for (T child : oldChildren) {
            var blueprint = child.blueprint;
            Object key = blueprint == null ? null : blueprint.key();
            if (key != null) {
                hasKeys = true;
                if (keyedOldChildren == null) {
                    keyedOldChildren = new HashMap<>();
                }
                keyedOldChildren.put(key, child);
            } else {
                if (unkeyedOldChildren == null) {
                    unkeyedOldChildren = new ArrayList<>();
                }
                unkeyedOldChildren.add(child);
            }
        }

        if (!hasKeys) {
            unkeyedOldChildren = new ArrayList<>(oldChildren);
        }

        int unkeyedIndex = 0;
        List<T> reusedWidgets = new ArrayList<>();

        for (Blueprint<T> newBlueprint : childrenBlueprints) {
            Object key = newBlueprint.key();
            T oldWidget = null;

            if (key != null && keyedOldChildren != null) {
                oldWidget = keyedOldChildren.remove(key);
            } else if (unkeyedOldChildren != null) {
                while (unkeyedIndex < unkeyedOldChildren.size()) {
                    T candidate = unkeyedOldChildren.get(unkeyedIndex);
                    unkeyedIndex++;
                    if (canUpdate(candidate.blueprint, newBlueprint)) {
                        oldWidget = candidate;
                        break;
                    }
                }
            }

            T newWidget = reconcile(scene, context, oldWidget, newBlueprint, parent);
            newChildren.add(newWidget);

            if (oldWidget != null) {
                reusedWidgets.add(oldWidget);
            }
        }

        if (keyedOldChildren != null) {
            for (T remaining : keyedOldChildren.values()) {
                remaining.unmount();
            }
        }
        if (unkeyedOldChildren != null) {
            for (int i = unkeyedIndex; i < unkeyedOldChildren.size(); i++) {
                T remaining = unkeyedOldChildren.get(i);
                if (!reusedWidgets.contains(remaining)) {
                    remaining.unmount();
                }
            }
        }
        parent.children.clear();
        parent.children.addAll(newChildren);
    }

    /**
     * Mounts a blueprint tree, creating all widgets.
     *
     * @param blueprint the root blueprint
     * @param parent    the parent widget (null for root)
     * @return the mounted widget tree
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Widget> T mount(Blueprint<T> blueprint, @Nullable CompositeWidget<T> parent, Scene scene, SceneContext context) {
        T widget = blueprint.createWidget(scene, context);

        widget.blueprint = blueprint;
        widget.key = blueprint.key();

        if (widget instanceof CompositeWidget<?> group && blueprint instanceof Blueprint.Group groupBlueprint) {
            widget.stateContext.setOnDirty(() -> {
                widget.stateContext.clearDirty();
                widget.onStateChanged();
                var stateContext = widget.stateContext;
                var children =
                        StateSlot.currentContext() == stateContext ?
                                groupBlueprint.children() :
                                StateSlot.withContext(stateContext, groupBlueprint::children);
                Reconciler.reconcileChildren(group, children, scene, context);
                stateContext.runEffects();
            });
        } else {
            widget.stateContext.setOnDirty(() -> {
                widget.stateContext.clearDirty();
                widget.onStateChanged();
                widget.stateContext.runEffects();
            });
        }
        if (parent != null) {
            parent.addWidget(widget);
        }

        blueprint.updateWidget(widget, scene, context);
        //FIXME:give correct handle
        widget.mount(scene, context, scene.handleOf(parent));
        return widget;
    }


}
