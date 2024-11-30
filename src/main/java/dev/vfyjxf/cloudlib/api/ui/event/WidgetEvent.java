package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventContext.Common;
import dev.vfyjxf.cloudlib.api.event.EventContext.Interruptible;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.math.Dimension;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.RichTooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import net.minecraft.client.gui.GuiGraphics;

public interface WidgetEvent {

    EventDefinition<OnPositionChanged> onPositionChanged = EventFactory.define(OnPositionChanged.class, listeners -> (context, position) -> {
        for (var listener : listeners) {
            listener.onPositionChanged(context, position);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnSizeChanged> onSizeChanged = EventFactory.define(OnSizeChanged.class, listeners -> (context, size) -> {
        for (var listener : listeners) {
            listener.onSizeChanged(context, size);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnRender> onRender = EventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnRender> onRenderPost = EventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
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

    EventDefinition<OnTick> onTick = EventFactory.define(OnTick.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onTick();
        }
    });

    EventDefinition<OnDelete> onDelete = EventFactory.define(OnDelete.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onDeleted(self);
        }
    });

    EventDefinition<OnChildAdded> onChildAdded = EventFactory.define(OnChildAdded.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildAdded(context, widget);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildAddedPost> onChildAddedPost = EventFactory.define(OnChildAddedPost.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildAdded(context, widget);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildRemoved> onChildRemoved = EventFactory.define(OnChildRemoved.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(context, widget);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnChildRemovedPost> onChildRemovedPost = EventFactory.define(OnChildRemovedPost.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(context, widget);
            if (context.interrupted()) return;
        }
    });


    EventDefinition<OnTooltip> onTooltip = EventFactory.define(OnTooltip.class, listeners -> (context, tooltip) -> {
        for (var listener : listeners) {
            listener.onTooltip(context, tooltip);
            if (context.interrupted()) return;
        }
    });

    EventDefinition<OnThemeUpdate> onThemeUpdate = EventFactory.define(OnThemeUpdate.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onThemeUpdate();
        }
    });

    @FunctionalInterface
    interface OnPositionChanged extends WidgetEvent {
        void onPositionChanged(Common context, Pos position);
    }

    @FunctionalInterface
    interface OnSizeChanged extends WidgetEvent {
        void onSizeChanged(Common context, Dimension size);
    }

    @FunctionalInterface
    interface OnRender extends WidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Common context);
    }

    @FunctionalInterface
    interface OnInit extends WidgetEvent {
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
    interface OnDelete extends WidgetEvent {
        void onDeleted(Widget self);
    }

    @FunctionalInterface
    interface OnChildAdded extends WidgetEvent {
        void onChildAdded(Common context, Widget widget);
    }

    @FunctionalInterface
    interface OnChildAddedPost extends WidgetEvent {
        void onChildAdded(Interruptible context, Widget widget);
    }

    @FunctionalInterface
    interface OnChildRemoved extends WidgetEvent {
        void onChildRemoved(Common context, Widget widget);
    }

    @FunctionalInterface
    interface OnChildRemovedPost extends WidgetEvent {
        void onChildRemoved(Interruptible context, Widget widget);
    }

    @FunctionalInterface
    interface OnTooltip extends WidgetEvent {
        void onTooltip(Common context, RichTooltip richTooltip);
    }

    interface OnThemeUpdate extends WidgetEvent {
        void onThemeUpdate();
    }

}
