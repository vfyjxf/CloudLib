package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

/**
 * TODO:rewrite layout resizer
 */
public class ColumnResizer extends AutomaticResizer {
    private int x;
    private int y;
    private int w;

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
     * Stretch column to the full width of the parent element
     */
    private boolean stretch;

    //TODO:rework scroll
    /**
     * Scroll mode, this will automatically calculate the scroll area
     */
    private boolean scroll;

    /**
     * Place elements after it reached the bottom on the left, instead of the right
     */
    private boolean flip;

    public static ColumnResizer apply(WidgetGroup<? extends Widget> widget, int margin) {
        ColumnResizer resizer = new ColumnResizer(widget, margin);

        widget.flex().setPost(resizer);

        return resizer;
    }

    protected ColumnResizer(WidgetGroup<?> parent, int margin) {
        super(parent, margin);
    }

    public ColumnResizer width(int width) {
        this.width = width;

        return this;
    }

    public ColumnResizer vertical() {
        this.vertical = true;

        return this;
    }

    public ColumnResizer stretch() {
        this.stretch = true;

        return this;
    }

    public ColumnResizer flip() {
        this.flip = true;

        return this;
    }

    @Override
    public void apply(Widget widget, Resizer resizer, ChildResizer child) {
        Margin margin = child.widget.margin();
        int w = resizer == null ? this.width : resizer.getWidth();
        int h = resizer == null ? this.height : resizer.getHeight();

        if (w == 0) {
            w = this.width;
        }

        if (h == 0) {
            h = this.height;
        }

        if (this.stretch) {
            w = this.parent.getWidth() - this.padding * 2;
        }

        int marginTop = margin.top;

        if (!this.vertical && this.y + h + marginTop > this.parent.getHeight() - this.padding * 2) {
            this.x += (this.w + this.padding) * (this.flip ? -1 : 1);
            this.y = this.w = 0;

            marginTop = 0;
        }

        int x = this.parent.posX() + this.x + this.padding + margin.left;
        int y = this.parent.posY() + this.y + this.padding + marginTop;

        widget.setBound(x, y, w, h);

        this.w = Math.max(this.w, w + margin.horizontal());
        this.y += h + this.margin + marginTop + margin.bottom;
    }

    @Override
    public void apply(Widget widget) {
        this.x = 0;
        this.y = 0;
        this.w = 0;
    }

    @Override
    public void postApply(Widget widget) {
//        if (this.scroll && this.parent.area().scroll != null) {
//            ScrollArea scroll = this.parent.area().scroll;
//
//            if (this.vertical && scroll.direction == ScrollDirection.VERTICAL) {
//                scroll.scrollSize = this.y - this.margin + this.padding * 2;
//            } else if (!this.vertical && scroll.direction == ScrollDirection.HORIZONTAL) {
//                scroll.scrollSize = this.x + this.w + this.padding * 2;
//            }
//
//            scroll.clamp();
//        }
    }

    @Override
    public int getHeight() {
        if (this.vertical && !this.scroll) {
            int y = this.padding * 2;

            for (ChildResizer child : this.getResizers()) {
                int h = child.resizer == null ? 0 : child.resizer.getHeight();

                y += (h == 0 ? this.height : h) + this.margin + child.widget.margin().vertical();
            }

            return y - this.margin;
        }

        return super.getHeight();
    }
}