package dev.vfyjxf.cloudlib.api.ui.base;

import org.jetbrains.annotations.Nullable;

/**
 * A focus scope that groups focus nodes and remembers the last focused child.
 * <p>
 * When a descendant within this scope gains primary focus, this scope records it
 * as the {@link #focusedChild()}. When {@link #requestFocus()} is called on this
 * scope, it attempts to restore focus to the previously focused child rather than
 * claiming primary focus itself. This enables focus memory for container widgets
 * (e.g., panels, tabs, floating windows).
 *
 * @see FocusNode
 * @see Scene#requestFocus(FocusNode)
 */
public class FocusScopeNode extends FocusNode {

    @Nullable FocusNode focusedChild;
    boolean autofocus;

    public FocusScopeNode() {
    }

    //region getters

    /**
     * Returns the most recently primary-focused descendant within this scope.
     * <p>
     * This value is updated by the {@link Scene} whenever a descendant node
     * gains primary focus.
     *
     * @return the last focused child node, or null if no child has been focused.
     */
    public @Nullable FocusNode focusedChild() {
        return focusedChild;
    }

    /**
     * @return true if a child in this scope should be automatically focused
     * when the scope first receives focus.
     */
    public boolean autofocus() {
        return autofocus;
    }

    //endregion

    //region setters

    public void setAutofocus(boolean autofocus) {
        this.autofocus = autofocus;
    }

    //endregion

    //region actions

    /**
     * Requests focus for this scope.
     * <p>
     * If a previously focused child exists and is still valid (mounted and
     * {@link #canRequestFocus()}), focus is restored to that child. Otherwise,
     * this scope itself becomes the primary focus.
     */
    @Override
    public void requestFocus() {
        if (focusedChild != null
            && focusedChild.owner != null
            && focusedChild.owner.lifecycle.mounted()
            && focusedChild.canRequestFocus) {
            focusedChild.requestFocus();
        } else {
            super.requestFocus();
        }
    }

    //endregion

    @Override
    public String toString() {
        return "FocusScopeNode{owner=" + owner + ", hasFocus=" + hasFocus + ", focusedChild=" + focusedChild + "}";
    }
}
