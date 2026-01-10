package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * RenderWidget is the actual renderable object in the UI tree.
 * <p>
 * Similar to Flutter's RenderObject, RenderWidget is responsible for:
 * <ul>
 *   <li>Layout - calculating size and positioning</li>
 *   <li>Painting - rendering to the screen</li>
 *   <li>Hit testing - determining which widget was interacted with</li>
 * </ul>
 * <p>
 * Unlike Blueprint (which is immutable and frequently recreated) and UIElement
 * (which manages lifecycle), RenderWidget is a mutable object that performs
 * the actual rendering work.
 * <p>
 * The three-layer architecture:
 * <ul>
 *   <li><b>Blueprint</b> - The immutable configuration/description</li>
 *   <li><b>UIElement</b> - Manages lifecycle and state</li>
 *   <li><b>RenderWidget</b> (this interface) - The actual renderable object</li>
 * </ul>
 *
 * @see Blueprint
 * @see UIElement
 */
@ApiStatus.Experimental
public interface RenderWidget {

    /**
     * Gets the parent render widget.
     *
     * @return the parent, or null if this is the root
     */
    @Nullable
    RenderWidget getParent();

    /**
     * Sets the parent render widget.
     *
     * @param parent the parent widget
     */
    void setParent(@Nullable RenderWidget parent);

    /**
     * Gets the owning element.
     *
     * @return the element that owns this render widget
     */
    UIElement<?> getOwner();

    /**
     * Marks this widget as needing layout.
     */
    void markNeedsLayout();

    /**
     * Marks this widget as needing paint.
     */
    void markNeedsPaint();

    /**
     * Checks if this widget needs layout.
     *
     * @return true if layout is needed
     */
    boolean needsLayout();

    /**
     * Checks if this widget needs paint.
     *
     * @return true if paint is needed
     */
    boolean needsPaint();

    /**
     * Performs layout for this widget.
     * <p>
     * This method calculates the size and position of this widget
     * and its children based on the given constraints.
     *
     * @param constraints the layout constraints
     */
    void layout(Constraints constraints);

    /**
     * Paints this widget.
     * <p>
     * This method renders the widget's visual representation.
     *
     * @param canvas the rendering context
     */
    void paint(RenderContext canvas);

    /**
     * Performs hit testing at the given position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return true if this widget was hit
     */
    boolean hitTest(double x, double y);

    /**
     * Gets the x position relative to parent.
     *
     * @return the x position
     */
    double getX();

    /**
     * Gets the y position relative to parent.
     *
     * @return the y position
     */
    double getY();

    /**
     * Gets the width of this widget.
     *
     * @return the width
     */
    double getWidth();

    /**
     * Gets the height of this widget.
     *
     * @return the height
     */
    double getHeight();

    /**
     * Sets the position of this widget.
     *
     * @param x the x position
     * @param y the y position
     */
    void setPosition(double x, double y);

    /**
     * Sets the size of this widget.
     *
     * @param width  the width
     * @param height the height
     */
    void setSize(double width, double height);

    /**
     * Attaches this render widget to the tree.
     */
    void attach();

    /**
     * Detaches this render widget from the tree.
     */
    void detach();

    /**
     * Checks if this widget is attached to the tree.
     *
     * @return true if attached
     */
    boolean isAttached();

    /**
     * Visits all child render widgets.
     *
     * @param visitor the visitor function
     */
    void visitChildren(RenderWidgetVisitor visitor);

    /**
     * Functional interface for visiting render widgets.
     */
    @FunctionalInterface
    interface RenderWidgetVisitor {
        void visit(RenderWidget widget);
    }
}
