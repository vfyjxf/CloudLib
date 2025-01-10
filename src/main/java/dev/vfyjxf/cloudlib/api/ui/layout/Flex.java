package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.alignment.Alignment;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

/**
 * Flex class
 * <p>
 * This class is used to define resizing behavior for a
 * {@link dev.vfyjxf.cloudlib.api.ui.widgets.Widget}.
 */
public class Flex implements Resizer {
    private final Unit x = new Unit();
    private final Unit y = new Unit();
    private final Unit width = new Unit();
    private final Unit height = new Unit();

    private final Margin margin = new Margin();
    private final Padding padding = new Padding();
    private final Offset offset = new Offset();

    /**
     * Whether the width can be expanded by layout.
     */
    private boolean widthExpandable = true;
    /**
     * Whether the height can be expanded by layout.
     */
    private boolean heightExpandable = true;
    private Alignment alignment;
    private Alignment.Horizontal horizontalAlignment;
    private Alignment.Vertical verticalAlignment;

    /**
     * Default value:{@link Widget#parent()}
     */
    private Resizer relative;
    private Resizer post;

    public Unit x() {
        return x;
    }

    public Unit y() {
        return y;
    }

    public Unit height() {
        return height;
    }

    public Unit width() {
        return width;
    }

    public boolean widthExpandable() {
        return widthExpandable;
    }

    public Flex setWidthExpandable(boolean expandable) {
        this.widthExpandable = expandable;
        return this;
    }

    public boolean heightExpandable() {
        return heightExpandable;
    }

    public Flex setHeightExpandable(boolean expandable) {
        this.heightExpandable = expandable;
        return this;
    }

    public Margin margin() {
        return margin;
    }

    public Offset offset() {
        return offset;
    }

    public Padding padding() {
        return padding;
    }

    public Resizer relative() {
        return relative;
    }

    public Resizer post() {
        return post;
    }

    public Alignment alignment() {
        return alignment;
    }

    public Alignment.Horizontal horizontalAlignment() {
        return horizontalAlignment;
    }

    public Alignment.Vertical verticalAlignment() {
        return verticalAlignment;
    }

    public Flex alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public Flex horizontalAlignment(Alignment.Horizontal horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        return this;
    }

    public Flex verticalAlignment(Alignment.Vertical verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        return this;
    }

    public void reset() {
        x.reset();
        y.reset();
        width.reset();
        height.reset();
        margin.reset();
        offset.reset();
        padding.reset();
        relative = null;
        post = null;
    }

    public void resetX() {
        x.reset();
    }

    public void resetY() {
        y.reset();
    }

    public void resetWidth() {
        width.reset();
    }

    public void resetHeight() {
        height.reset();
    }

    public void resetRelative() {
        relative = null;
    }

    public void resetPost() {
        post = null;
    }

    public void resetMargin() {
        margin.reset();
    }

    public void resetOffset() {
        offset.reset();
    }

    public void resetPadding() {
        padding.reset();
    }

    public Flex setRelative(Resizer relative) {
        this.x.setRelative(relative);
        this.y.setRelative(relative);
        this.width.setRelative(relative);
        this.height.setRelative(relative);
        this.relative = relative;
        return this;
    }

    public Flex setPost(Resizer post) {
        this.post = post;
        return this;
    }

    /* Resizer implementation */

    @Override
    public void apply(Widget widget) {
        if (this.post != null) {
            this.post.preApply(widget);
        }

        widget.setPos(this.getX(), this.getY());
        widget.setSize(this.getWidth(), this.getHeight());

        if (this.post != null) {
            this.post.apply(widget);
        }
    }

    @Override
    public void postApply(Widget widget) {
        if (this.post != null) {
            this.post.postApply(widget);
        }
    }

    @Override
    public void addResizer(WidgetGroup<?> parent, Widget child) {
        if (this.post != null) {
            this.post.addResizer(parent, child);
        }
    }

    @Override
    public void removeResizer(WidgetGroup<?> parent, Widget child) {
        if (this.post != null) {
            this.post.removeResizer(parent, child);
        }
    }

    @Override
    public int getX() {
        int value = this.x.value() == Unit.UNDEFINED ? 0 : this.x.value();

        if (this.x.relative() != null) {
            if (this.x.factor() != 0) {
                value += (int) (this.x.relative().getWidth() * this.x.factor());
            }
        }
        if (this.x.anchor() != 0) {
            value -= (int) (this.x.anchor() * this.getWidth());
        }
        return value;
    }

    @Override
    public int getY() {
        int value = this.y.value() == Unit.UNDEFINED ? 0 : this.y.value();

        if (this.y.relative() != null) {
            if (this.y.factor() != 0) {
                value += (int) (this.relative.getHeight() * this.y.factor());
            }
        }

        if (this.y.anchor() != 0) {
            value -= (int) (this.y.anchor() * this.getHeight());
        }

        return value;
    }

    @Override
    public int getWidth() {
        int value = this.post == null ? 0 : this.post.getWidth();

        if (value != 0) {
            return value;
        }

        value = this.width.value() == Unit.UNDEFINED ? 0 : this.width.value();

        if (this.relative != null && this.width.factor() != 0) {
            value += (int) (this.relative.getWidth() * this.width.factor());
        }

        return this.width.normalizeSize(value);
    }

    @Override
    public int getHeight() {

        int value = this.post == null ? 0 : this.post.getHeight();

        if (value != 0) {
            return value;
        }

        value = this.height.value() == Unit.UNDEFINED ? 0 : this.height.value();

        if (this.relative != null && this.height.factor() != 0) {
            value += (int) (this.relative.getHeight() * this.height.factor());
        }

        return this.height.normalizeSize(value);
    }
}