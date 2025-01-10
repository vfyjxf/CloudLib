package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventContext.Common;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.ui.InputContext;

import static dev.vfyjxf.cloudlib.api.event.EventContext.Interruptible;

public interface InputEvent extends WidgetEvent {

    EventDefinition<OnKeyPressed> onKeyPressed = EventFactory.define(OnKeyPressed.class, listeners -> (input, context) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyPressed(input, context);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnKeyReleased> onKeyReleased = EventFactory.define(OnKeyReleased.class, listeners -> (input, context) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyReleased(input, context);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseClicked> onMouseClicked = EventFactory.define(OnMouseClicked.class, listeners -> (input, context) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onClicked(input, context);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseReleased> onMouseReleased = EventFactory.define(OnMouseReleased.class, listeners -> (input, context) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onReleased(input, context);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseHover> onMouseHover = EventFactory.define(OnMouseHover.class, listeners -> (mouseX, mouseY) -> {
        for (var listener : listeners) {
            listener.onHover(mouseX, mouseY);
        }
    });

    EventDefinition<OnMouseDragged> onMouseDragged = EventFactory.define(OnMouseDragged.class, listeners -> (input, deltaX, deltaY, context) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onDragged(input, deltaX, deltaY, context);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseScrolled> onMouseScrolled = EventFactory.define(OnMouseScrolled.class, listeners -> (mouseX, mouseY, scrollX, scrollY, context) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onScrolled(mouseX, mouseY, scrollX, scrollY, context);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseMoved> onMouseMoved = EventFactory.define(OnMouseMoved.class, listeners -> (mouseX, mouseY, context) -> {
        for (var listener : listeners) {
            listener.onMoved(mouseX, mouseY, context);
            if (context.interrupted()) return;
        }
    });

    @FunctionalInterface
    interface OnKeyPressed extends InputEvent {
        boolean onKeyPressed(InputContext input, Common context);
    }

    @FunctionalInterface
    interface OnKeyReleased extends InputEvent {
        boolean onKeyReleased(InputContext input, Common context);
    }

    @FunctionalInterface
    interface OnMouseClicked extends InputEvent {
        boolean onClicked(InputContext input, Common context);
    }

    @FunctionalInterface
    interface OnMouseReleased extends InputEvent {
        boolean onReleased(InputContext input, Common context);
    }

    @FunctionalInterface
    interface OnMouseDragged extends InputEvent {
        boolean onDragged(InputContext input, double deltaX, double deltaY, Common context);
    }

    @FunctionalInterface
    interface OnMouseScrolled extends InputEvent {
        boolean onScrolled(double mouseX, double mouseY, double scrollX, double scrollY, Common context);
    }

    @FunctionalInterface
    interface OnMouseMoved extends InputEvent {
        void onMoved(double mouseX, double mouseY, Interruptible context);
    }

    @FunctionalInterface
    interface OnMouseHover extends InputEvent {
        void onHover(int mouseX, int mouseY);
    }
}
