package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import net.minecraft.client.gui.GuiGraphics;

class SimpleDraggableElement implements DraggableElement<Widget> {

    protected final Widget widget;
    private final Rect originalBounds;

    SimpleDraggableElement(Widget widget) {
        this.widget = widget;
        this.originalBounds = widget.getAbsoluteBounds().copy();
    }

    @Override
    public void dragStart(InputContext input, DragContext context) {
        widget.setDragging(true);
    }

    @Override
    public void dragEnd(InputContext input, DragContext context, double deltaX, double deltaY, boolean consumed) {
        if (!consumed) {
            widget.translate((int) deltaX, (int) deltaY);
        }
        widget.setDragging(false);
    }

    @Override
    public Widget value() {
        return widget;
    }

    @Override
    public Rect originalBounds() {
        return originalBounds;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        widget.render(graphics, mouseX, mouseY, partialTicks);
    }

}
