package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import org.jetbrains.annotations.Contract;

/**
 * TODO:rewrite layout resizer
 */
public class GridResizer extends AutomaticResizer<GridResizer> {
    private int index;
    private int lastX;
    private int lastY;
    private int lastHeight;
    private ChildResizer lastResizer;

    /**
     * How many elements in a row
     */
    private int count = 2;

    /**
     * If above zero, what is the width of every cell should be,
     * instead of items per row
     */
    private int width = 0;

    /**
     * Whether this resizes changes the bounds of the area
     */
    private boolean resizes = true;

    public GridResizer(WidgetGroup<? extends Widget> parent) {
        super(parent);
        parent.flex().setPost(this);
    }

    public GridResizer resizes(boolean resizes) {
        this.resizes = resizes;
        return this;
    }

    //TODO:implement
    @Contract("_ -> this")
    public GridResizer fixed(int count) {
        this.count = count;
        return this;
    }

    @Contract("_ -> this")
    public GridResizer adaptive(int minSize) {
        return this;
    }

    @Contract("_ -> this")
    public GridResizer fixedSize(int size) {
        return this;
    }

    public GridResizer items(int items) {
        this.count = items;
        return this;
    }

    /**
     * If you use this feature, make sure to resize the elements twice, as
     * the it needs parent's width, and it's not available on the first
     * layout resizing pass
     * <p>
     * TODO: maybe fix it?
     */
    public GridResizer width(int width) {
        this.width = width;
        return this;
    }

    @Override
    public void apply(Widget widget, Flex resizer, ChildResizer childResizer) {
        //TODO:rewrite
        int width;
        int height;
        int x;
        int y;
        if (this.index != 0 && this.index % this.count == 0) {
            this.lastY += this.lastHeight + this.margin;
            this.lastHeight = 0;
            this.index = 0;
        }

        width = (this.group.getWidth() - this.padding * 2 - this.margin * (this.count - 1)) / this.count;
        height = resizer == null ? 0 : resizer.getHeight();
        x = childResizer.widget.margin().start + (width + group.padding().start) * this.index + childResizer.widget.offset().x();
        y = this.group.padding().top + this.lastY + childResizer.widget.offset().y() + childResizer.widget.margin().top;

        this.lastHeight = Math.max(this.lastHeight, height);
        this.index++;

        widget.setBound(x, y, width, height);
    }

    @Override
    public void apply(Widget widget) {
        this.index = this.lastX = this.lastY = this.lastHeight = 0;
    }

    @Override
    public int getHeight() {
        if (this.resizes) {
            int i = 0;
            int x = 0;
            int y = 0;
            int maxH = 0;
            int width = this.group.getWidth();

            if (this.width > 0) {
                for (ChildResizer child : this.getResizers()) {
                    if (x + this.width > width - this.padding * 2) {
                        y += maxH + this.margin;
                        maxH = 0;
                        x = 0;
                    }

                    int w = this.width;
                    int h = child.flex() == null ? 0 : child.flex().getHeight();

                    if (h <= 0) {
                        h = this.height;
                    }

                    if (h <= 0) {
                        h = w;
                    }

                    maxH = Math.max(maxH, h);
                    x += this.width + this.margin;
                }
            } else {
                for (ChildResizer child : this.getResizers()) {
                    if (i != 0 && i % this.count == 0) {
                        y += maxH + this.margin;
                        maxH = 0;
                        i = 0;
                    }

                    int w = (width - this.padding * 2 - this.margin * (this.count - 1)) / this.count;
                    int h = child.flex() == null ? 0 : child.flex().getHeight();

                    if (h <= 0) {
                        h = this.height;
                    }

                    if (h <= 0) {
                        h = w;
                    }

                    maxH = Math.max(maxH, h);

                    i++;
                }
            }

            return y + maxH + this.padding * 2;
        }

        return super.getHeight();
    }
}