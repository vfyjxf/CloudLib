package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

//TODO:rework scroll
public class ColumnResizer extends AutomaticResizer<ColumnResizer> {
    private ChildResizer lastResizer;

    /**
     * Default width
     */
    private int width;

    /**
     * Keeps on adding elements vertically without shifting them into
     * the next row and resize the height of the element
     */
    private boolean vertical;

    /**
     * Place elements after it reached the bottom on the left, instead of the right
     */
    private boolean flip;

    public ColumnResizer(WidgetGroup<? extends Widget> parent) {
        super(parent);
        parent.flex().setPost(this);
    }


    public ColumnResizer width(int width) {
        this.width = width;
        return this;
    }

    public ColumnResizer vertical() {
        this.vertical = true;
        return this;
    }

    public ColumnResizer flip() {
        this.flip = true;
        return this;
    }

    @Override
    public void apply(Widget widget, Flex resizer, ChildResizer childResizer) {
        int size = this.getResizers().size();

        int baseX = group.padding().start;
        int posX = baseX + childResizer.widget.margin().start;
        posX += childResizer.widget.offset().x();

        int baseY = lastResizer == null ? 0 : lastResizer.getY() + lastResizer.getHeight() + lastResizer.widget.margin().bottom;
        int posY = baseY + childResizer.widget.margin().top + group.padding().top;
        posY += childResizer.widget.offset().y();

        int width = resizer.width().isUndefined() ?
                group.getWidth() - group.padding().start : resizer.getWidth();
        width = resizer.width().rangeDefined() ? resizer.width().normalizeSize(width) : width;
        int height = resizer.height().isUndefined() ?
                (group.getHeight()) / size : resizer.getHeight();
        height = resizer.height().rangeDefined() ? resizer.height().normalizeSize(height) : height;
        widget.setBound(
                posX,
                posY,
                width,
                height
        );
        lastResizer = childResizer;
    }

    @Override
    public void apply(Widget widget) {
        this.lastResizer = null;
    }

}