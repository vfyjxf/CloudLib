package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.event.IEventContext;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Point;
import net.minecraft.client.gui.GuiGraphics;

public interface IWidgetEvent {

    IEventDefinition<OnPositionChanged> onPositionChanged = EventFactory.define(OnPositionChanged.class, listeners -> (context, position) -> {
        for (var listener : listeners) {
            listener.onPositionChanged(context, position);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnSizeChanged> onSizeChanged = EventFactory.define(OnSizeChanged.class, listeners -> (context, size) -> {
        for (var listener : listeners) {
            listener.onSizeChanged(context, size);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnAttributesAdded> onAttributesAdded = EventFactory.define(OnAttributesAdded.class, listeners -> (context, attribute) -> {
        for (var listener : listeners) {
            listener.onAttributesAdded(context, attribute);
            if (context.isInterrupted()) return;
        }
    });


    IEventDefinition<OnRender> onRender = EventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnRender> onRenderPost = EventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnUpdate> onUpdate = EventFactory.define(OnUpdate.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onUpdate(widget);
        }
    });

    IEventDefinition<OnInit> onInit = EventFactory.define(OnInit.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onInit(widget);
        }
    });

    IEventDefinition<OnTick> onTick = EventFactory.define(OnTick.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onTick();
        }
    });
    IEventDefinition<OnChildAdded> onChildAdded = EventFactory.define(OnChildAdded.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildAdded(context, widget);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnChildAddedPost> onChildAddedPost = EventFactory.define(OnChildAddedPost.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildAdded(context, widget);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnChildRemoved> onChildRemoved = EventFactory.define(OnChildRemoved.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(context, widget);
            if (context.isInterrupted()) return;
        }
    });

    IEventDefinition<OnChildRemovedPost> onChildRemovedPost = EventFactory.define(OnChildRemovedPost.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(context, widget);
            if (context.isInterrupted()) return;
        }
    });


    IEventDefinition<OnTooltip> onTooltip = EventFactory.define(OnTooltip.class, listeners -> (context, tooltip) -> {
        for (var listener : listeners) {
            listener.onTooltip(context, tooltip);
            if (context.isInterrupted()) return;
        }
    });

    interface OnPositionChanged extends IWidgetEvent {
        void onPositionChanged(IEventContext context, Point position);
    }

    interface OnSizeChanged extends IWidgetEvent {
        void onSizeChanged(IEventContext context, Dimension size);
    }

    interface OnAttributesAdded extends IWidgetEvent {
        void onAttributesAdded(IEventContext context, ITrait attribute);
    }


    interface OnRender extends IWidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, IEventContext context);
    }

    interface OnInit extends IWidgetEvent {
        void onInit(IWidget widget);
    }

    interface OnUpdate extends IWidgetEvent {
        void onUpdate(IWidget widget);
    }

    interface OnTick extends IWidgetEvent {
        void onTick();
    }

    interface OnChildAdded extends IWidgetEvent {
        void onChildAdded(IEventContext context, IWidget widget);
    }

    interface OnChildAddedPost extends IWidgetEvent {
        void onChildAdded(IEventContext context, IWidget widget);
    }

    interface OnChildRemoved extends IWidgetEvent {
        void onChildRemoved(IEventContext context, IWidget widget);
    }

    interface OnChildRemovedPost extends IWidgetEvent {
        void onChildRemoved(IEventContext context, IWidget widget);
    }

    interface OnMouseHover extends IWidgetEvent {

    }

    interface OnTooltip extends IWidgetEvent {
        void onTooltip(IEventContext context, ITooltip tooltip);
    }

}
