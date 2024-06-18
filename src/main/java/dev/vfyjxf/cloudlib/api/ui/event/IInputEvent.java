package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.IEventContext.Common;
import dev.vfyjxf.cloudlib.api.ui.inputs.IInputContext;

public interface IInputEvent {

    IUIEventDefinition<OnKeyPressed> onKeyPressed = UIEventFactory.define(OnKeyPressed.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyPressed(context, input);
            if (context.isInterrupted()) return result;
        }
        return result;
    });

    IUIEventDefinition<OnKeyReleased> onKeyReleased = UIEventFactory.define(OnKeyReleased.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyReleased(context, input);
            if (context.isInterrupted()) return result;
        }
        return result;
    });

    IUIEventDefinition<OnMouseClicked> onMouseClicked = UIEventFactory.define(OnMouseClicked.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onClicked(context, input);
            if (context.isCancelled()) return result;
        }
        return result;
    });

    IUIEventDefinition<OnMouseReleased> onMouseReleased = UIEventFactory.define(OnMouseReleased.class, listeners -> (context, input) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onReleased(context, input);
            if (context.isCancelled()) return result;
        }
        return result;
    });


    interface OnKeyPressed {
        boolean onKeyPressed(Common context, IInputContext input);
    }

    interface OnKeyReleased {
        boolean onKeyReleased(Common context, IInputContext input);
    }

    interface OnMouseClicked {
        boolean onClicked(Common context, IInputContext input);
    }

    interface OnMouseReleased {
        boolean onReleased(Common context, IInputContext input);
    }

    interface OnMouseDragged {
        void onDragged(Common context, IInputContext input, double deltaX, double deltaY);
    }

    interface OnMouseScrolled {
        void onScrolled(Common context, IInputContext input, double scrollDelta);
    }

    interface OnMouseMoved {
        void onMoved(Common context, IInputContext input);
    }
}
