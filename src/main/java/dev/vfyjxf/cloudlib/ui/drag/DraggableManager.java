package dev.vfyjxf.cloudlib.ui.drag;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.drag.DragConsumer;
import dev.vfyjxf.cloudlib.api.ui.drag.DragContext;
import dev.vfyjxf.cloudlib.api.ui.drag.DragProvider;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class DraggableManager {

    /**
     * When the mouse is dragged, the distance between the start position and the current position is less than this value,
     * and the drag event is not triggered.
     * <p>
     * C magic number from testing.
     */
    private static final double MIN_DRAG_DISTANCE = 3 * 3;

    /**
     * main panel.
     */
    private final WidgetGroup<Widget> mainGroup;
    /**
     * current drag context.
     */
    private DragContextImpl currentContext;
    private boolean dragging = false;

    public DraggableManager(WidgetGroup<Widget> mainGroup) {
        this.mainGroup = mainGroup;

        mainGroup.onMouseClicked(((input, context) -> {
            if (!input.isLeftClick()) return false;
            this.endDrag(input);
            var dragContext = new DragContextImpl(this);
            DragProvider dragProvider = mainGroup.getPerformer(DragProvider.SCENARIO);
            if (dragProvider.draggable(mainGroup.getContext(), input, dragContext)) {
                var draggableElement = dragProvider.getDraggableElement(mainGroup.getContext(), input, dragContext);
                if (draggableElement != null) {
                    this.currentContext = dragContext;
                    dragContext.setDrag(draggableElement);
                    return true;
                }
            }
            return false;
        }));

        mainGroup.onRender(((graphics, mouseX, mouseY, partialTicks, context) -> {
            if (currentContext == null) return;
            var start = currentContext.getStart();
            double deltaX = mouseX - start.x();
            double deltaY = mouseY - start.y();
            var draggableElement = currentContext.draggingElement();
            assert draggableElement != null;
            if (!dragging) {
                var distance = deltaX * deltaX + deltaY * deltaY;
                if (distance < MIN_DRAG_DISTANCE) return;
                var input = InputContext.fromMouse(mouseX, mouseY, 0);
                draggableElement.dragStart(input, currentContext);
                mainGroup.getPerformer(DragConsumer.SCENARIO).dragStart(draggableElement, currentContext);
                dragging = true;
            }
            draggableElement.onDrag(mouseX, mouseY, currentContext);
            var dragConsumer = mainGroup.getPerformer(DragConsumer.SCENARIO);
            dragConsumer.onDrag(draggableElement, currentContext, deltaX, deltaY);
        }));

        mainGroup.onMouseReleased(((input, context) -> {
            if (input.isLeftClick() && this.draggable()) {
                endDrag(input);
            }
            return false;
        }));
    }

    public void endDrag(InputContext input) {
        if (dragging && currentContext != null) {
            var draggableElement = currentContext.draggingElement();
            if (draggableElement != null) {
                DragConsumer dragConsumer = mainGroup.getPerformer(DragConsumer.SCENARIO);
                FloatPos start = currentContext.getStart();
                double deltaX = input.mouseX() - start.x();
                double deltaY = input.mouseY() - start.y();
                boolean consumed = dragConsumer.consume(draggableElement, currentContext);
                draggableElement.dragEnd(input, currentContext, deltaX, deltaY, consumed);
                dragConsumer.dragEnd(draggableElement, currentContext, deltaX, deltaY);

            }
        }
        dragging = false;
        currentContext = null;
    }

    public void renderDragging(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!dragging) return;
        var draggableElement = currentContext.draggingElement();
        assert draggableElement != null;
        var start = currentContext.getStart();
        var origin = draggableElement.originalBounds();
        double xPadding = mouseX - start.x() + origin.x;
        double yPadding = mouseY - start.y() + origin.y;
        graphics.pose().pushPose();
        {
            graphics.pose().translate(xPadding, yPadding, 600);
            draggableElement.render(graphics, mouseX, mouseY, partialTicks);
        }
        graphics.pose().popPose();
    }

    public DragContext currentContext() {
        return currentContext;
    }

    public boolean draggable() {
        return currentContext != null && currentContext.draggingElement() != null;
    }

}
