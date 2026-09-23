package dev.vfyjxf.cloudlib.ui.drag;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.drag.DragConsumer;
import dev.vfyjxf.cloudlib.api.ui.drag.DragContext;
import dev.vfyjxf.cloudlib.api.ui.drag.DragProvider;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

// TODO:Refactor dragging system to blueprint based system.
@ApiStatus.Internal
public class DraggableManager {

    /**
     * When the mouse is dragged, the distance between the start position and the current position is less than this value,
     * and the drag event is not triggered.
     * <p>
     * C magic number from testing.
     */
    private static final double minDragDistance = 3 * 3;

    /**
     * main panel.
     */
    private final CompositeWidget<? extends Widget> mainGroup;
    /**
     * current drag context.
     */
    private @Nullable DragContextImpl currentContext;

    private boolean dragging = false;

    public DraggableManager(CompositeWidget<? extends Widget> mainGroup) {
        this.mainGroup = mainGroup;

        //        throw new UnsupportedOperationException("Not Implemented");

        mainGroup.onMouseClicked(((input, context) -> {
            if (!input.isLeftClick()) return EventDispatch.pass;
            this.endDrag(input);
            var dragContext = new DragContextImpl(this);
            DragProvider dragProvider = mainGroup.getPerformer(DragProvider.scenario);
            if (dragProvider.draggable(mainGroup.scene(), input, dragContext)) {
                var draggableElement = dragProvider.getDraggableElement(mainGroup.scene(), input, dragContext);
                if (draggableElement != null) {
                    this.currentContext = dragContext;
                    dragContext.setDrag(draggableElement);
                    return EventDispatch.consumed;
                }
            }
            return EventDispatch.pass;
        }));

        mainGroup.onRender(((canvas, mouseX, mouseY, partialTicks, self, context) -> {
            DragContextImpl activeContext = currentContext;
            if (activeContext == null) return;
            var start = activeContext.getStart();
            double deltaX = mouseX - start.x();
            double deltaY = mouseY - start.y();
            var draggableElement = activeContext.draggingElement();
            if (draggableElement == null) return;
            if (!dragging) {
                var distance = deltaX * deltaX + deltaY * deltaY;
                if (distance < minDragDistance) return;
                var input = InputContext.fromMouse(mouseX, mouseY, 0);
                draggableElement.dragStart(input, activeContext);
                mainGroup.getPerformer(DragConsumer.scenario).dragStart(draggableElement, activeContext);
                dragging = true;
            }
            draggableElement.onDrag(mouseX, mouseY, activeContext);
            var dragConsumer = mainGroup.getPerformer(DragConsumer.scenario);
            dragConsumer.onDrag(draggableElement, activeContext, deltaX, deltaY);
        }));

        mainGroup.onMouseReleased(((input, context) -> {
            if (input.isLeftClick() && this.draggable()) {
                endDrag(input);
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        }));
    }

    public void endDrag(InputContext input) {
        DragContextImpl activeContext = currentContext;
        if (dragging && activeContext != null) {
            var draggableElement = activeContext.draggingElement();
            if (draggableElement != null) {
                DragConsumer dragConsumer = mainGroup.getPerformer(DragConsumer.scenario);
                FloatPos start = activeContext.getStart();
                double deltaX = input.mouseX() - start.x();
                double deltaY = input.mouseY() - start.y();
                boolean consumed = dragConsumer.consume(draggableElement, activeContext);
                draggableElement.dragEnd(input, activeContext, deltaX, deltaY, consumed);
                dragConsumer.dragEnd(draggableElement, activeContext, deltaX, deltaY);
            }
        }
        dragging = false;
        currentContext = null;
    }

    public void renderDragging(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        DragContextImpl activeContext = currentContext;
        if (!dragging || activeContext == null) return;
        var draggableElement = activeContext.draggingElement();
        if (draggableElement == null) return;
        var start = activeContext.getStart();
        var origin = draggableElement.originalBounds();
        double xPadding = mouseX - start.x() + origin.x();
        double yPadding = mouseY - start.y() + origin.y();
        graphics.pose().pushPose();
        {
            graphics.pose().translate(xPadding, yPadding, 600);
            draggableElement.render(graphics, mouseX, mouseY, partialTicks);
        }
        graphics.pose().popPose();
    }

    public @Nullable DragContext currentContext() {
        return currentContext;
    }

    public boolean draggable() {
        return currentContext != null && currentContext.draggingElement() != null;
    }
}
