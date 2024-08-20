package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.IEventContext.Common;
import dev.vfyjxf.cloudlib.api.ui.traits.ITrait;
import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Point;
import net.minecraft.client.gui.GuiGraphics;

public interface IWidgetEvent {

    IUIEventDefinition<OnPositionChanged> onPositionChanged = UIEventFactory.define(OnPositionChanged.class, listeners -> (context, position) -> {
        for (var listener : listeners) {
            listener.onPositionChanged(context, position);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnSizeChanged> onSizeChanged = UIEventFactory.define(OnSizeChanged.class, listeners -> (context, size) -> {
        for (var listener : listeners) {
            listener.onSizeChanged(context, size);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnTraitUpdate> onAttributesAdded = UIEventFactory.define(OnTraitUpdate.class, listeners -> (context, trait) -> {
        for (var listener : listeners) {
            listener.onTraitUpdate(context, trait);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnRender> onRender = UIEventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnRender> onRenderPost = UIEventFactory.define(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTicks, context) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTicks, context);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnUpdate> onUpdate = UIEventFactory.define(OnUpdate.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onUpdate(widget);
        }
    });

    IUIEventDefinition<OnInit> onInit = UIEventFactory.define(OnInit.class, listeners -> (widget) -> {
        for (var listener : listeners) {
            listener.onInit(widget);
        }
    });

    IUIEventDefinition<OnTick> onTick = UIEventFactory.define(OnTick.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onTick();
        }
    });

    IUIEventDefinition<OnDelete> onDelete = UIEventFactory.define(OnDelete.class, listeners -> (self) -> {
        for (var listener : listeners) {
            listener.onDeleted(self);
        }
    });

    IUIEventDefinition<OnChildAdded> onChildAdded = UIEventFactory.define(OnChildAdded.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildAdded(context, widget);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnChildAddedPost> onChildAddedPost = UIEventFactory.define(OnChildAddedPost.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildAdded(context, widget);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnChildRemoved> onChildRemoved = UIEventFactory.define(OnChildRemoved.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(context, widget);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnChildRemovedPost> onChildRemovedPost = UIEventFactory.define(OnChildRemovedPost.class, listeners -> (context, widget) -> {
        for (var listener : listeners) {
            listener.onChildRemoved(context, widget);
            if (context.isInterrupted()) return;
        }
    });


    IUIEventDefinition<OnTooltip> onTooltip = UIEventFactory.define(OnTooltip.class, listeners -> (context, tooltip) -> {
        for (var listener : listeners) {
            listener.onTooltip(context, tooltip);
            if (context.isInterrupted()) return;
        }
    });

    IUIEventDefinition<OnMeasure> onMeasure = UIEventFactory.define(OnMeasure.class, listeners -> (widget, size) -> {
        for (var listener : listeners) {
            listener.onMeasure(widget, size);
        }
    });

    IUIEventDefinition<OnLayout> onLayout = UIEventFactory.define(OnLayout.class, listeners -> (context) -> {
        for (var listener : listeners) {
            listener.onLayout(context);
        }
    });

    IUIEventDefinition<OnThemeUpdate> onThemeUpdate = UIEventFactory.define(OnThemeUpdate.class, listeners -> () -> {
        for (var listener : listeners) {
            listener.onThemeUpdate();
        }
    });

    @FunctionalInterface
    interface OnPositionChanged extends IWidgetEvent {
        void onPositionChanged(Common context, Point position);
    }

    @FunctionalInterface
    interface OnSizeChanged extends IWidgetEvent {
        void onSizeChanged(Common context, Dimension size);
    }

    @FunctionalInterface
    interface OnTraitUpdate extends IWidgetEvent {
        void onTraitUpdate(Common context, ITrait trait);
    }

    @FunctionalInterface
    interface OnRender extends IWidgetEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, Common context);
    }

    @FunctionalInterface
    interface OnInit extends IWidgetEvent {
        void onInit(IWidget widget);
    }

    @FunctionalInterface
    interface OnUpdate extends IWidgetEvent {
        void onUpdate(IWidget widget);
    }

    @FunctionalInterface
    interface OnTick extends IWidgetEvent {
        void onTick();
    }

    @FunctionalInterface
    interface OnDelete extends IWidgetEvent {
        void onDeleted(IWidget self);
    }

    @FunctionalInterface
    interface OnChildAdded extends IWidgetEvent {
        void onChildAdded(Common context, IWidget widget);
    }

    @FunctionalInterface
    interface OnChildAddedPost extends IWidgetEvent {
        void onChildAdded(Common context, IWidget widget);
    }

    @FunctionalInterface
    interface OnChildRemoved extends IWidgetEvent {
        void onChildRemoved(Common context, IWidget widget);
    }

    @FunctionalInterface
    interface OnChildRemovedPost extends IWidgetEvent {
        void onChildRemoved(Common context, IWidget widget);
    }

    @FunctionalInterface
    interface OnMouseHover extends IWidgetEvent {
        void onHover(int mouseX, int mouseY);
    }

    @FunctionalInterface
    interface OnTooltip extends IWidgetEvent {
        void onTooltip(Common context, ITooltip tooltip);
    }

    interface OnMeasure extends IWidgetEvent {
        void onMeasure(IWidget widget, Dimension size);
    }

    interface OnLayout extends IWidgetEvent {
        void onLayout(IWidget widget);
    }

    interface OnThemeUpdate extends IWidgetEvent {
        void onThemeUpdate();
    }

}
