package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;

/**
 * Callback that controls how a widget's layout is resolved.
 * <p>
 * By default, every widget's position and size are computed by the taffy
 * layout engine and applied automatically. Setting a {@code LayoutHandler}
 * gives the widget full control over its own layout — the handler writes
 * the desired bounds into a {@link LayoutScope}, and the framework applies
 * whatever the scope contains.
 *
 * <h3>The taffy result as optional context</h3>
 * <p>
 * Taffy still computes a layout for every node (so parent containers can
 * reserve the correct space). The computed result is available via
 * {@link LayoutScope#taffyLayout()} for handlers that want to read or
 * tweak it. But most custom-layout widgets simply ignore it and set their
 * own bounds directly — the taffy result is just <em>one possible input</em>,
 * not a default to "modify".
 *
 * @see LayoutScope
 * @see Widget#onLayout(LayoutHandler)
 */
@FunctionalInterface
public interface LayoutHandler {

    //region core

    /**
     * Resolves this widget's layout by writing position and/or size into the scope.
     * <p>
     * If the handler does not call any mutation method on {@code scope}, the scope
     * remains <em>unresolved</em> and the framework falls back to the taffy result.
     *
     * @param widget the widget being laid out
     * @param scope  the mutable layout scope — write bounds here
     */
    void layout(Widget widget, LayoutScope scope);

    //endregion

    //region composition

    /**
     * Creates a handler that runs {@code this} first, then {@code after}.
     *
     * @param after the handler to run after this one
     * @return a composed handler
     */
    default LayoutHandler then(LayoutHandler after) {
        return (widget, scope) -> {
            this.layout(widget, scope);
            after.layout(widget, scope);
        };
    }

    //endregion

    //region built-in handlers

    /**
     * A handler that applies the taffy result and then offsets the position.
     *
     * @param dx horizontal offset
     * @param dy vertical offset
     * @return a new handler
     */
    static LayoutHandler offset(float dx, float dy) {
        return (widget, scope) -> scope.useTaffy().offset(dx, dy);
    }

    /**
     * A handler that sets fixed bounds, ignoring taffy entirely.
     *
     * @param x      the x position
     * @param y      the y position
     * @param width  the width
     * @param height the height
     * @return a new handler
     */
    static LayoutHandler fixed(float x, float y, float width, float height) {
        return (widget, scope) -> scope.setBounds(x, y, width, height);
    }

    /**
     * A handler that sets a fixed position. Size falls back to taffy.
     *
     * @param x the x position
     * @param y the y position
     * @return a new handler
     */
    static LayoutHandler fixedPos(float x, float y) {
        return (widget, scope) -> scope.useTaffy().setPosition(x, y);
    }

    /**
     * A handler that sets a fixed size. Position falls back to taffy.
     *
     * @param width  the width
     * @param height the height
     * @return a new handler
     */
    static LayoutHandler fixedSize(float width, float height) {
        return (widget, scope) -> scope.useTaffy().setSize(width, height);
    }

    //endregion
}
