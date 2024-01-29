package dev.vfyjxf.cloudlib.ui.traits;

import dev.vfyjxf.cloudlib.api.ui.event.IWidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.math.Dimension;

public class DynamicSizeTrait extends AbstractTrait {

    private IWidgetEvent.OnChildAddedPost onChildAdded;
    private IWidgetEvent.OnChildRemovedPost onChildRemoved;

    @Override
    public void init() {
        onChildAdded = holder.registerListener(IWidgetEvent.onChildAddedPost, (context, widget) -> computeBounds());
        onChildRemoved = holder.registerListener(IWidgetEvent.onChildRemovedPost, (context, widget) -> computeBounds());
    }

    private void computeBounds() {
        int currentX = holder.getX();
        int currentY = holder.getY();
        int currentWidth = holder().getSize().width;
        int currentHeight = holder().getSize().height;
        IWidgetGroup<IWidget> holder = this.holder.cast();
        for (IWidget child : holder.children()) {
            int childX = child.getX();
            int childY = child.getY();
            Dimension childSize = child.getSize();

            if (childX < currentX) {
                currentWidth += currentX - childX;
                currentX = childX;
            }
            if (childY < currentY) {
                currentHeight += currentY - childY;
                currentY = childY;
            }
            if (childX + childSize.width > currentX + currentWidth) {
                currentWidth = childX + childSize.width - currentX;
            }
            if (childY + childSize.height > currentY + currentHeight) {
                currentHeight = childY + childSize.height - currentY;
            }
        }
        holder.setSize(new Dimension(currentWidth, currentHeight));
    }

    @Override
    public void dispose() {
        holder.events().get(IWidgetEvent.onChildAddedPost).unregister(onChildAdded);
        holder.events().get(IWidgetEvent.onChildRemovedPost).unregister(onChildRemoved);
    }

    @Override
    protected void check(IWidget holder) {
        if (!(holder instanceof IWidgetGroup<?>))
            throw new IllegalArgumentException("The holder must be an IWidgetGroup!");
    }
}
