package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.event.IEventContext;
import dev.vfyjxf.cloudlib.api.event.IEventDefinition;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;

public interface IInputEvent {

    IEventDefinition<OnKeyPressed> onKeyPressed = EventFactory.define(OnKeyPressed.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyPressed(context, input);
            if (context.isInterrupted()) return result;
        }
        return result;
    });

    IEventDefinition<OnKeyReleased> onKeyReleased = EventFactory.define(OnKeyReleased.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyReleased(context, input);
            if (context.isInterrupted()) return result;
        }
        return result;
    });

    IEventDefinition<OnMouseClicked> onMouseClicked = EventFactory.define(OnMouseClicked.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onClicked(context, input);
            if (context.isCancelled()) return result;
        }
        return result;
    });

    IEventDefinition<OnMouseReleased> onMouseReleased = EventFactory.define(OnMouseReleased.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onReleased(context, input);
            if (context.isCancelled()) return result;
        }
        return result;
    });


    interface OnKeyPressed {
        boolean onKeyPressed(IEventContext context, IInputContext input);
    }

    interface OnKeyReleased {
        boolean onKeyReleased(IEventContext context, IInputContext input);
    }

    interface OnMouseClicked {
        boolean onClicked(IEventContext context, IInputContext input);
    }

    interface OnMouseReleased {
        boolean onReleased(IEventContext context, IInputContext input);
    }

    interface OnMouseDragged {
        void onDragged(IEventContext context, IInputContext input, double deltaX, double deltaY);
    }

    interface OnMouseScrolled {
        void onScrolled(IEventContext context, IInputContext input, double scrollDelta);
    }

    interface OnMouseMoved {
        void onMoved(IEventContext context, IInputContext input);
    }
}
