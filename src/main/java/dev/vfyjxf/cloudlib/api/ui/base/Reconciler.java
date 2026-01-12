package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.UIContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private Reconciler() {}

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
        @Nullable T oldWidget,
        Blueprint<T> newBlueprint,
        @Nullable WidgetGroup<T> parent
    ) {
        //create if there is no existing widget
        if (oldWidget == null) {
            return mount(newBlueprint, parent, parent != null ? parent.getContext() : UIContext.current());
        }
        //try to update
        if (canUpdate(oldWidget.blueprint, newBlueprint)) {
            oldWidget.blueprint = newBlueprint;
            oldWidget.key = newBlueprint.key();
            newBlueprint.updateWidget(oldWidget, parent != null ? parent.getContext() : UIContext.current());
            if (oldWidget instanceof WidgetGroup<?> group && newBlueprint instanceof Blueprint.Group groupBlueprint) {
                var stateContext = oldWidget.stateContext;
                var children =
                    StateSlot.currentContext() == stateContext ?
                    groupBlueprint.children() :
                    StateSlot.withContext(stateContext, groupBlueprint::children);
                reconcileChildren(group, children);
                stateContext.runEffects();
            }
            return oldWidget;
        }

        oldWidget.unmount();
        return mount(newBlueprint, parent, parent != null ? parent.getContext() : UIContext.current());
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
    public static <T extends Widget> void reconcileChildren(WidgetGroup<T> parent, List<? extends Blueprint<T>> newBlueprints) {

        if (parent.children.isEmpty() && newBlueprints.isEmpty()) {
            return;
        }

        if (parent.children.size() == newBlueprints.size()) {
            boolean allMatch = true;
            for (int i = 0; i < parent.children.size(); i++) {
                Widget oldChild = parent.children.get(i);
                var newBlueprint = newBlueprints.get(i);
                if (!canUpdate(oldChild.blueprint, newBlueprint)) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) {
                for (int i = 0; i < parent.children.size(); i++) {
                    reconcile(parent.children.get(i), newBlueprints.get(i), parent);
                }
                return;
            }
        }

        reconcileChildrenFull(parent, parent.children, newBlueprints);
    }

    /**
     * Full reconciliation algorithm with key matching support.
     */
    private static <T extends Widget> void reconcileChildrenFull(WidgetGroup<T> parent, List<? extends T> oldChildren, List<? extends Blueprint<T>> newBlueprints) {
        List<T> newChildren = new ArrayList<>(newBlueprints.size());

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

        for (Blueprint<T> newBlueprint : newBlueprints) {
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

            T newWidget = reconcile(oldWidget, newBlueprint, parent);
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
    public static <T extends Widget> T mount(Blueprint<T> blueprint, @Nullable WidgetGroup<T> parent, UIContext context) {
        T widget = blueprint.createWidget(context);

        widget.blueprint = blueprint;
        widget.key = blueprint.key();

        if (widget instanceof WidgetGroup<?> group && blueprint instanceof Blueprint.Group groupBlueprint) {
            widget.stateContext.setOnDirty(() -> {
                widget.stateContext.clearDirty();
                widget.onStateChanged();
                var stateContext = widget.stateContext;
                var children =
                    StateSlot.currentContext() == stateContext ?
                    groupBlueprint.children() :
                    StateSlot.withContext(stateContext, groupBlueprint::children);
                Reconciler.reconcileChildren(group, children);
                stateContext.runEffects();
            });
        }else {
            widget.stateContext.setOnDirty(() -> {
                widget.stateContext.clearDirty();
                widget.onStateChanged();
                widget.stateContext.runEffects();
            });
        }
        if (parent != null) {
            parent.addWidget(widget);
        }

        blueprint.updateWidget(widget, context);
        widget.mount();
        return widget;
    }


}
