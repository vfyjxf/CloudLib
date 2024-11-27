package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventContext.Common;
import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.ui.inputs.InputContext;

public interface InputEvent extends WidgetEvent {

    EventDefinition<OnKeyPressed> onKeyPressed = EventFactory.define(OnKeyPressed.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyPressed(context, input);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnKeyReleased> onKeyReleased = EventFactory.define(OnKeyReleased.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyReleased(context, input);
            if (context.interrupted()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseClicked> onMouseClicked = EventFactory.define(OnMouseClicked.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onClicked(context, input);
            if (context.cancelled()) return result;
        }
        return result;
    });

    EventDefinition<OnMouseReleased> onMouseReleased = EventFactory.define(OnMouseReleased.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onReleased(context, input);
            if (context.cancelled()) return result;
        }
        return result;
    });


    interface OnKeyPressed extends InputEvent{
        boolean onKeyPressed(Common context, InputContext input);
    }

    interface OnKeyReleased extends InputEvent{
        boolean onKeyReleased(Common context, InputContext input);
    }

    interface OnMouseClicked extends InputEvent{
        boolean onClicked(Common context, InputContext input);
    }

    interface OnMouseReleased extends InputEvent{
        boolean onReleased(Common context, InputContext input);
    }

    interface OnMouseDragged extends InputEvent{
        void onDragged(Common context, InputContext input, double deltaX, double deltaY);
    }

    interface OnMouseScrolled extends InputEvent{
        void onScrolled(Common context, InputContext input, double scrollX, double scrollY);
    }

    interface OnMouseMoved extends InputEvent{
        void onMoved(Common context, InputContext input);
    }
}
