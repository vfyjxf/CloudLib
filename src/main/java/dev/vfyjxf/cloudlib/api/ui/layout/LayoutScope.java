package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.taffy.tree.Layout;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable scope for layout decisions.
 * <p>
 * A {@code LayoutScope} carries the position and size that will be applied to a
 * widget's viewport. A {@link LayoutHandler} receives this scope and writes the
 * desired bounds — the framework then applies whatever the scope contains.
 *
 * <h3>Taffy integration</h3>
 * <p>
 * When the taffy engine has computed a layout, it is accessible via
 * {@link #taffyLayout()} — but this is purely informational. The handler is
 * free to ignore it and set entirely independent values. Taffy's result is
 * just <em>one possible input</em>, not the default.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Created empty, with the taffy layout stored as optional context.</li>
 *   <li>Passed to the widget's {@link LayoutHandler#layout} callback.</li>
 *   <li>The handler writes position / size via the mutation methods.</li>
 *   <li>If {@link #isResolved()}, the framework applies the values to the
 *       widget's viewport. Otherwise the widget manages layout itself.</li>
 * </ol>
 *
 * @see LayoutHandler
 */
public final class LayoutScope {

    //region state

    private final @Nullable Layout taffyLayout;

    private float x;
    private float y;
    private float width;
    private float height;

    /**
     * Whether the handler has written meaningful layout values.
     * Only resolved scopes are applied to the widget's viewport.
     */
    private boolean resolved;

    //endregion

    //region factory

    private LayoutScope(@Nullable Layout taffyLayout) {
        this.taffyLayout = taffyLayout;
    }

    /**
     * Creates a scope with a taffy layout available as optional context.
     * <p>
     * The scope starts <b>unresolved</b> — the handler must explicitly set
     * bounds or call {@link #useTaffy()} to populate values.
     *
     * @param taffyLayout the raw taffy layout (may be {@code null})
     * @return a new scope
     */
    public static LayoutScope create(@Nullable Layout taffyLayout) {
        return new LayoutScope(taffyLayout);
    }

    //endregion

    //region taffy

    /**
     * @return the raw taffy layout, or {@code null} if not available
     */
    public @Nullable Layout taffyLayout() {
        return taffyLayout;
    }

    /**
     * Populates this scope with the taffy-computed values.
     * <p>
     * This is the explicit opt-in for "use what taffy calculated".
     * After this call the scope is {@link #isResolved() resolved}.
     *
     * @return this scope for chaining
     * @throws IllegalStateException if no taffy layout is available
     */
    @Contract("-> this")
    public LayoutScope useTaffy() {
        if (taffyLayout == null) {
            throw new IllegalStateException("No taffy layout available");
        }
        this.x = taffyLayout.location().x;
        this.y = taffyLayout.location().y;
        this.width = taffyLayout.size().width;
        this.height = taffyLayout.size().height;
        this.resolved = true;
        return this;
    }

    //endregion

    //region getters

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    /**
     * @return {@code true} if the handler has written layout values that
     * should be applied to the widget
     */
    public boolean isResolved() {
        return resolved;
    }

    //endregion

    //region mutation

    /**
     * Sets position and marks the scope as resolved.
     *
     * @param x the x position relative to parent
     * @param y the y position relative to parent
     * @return this scope for chaining
     */
    @Contract("_, _ -> this")
    public LayoutScope setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.resolved = true;
        return this;
    }

    /**
     * Sets size and marks the scope as resolved.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     * @return this scope for chaining
     */
    @Contract("_, _ -> this")
    public LayoutScope setSize(float width, float height) {
        this.width = width;
        this.height = height;
        this.resolved = true;
        return this;
    }

    /**
     * Sets position and size in one call.
     *
     * @param x      the x position relative to parent
     * @param y      the y position relative to parent
     * @param width  the width in pixels
     * @param height the height in pixels
     * @return this scope for chaining
     */
    @Contract("_, _, _, _ -> this")
    public LayoutScope setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.resolved = true;
        return this;
    }

    /**
     * Adds a delta to the current position.
     * <p>
     * Typically used after {@link #useTaffy()} to apply a small adjustment.
     *
     * @param dx horizontal offset
     * @param dy vertical offset
     * @return this scope for chaining
     */
    @Contract("_, _ -> this")
    public LayoutScope offset(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    //endregion

    //region conversion

    /**
     * @return the current position as an integer {@link Pos}
     */
    public Pos pos() {
        return new Pos((int) x, (int) y);
    }

    /**
     * @return the current size as an integer {@link Size}
     */
    public Size size() {
        return new Size((int) width, (int) height);
    }

    //endregion
}
