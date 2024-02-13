package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.math.Rectangle;
import dev.vfyjxf.cloudlib.utils.Size;

public class WidgetWithBorder extends Widget {

    private int border;

    public WidgetWithBorder setBorder(int border) {
        this.border = border;
        return this;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return getInnerBounds().contains(mouseX, mouseY);
    }

    public Size getInnerSize() {
        return new Size(getWidth() - border * 2, getHeight() - border * 2);
    }

    public Rectangle getInnerBounds() {
        return new Rectangle(position.x + border, position.y + border, size.width - border * 2, size.height - border * 2);
    }

}
