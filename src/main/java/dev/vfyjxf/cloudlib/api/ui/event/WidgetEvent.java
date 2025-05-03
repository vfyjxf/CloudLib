package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventContext.Common;
import dev.vfyjxf.cloudlib.api.event.EventContext.Interruptible;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.ContextMenuBuilder;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.drag.DragContext;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;
import dev.vfyjxf.cloudlib.api.ui.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;

public interface WidgetEvent {

    EventDefinition<OnPositionChanged> onPositionChanged = EventFactory.define(OnPositionChanged.class, listeners -> (position, context) -> {
        for (var listener : listeners) {
            listener.onPositionChanged(position, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnSizeChanged> onSizeChanged = EventFactory.define(OnSizeChanged.class, listeners -> (size, context) -> {
        for (var listener : listeners) {
            listener.onSizeChanged(size, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnRender> onRender = EventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, self, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, self, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnRenderPost> onRenderPost = EventFactory.define(OnRenderPost.class, listeners -> (graphics, mouseX, mouseY, partialTicks, self, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, self, context);
            if (context.interrupted()) return;
        }
    });

    /**
     * Call when mouse over the widget
     */
    EventDefinition<OnOverlayRender> onOverlayRender = EventFactory.define(OnOverlayRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.interrupted()) return;
        }
    });

    /**
     * Call after tooltip render
     */
    EventDefinition<OnOverlayRenderPost> onOverlayRenderPost = EventFactory.define(OnOverlayRenderPost.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnUpdate> onUpdate = EventFactory.define(OnUpdate.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onUpdate(widget);
        }
    });

    EventDefinition<OnInit> onInit = EventFactory.define(OnInit.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onInit(widget);
        }
    });

    EventDefinition<OnInitPost> onInitPost = EventFactory.define(OnInitPost.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onInit(widget);
        }
    });

    EventDefinition<OnTick> onTick = EventFactory.define(OnTick.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onTick();
        }
    });

    EventDefinition<OnRemove> onRemove = EventFactory.define(OnRemove.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onRemove(self);
        }
    });

    EventDefinition<OnChildAdded> onChildAdded = EventFactory.define(OnChildAdded.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildAdded(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildAddedPost> onChildAddedPost = EventFactory.define(OnChildAddedPost.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildAdded(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildRemoved> onChildRemoved = EventFactory.define(OnChildRemoved.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildRemovedPost> onChildRemovedPost = EventFactory.define(OnChildRemovedPost.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnTooltip> onTooltip = EventFactory.define(OnTooltip.class, listeners -> (tooltip, context) -> {
        for (var listener : listeners) {
            listener.onTooltip(tooltip, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnThemeUpdate> onThemeUpdate = EventFactory.define(OnThemeUpdate.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onThemeUpdate();
        }
    });

    EventDefinition<OnResize> onResize = EventFactory.define(OnResize.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onResize(self);
        }
    });

    EventDefinition<OnResizePost> onResizePost = EventFactory.define(OnResizePost.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onResizePost(self);
        }
    });

    EventDefinition<OnContextMenuBuild> onContextMenuBuild = EventFactory.define(OnContextMenuBuild.class, listeners -> (builder, widget, context) -> {
        for (var listener : listeners) {
            listener.onContextMenuBuild(builder, widget, context);
            if (context.interrupted()) return;
        }
    });

    /**
     * Post on the widget to be dragged
     * <p>
     * For widget itself to use
     */
    EventDefinition<OnWidgetDragStart> onWidgetDragStart = EventFactory.define(OnWidgetDragStart.class, listeners -> (input, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDragStart(input, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });

    /**
     * Post on the widget to be dragged
     * <p>
     * For widget itself to use
     */
    EventDefinition<OnWidgetDrag> onWidgetDrag = EventFactory.define(OnWidgetDrag.class, listeners -> (input, deltaX, deltaY, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDrag(input, deltaX, deltaY, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });

    /**
     * Post on the widget to be dragged
     * <p>
     * For widget itself to use
     */
    EventDefinition<OnWidgetDragEnd> onWidgetDragEnd = EventFactory.define(OnWidgetDragEnd.class, listeners -> (input, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDragEnd(input, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });

    /**
     * Post on MainGroup.
     * <p>
     * For {@link dev.vfyjxf.cloudlib.api.ui.drag.DragConsumer} to use
     */
    EventDefinition<OnDragStart> onDragStart = EventFactory.define(OnDragStart.class, listeners -> (toDrag, input, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDragStart(toDrag, input, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });

    /**
     * Post on MainGroup
     * <p>
     * For {@link dev.vfyjxf.cloudlib.api.ui.drag.DragConsumer} to use
     */
    EventDefinition<OnDrag> onDrag = EventFactory.define(OnDrag.class, listeners -> (dragging, input, deltaX, deltaY, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDrag(dragging, input, deltaX, deltaY, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });

    /**
     * Post on MainGroup
     * <p>
     * For {@link dev.vfyjxf.cloudlib.api.ui.drag.DragConsumer} to use
     */
    EventDefinition<OnDragEnd> onDragEnd = EventFactory.define(OnDragEnd.class, listeners -> (dragging, input, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDragEnd(dragging, input, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });

    @FunctionalInterface
    interface OnPositionChanged extends WidgetEvent {
        void onPositionChanged(Pos position, Common context);
    }

    @FunctionalInterface
    interface OnSizeChanged extends WidgetEvent {
        void onSizeChanged(Size size, Common context);
    }

    @FunctionalInterface
    interface OnRender extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Widget self, Common context);
    }

    @FunctionalInterface
    interface OnRenderPost extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Widget self, Interruptible context);
    }

    @FunctionalInterface
    interface OnOverlayRender extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Common context);
    }

    @FunctionalInterface
    interface OnOverlayRenderPost extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Interruptible context);
    }

    @FunctionalInterface
    interface OnInit extends WidgetEvent {
        void onInit(Widget widget);
    }

    @FunctionalInterface
    interface OnInitPost extends WidgetEvent {
        void onInit(Widget widget);
    }

    @FunctionalInterface
    interface OnUpdate extends WidgetEvent {
        void onUpdate(Widget widget);
    }

    @FunctionalInterface
    interface OnTick extends WidgetEvent {
        void onTick();
    }

    @FunctionalInterface
    interface OnRemove extends WidgetEvent {
        void onRemove(Widget self);
    }

    @FunctionalInterface
    interface OnChildAdded extends WidgetEvent {
        void onChildAdded(Widget widget, Common context);
    }

    @FunctionalInterface
    interface OnChildAddedPost extends WidgetEvent {
        void onChildAdded(Widget widget, Interruptible context);
    }

    @FunctionalInterface
    interface OnChildRemoved extends WidgetEvent {
        void onChildRemoved(Widget widget, Common context);
    }

    @FunctionalInterface
    interface OnChildRemovedPost extends WidgetEvent {
        void onChildRemoved(Widget widget, Interruptible context);
    }

    @FunctionalInterface
    interface OnTooltip extends WidgetEvent {
        void onTooltip(RichTooltip richTooltip, Common context);
    }

    @FunctionalInterface
    interface OnThemeUpdate extends WidgetEvent {
        void onThemeUpdate();
    }

    @FunctionalInterface
    interface OnResize extends WidgetEvent {
        void onResize(Widget self);
    }

    @FunctionalInterface
    interface OnResizePost extends WidgetEvent {
        void onResizePost(Widget self);
    }

    @FunctionalInterface
    interface OnContextMenuBuild extends WidgetEvent {
        void onContextMenuBuild(ContextMenuBuilder builder, Widget widget, Common context);
    }

    interface OnWidgetDragStart extends WidgetEvent {
        void onDragStart(InputContext input, DragContext dragContext, Common eventContext);
    }

    interface OnWidgetDrag extends WidgetEvent {
        void onDrag(InputContext input, int deltaX, int deltaY, DragContext dragContext, Interruptible eventContext);
    }

    interface OnWidgetDragEnd extends WidgetEvent {
        void onDragEnd(InputContext input, DragContext dragContext, Interruptible eventContext);
    }

    interface OnDragStart extends WidgetEvent {
        void onDragStart(Widget toDrag, InputContext input, DragContext dragContext, Common eventContext);
    }

    interface OnDrag extends WidgetEvent {
        void onDrag(Widget dragging, InputContext input, int deltaX, int deltaY, DragContext dragContext, Interruptible eventContext);
    }

    interface OnDragEnd extends WidgetEvent {
        void onDragEnd(Widget dragging, InputContext input, DragContext dragContext, Interruptible eventContext);
    }

}
