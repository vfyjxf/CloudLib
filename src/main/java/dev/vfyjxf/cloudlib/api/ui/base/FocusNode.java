package dev.vfyjxf.cloudlib.api.ui.base;

import org.jetbrains.annotations.Nullable;

/**
 * A node in the focus tree that manages focus participation for a single widget.
 * <p>
 * Each focusable widget holds a {@link FocusNode}. The focus tree is derived from
 * the widget tree — a FocusNode's parent is the nearest ancestor widget that also
 * has a FocusNode. Focus state ({@link #hasFocus()}, {@link #hasPrimaryFocus()})
 * is maintained automatically by the {@link Scene}.
 * <p>
 * To make a widget focusable, create a FocusNode and attach it via
 * {@link Widget#setFocusNode(FocusNode)}. To create a focus scope that groups
 * focus nodes and remembers the last focused child, use {@link FocusScopeNode}.
 *
 * @see FocusScopeNode
 * @see Scene#requestFocus(FocusNode)
 */
public class FocusNode {

    @Nullable Widget owner;
    boolean hasFocus;
    boolean hasPrimaryFocus;
    boolean canRequestFocus = true;

    public FocusNode() {
    }

    //region getters

    /**
     * @return the widget this focus node is attached to, or null if not yet attached.
     */
    public @Nullable Widget owner() {
        return owner;
    }

    /**
     * Returns true if this node or any descendant node currently has the primary focus.
     * <p>
     * This is useful for composite widgets (such as floating panels or dropdown menus)
     * that need to know if focus is anywhere within their subtree.
     *
     * @see #hasPrimaryFocus()
     */
    public boolean hasFocus() {
        return hasFocus;
    }

    /**
     * Returns true if this node is the current primary focus holder in the scene.
     * <p>
     * Only one node in the entire scene can hold primary focus at a time.
     * Use {@link #hasFocus()} to check whether focus is within this node's subtree.
     */
    public boolean hasPrimaryFocus() {
        return hasPrimaryFocus;
    }

    /**
     * @return true if this node is allowed to request focus.
     */
    public boolean canRequestFocus() {
        return canRequestFocus;
    }

    //endregion

    //region setters

    /**
     * Sets whether this node is allowed to request focus.
     * If set to false while this node holds primary focus, the focus will be cleared.
     */
    public void setCanRequestFocus(boolean canRequestFocus) {
        this.canRequestFocus = canRequestFocus;
        if (!canRequestFocus && hasPrimaryFocus) {
            unfocus();
        }
    }

    //endregion

    //region actions

    /**
     * Requests the focus for this node.
     * <p>
     * If this node's owner is mounted and {@link #canRequestFocus()} is true,
     * it will become the primary focus. The previous primary focus (if any) will
     * lose focus and receive appropriate events.
     */
    public void requestFocus() {
        if (owner == null || owner.scene == null) return;
        if (!canRequestFocus) return;
        if (!owner.lifecycle.mounted()) return;
        owner.scene.requestFocus(this);
    }

    /**
     * Removes focus from this node.
     * <p>
     * If this node is the primary focus, focus is cleared entirely.
     * If this node has {@link #hasFocus()} (is an ancestor of the primary focus),
     * the entire focus chain is cleared.
     */
    public void unfocus() {
        if (owner == null || owner.scene == null) return;
        owner.scene.unfocus(this);
    }

    //endregion

    //region tree traversal

    /**
     * Finds the nearest enclosing {@link FocusScopeNode} by walking up the widget tree
     * from this node's owner.
     *
     * @return the nearest ancestor FocusScopeNode, or null if none exists.
     */
    public @Nullable FocusScopeNode enclosingScope() {
        if (owner == null) return null;
        CompositeWidget<?> parent = owner.parent;
        while (parent != null) {
            if (parent.focusNode instanceof FocusScopeNode scope) {
                return scope;
            }
            parent = parent.parent;
        }
        return null;
    }

    //endregion

    @Override
    public String toString() {
        return "FocusNode{owner=" + owner + ", hasFocus=" + hasFocus + ", hasPrimaryFocus=" + hasPrimaryFocus + "}";
    }
}
