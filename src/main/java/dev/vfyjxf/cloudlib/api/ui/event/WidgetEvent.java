package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.Events;
import dev.vfyjxf.cloudlib.api.event.context.CommonContext;
import dev.vfyjxf.cloudlib.api.event.context.InterruptibleContext;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.drag.DragContext;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.UnknownNullability;

//TODO:move all definition to WidgetEvents
public interface WidgetEvent {


    //region lifecycle

    EventDefinition<OnInit> onInit = Events.define(OnInit.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onInit(self);
        }
    });

    EventDefinition<OnMount> onMount = Events.define(OnMount.class, listeners -> (scene, context) -> {
        for (var listener : listeners) {
            listener.onMount(scene, context);
        }
    });

    EventDefinition<OnUnmount> onUnmount = Events.define(OnUnmount.class, listeners -> (parent, self) -> {
        for (var listener : listeners) {
            listener.onUnmount(parent, self);
        }
    });

    EventDefinition<OnDestroy> onDestroy = Events.define(OnDestroy.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onDestroy(self);
        }
    });

    //endregion

    EventDefinition<OnPositionChanged> onPositionChanged = Events.define(OnPositionChanged.class, listeners -> (position, context) -> {
        for (var listener : listeners) {
            listener.onPositionChanged(position, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnSizeChanged> onSizeChanged = Events.define(OnSizeChanged.class, listeners -> (size, context) -> {
        for (var listener : listeners) {
            listener.onSizeChanged(size, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnRender> onRender = Events.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, self, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, self, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnRenderPost> onRenderPost = Events.define(OnRenderPost.class, listeners -> (graphics, mouseX, mouseY, partialTicks, self, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, self, context);
            if (context.interrupted()) return;
        }
    });

    /**
     * Call when mouse over the widget
     */
    EventDefinition<OnOverlayRender> onOverlayRender = Events.define(OnOverlayRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.interrupted()) return;
        }
    });

    /**
     * Call after tooltip render
     */
    EventDefinition<OnOverlayRenderPost> onOverlayRenderPost = Events.define(OnOverlayRenderPost.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnTick> onTick = Events.define(OnTick.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onTick();
        }
    });

    EventDefinition<OnRemove> onRemove = Events.define(OnRemove.class, listeners -> (parent, self) -> {
        for (var listener : listeners) {
            listener.onRemove(parent, self);
        }
    });

    EventDefinition<OnChildAdded> onChildAdded = Events.define(OnChildAdded.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildAdded(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildAddedPost> onChildAddedPost = Events.define(OnChildAddedPost.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildAdded(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildRemoved> onChildRemoved = Events.define(OnChildRemoved.class, listeners -> (widget, context) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(widget, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnTooltip> onTooltip = Events.define(OnTooltip.class, listeners -> (tooltip, context) -> {
        for (var listener : listeners) {
            listener.onTooltip(tooltip, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnThemeUpdate> onThemeUpdate = Events.define(OnThemeUpdate.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onThemeUpdate();
        }
    });

    EventDefinition<OnResize> onResize = Events.define(OnResize.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onResize(self);
        }
    });

    EventDefinition<OnResizePost> onResizePost = Events.define(OnResizePost.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onResizePost(self);
        }
    });

    /**
     * Post on the widget to be dragged
     * <p>
     * For widget itself to use
     */
    EventDefinition<OnWidgetDragStart> onWidgetDragStart = Events.define(OnWidgetDragStart.class, listeners -> (input, dragContext, eventContext) -> {
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
    EventDefinition<OnWidgetDrag> onWidgetDrag = Events.define(OnWidgetDrag.class, listeners -> (input, deltaX, deltaY, dragContext, eventContext) -> {
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
    EventDefinition<OnWidgetDragEnd> onWidgetDragEnd = Events.define(OnWidgetDragEnd.class, listeners -> (input, dragContext, eventContext) -> {
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
    EventDefinition<OnDragStart> onDragStart = Events.define(OnDragStart.class, listeners -> (toDrag, input, dragContext, eventContext) -> {
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
    EventDefinition<OnDrag> onDrag = Events.define(OnDrag.class, listeners -> (dragging, input, deltaX, deltaY, dragContext, eventContext) -> {
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
    EventDefinition<OnDragEnd> onDragEnd = Events.define(OnDragEnd.class, listeners -> (dragging, input, dragContext, eventContext) -> {
        for (var listener : listeners) {
            listener.onDragEnd(dragging, input, dragContext, eventContext);
            if (eventContext.interrupted()) return;
        }
    });


    //region lifecycle

    interface OnInit extends WidgetEvent {
        void onInit(Widget self);
    }

    interface OnMount extends WidgetEvent {
        void onMount(Scene scene, SceneContext context);
    }

    interface OnUnmount extends WidgetEvent {
        void onUnmount(@UnknownNullability CompositeWidget<? extends Widget> parent, Widget self);
    }

    interface OnDestroy extends WidgetEvent {
        void onDestroy(Widget self);
    }


    //endregion


    @FunctionalInterface
    interface OnPositionChanged extends WidgetEvent {
        void onPositionChanged(Pos position, CommonContext context);
    }

    @FunctionalInterface
    interface OnSizeChanged extends WidgetEvent {
        void onSizeChanged(Size size, CommonContext context);
    }

    @FunctionalInterface
    interface OnRender extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Widget self, CommonContext context);
    }

    @FunctionalInterface
    interface OnRenderPost extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Widget self, InterruptibleContext context);
    }

    @FunctionalInterface
    interface OnOverlayRender extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CommonContext context);
    }

    @FunctionalInterface
    interface OnOverlayRenderPost extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, InterruptibleContext context);
    }

    @FunctionalInterface
    interface OnTick extends WidgetEvent {
        void onTick();
    }

    @FunctionalInterface
    interface OnRemove extends WidgetEvent {
        void onRemove(CompositeWidget<? extends Widget> parent, Widget self);
    }

    @FunctionalInterface
    interface OnChildAdded extends WidgetEvent {
        void onChildAdded(Widget widget, CommonContext context);
    }

    @FunctionalInterface
    interface OnChildAddedPost extends WidgetEvent {
        void onChildAdded(Widget widget, InterruptibleContext context);
    }

    @FunctionalInterface
    interface OnChildRemoved extends WidgetEvent {
        void onChildRemoved(Widget widget, InterruptibleContext context);
    }

    @FunctionalInterface
    interface OnTooltip extends WidgetEvent {
        void onTooltip(RichTooltip richTooltip, CommonContext context);
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
    interface OnWidgetDragStart extends WidgetEvent {
        void onDragStart(InputContext input, DragContext dragContext, CommonContext eventContext);
    }

    @FunctionalInterface
    interface OnWidgetDrag extends WidgetEvent {
        void onDrag(InputContext input, int deltaX, int deltaY, DragContext dragContext, InterruptibleContext eventContext);
    }

    @FunctionalInterface
    interface OnWidgetDragEnd extends WidgetEvent {
        void onDragEnd(InputContext input, DragContext dragContext, InterruptibleContext eventContext);
    }

    @FunctionalInterface
    interface OnDragStart extends WidgetEvent {
        void onDragStart(Widget toDrag, InputContext input, DragContext dragContext, CommonContext eventContext);
    }

    @FunctionalInterface
    interface OnDrag extends WidgetEvent {
        void onDrag(Widget dragging, InputContext input, int deltaX, int deltaY, DragContext dragContext, InterruptibleContext eventContext);
    }

    @FunctionalInterface
    interface OnDragEnd extends WidgetEvent {
        void onDragEnd(Widget dragging, InputContext input, DragContext dragContext, InterruptibleContext eventContext);
    }

}
