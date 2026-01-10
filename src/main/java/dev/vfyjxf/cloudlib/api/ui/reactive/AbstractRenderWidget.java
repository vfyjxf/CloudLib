package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base implementation of RenderWidget providing common functionality.
 * <p>
 * This class provides:
 * <ul>
 *   <li>Position and size management</li>
 *   <li>Dirty flag tracking for layout and paint</li>
 *   <li>Parent/child relationships</li>
 *   <li>Attachment state</li>
 * </ul>
 */
@ApiStatus.Experimental
public abstract class AbstractRenderWidget implements RenderWidget {

    @Nullable
    protected RenderWidget parent;
    protected final UIElement<?> owner;

    protected double x = 0;
    protected double y = 0;
    protected double width = 0;
    protected double height = 0;

    protected boolean needsLayout = true;
    protected boolean needsPaint = true;
    protected boolean attached = false;

    protected AbstractRenderWidget(UIElement<?> owner) {
        this.owner = owner;
    }

    @Override
    @Nullable
    public RenderWidget getParent() {
        return parent;
    }

    @Override
    public void setParent(@Nullable RenderWidget parent) {
        this.parent = parent;
    }

    @Override
    public UIElement<?> getOwner() {
        return owner;
    }

    @Override
    public void markNeedsLayout() {
        needsLayout = true;
        // When layout is needed, paint is also needed
        needsPaint = true;
        // Propagate up the tree
        if (parent != null) {
            parent.markNeedsLayout();
        }
    }

    @Override
    public void markNeedsPaint() {
        needsPaint = true;
    }

    @Override
    public boolean needsLayout() {
        return needsLayout;
    }

    @Override
    public boolean needsPaint() {
        return needsPaint;
    }

    @Override
    public void layout(Constraints constraints) {
        if (needsLayout) {
            performLayout(constraints);
            needsLayout = false;
        }
    }

    /**
     * Performs the actual layout calculation.
     * Subclasses must implement this to determine their size.
     *
     * @param constraints the layout constraints
     */
    protected abstract void performLayout(Constraints constraints);

    @Override
    public void paint(RenderContext context) {
        if (needsPaint) {
            performPaint(context);
            needsPaint = false;
        }
    }

    /**
     * Performs the actual painting.
     * Subclasses must implement this to render their content.
     *
     * @param context the render context
     */
    protected abstract void performPaint(RenderContext context);

    @Override
    public boolean hitTest(double testX, double testY) {
        return testX >= x && testX < x + width && testY >= y && testY < y + height;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setPosition(double x, double y) {
        if (this.x != x || this.y != y) {
            this.x = x;
            this.y = y;
            markNeedsPaint();
        }
    }

    @Override
    public void setSize(double width, double height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            markNeedsPaint();
        }
    }

    @Override
    public void attach() {
        attached = true;
        visitChildren(RenderWidget::attach);
    }

    @Override
    public void detach() {
        visitChildren(RenderWidget::detach);
        attached = false;
    }

    @Override
    public boolean isAttached() {
        return attached;
    }

    @Override
    public void visitChildren(RenderWidgetVisitor visitor) {
        // Default: no children
    }
}
