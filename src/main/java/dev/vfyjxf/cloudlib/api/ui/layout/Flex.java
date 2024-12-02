package dev.vfyjxf.cloudlib.api.ui.layout;


import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

/**
 * Flex class
 * <p>
 * This class is used to define resizing behavior for a
 * {@link dev.vfyjxf.cloudlib.api.ui.widgets.Widget}.
 */
public class Flex implements Resizer {
    public final Unit x = new Unit();
    public final Unit y = new Unit();
    public final Unit width = new Unit();
    public final Unit height = new Unit();

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

    public Resizer relative() {
        return relative;
    }

    public Resizer post() {
        return post;
    }

    public void reset() {
        x.reset();
        y.reset();
        width.reset();
        height.reset();
        relative = null;
        post = null;
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
    public void add(WidgetGroup<?> parent, Widget child) {
        if (this.post != null) {
            this.post.add(parent, child);
        }
    }

    @Override
    public void remove(WidgetGroup<?> parent, Widget child) {
        if (this.post != null) {
            this.post.remove(parent, child);
        }
    }

    @Override
    public int getX() {
        int value = this.x.value();

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
        int value = this.y.value();

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

        value = this.width.value();

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

        value = this.height.value();

        if (this.relative != null && this.height.factor() != 0) {
            value += (int) (this.relative.getHeight() * this.height.factor());
        }

        return this.height.normalizeSize(value);
    }
}