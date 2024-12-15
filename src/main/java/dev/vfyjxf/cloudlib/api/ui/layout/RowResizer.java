package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

import java.util.List;

public class RowResizer extends AutomaticResizer<RowResizer> {

    private ChildResizer lastResizer;

    /**
     * Default width for row elements if not specified by resizer
     */
    private int width;

    /**
     * Whether the elements would be placed from right to left
     */
    private boolean reverse;

    public RowResizer(WidgetGroup<? extends Widget> parent) {
        super(parent);
        parent.flex().setPost(this);
    }

    public RowResizer width(int width) {
        this.width = width;
        return this;
    }

    public RowResizer reverse() {
        this.reverse = true;
        return this;
    }

    @Override
    public void apply(Widget widget, Flex resizer, ChildResizer childResizer) {
        int size = this.getResizers().size();

        int baseX = lastResizer == null ? 0 : lastResizer.getX() + lastResizer.getWidth() + lastResizer.widget.margin().end;
        int posX = baseX + childResizer.widget.margin().start + group.padding().start;
        posX += childResizer.widget.offset().x();

        int baseY = group.padding().top;
        int posY = baseY + childResizer.widget.margin().top;
        posY += childResizer.widget.offset().y();

        int width = resizer.width().isUndefined() ?
                (group.getWidth()) / size : resizer.getWidth();
        width = resizer.width().rangeDefined() ? resizer.width().normalizeSize(width) : width;
        int height = resizer.height().isUndefined() ?
                group.getHeight() - group.padding().top : resizer.getHeight();
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

    @Override
    public int getWidth() {
        if (!this.group.flex().widthExpandable()) return 0;
        if (widthDefined()) return 0;
        int totalWidth = (int) this.getResizers()
                .select(resizer -> resizer.flex() != null)
                .summarizeInt(resizer -> resizer.flex().getWidth() + resizer.widget.margin().horizontal())
                .getSum();
        return this.group.flex().width().normalizeSize(totalWidth);
    }

    @Override
    public int getHeight() {
        if (!this.group.flex().heightExpandable()) return 0;
        List<ChildResizer> resizers = this.getResizers();
        int h = 0;

        for (ChildResizer child : resizers) {
            h = Math.max(h, child.flex() == null ? 0 : child.flex().getHeight() + child.widget.margin().vertical());
        }

        if (h == 0) {
            h = this.height;
        }

        return h + this.padding * 2;
    }
}