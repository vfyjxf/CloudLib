package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventDefinition;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.Events;

public final class InputEvents {

    //region mouse click events

    public static final EventDefinition<InputEvent.OnMouseClicked> onMouseClicked = Events.define(InputEvent.OnMouseClicked.class, listeners -> (input, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onClicked(input, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    public static final EventDefinition<InputEvent.OnMouseReleased> onMouseReleased = Events.define(InputEvent.OnMouseReleased.class, listeners -> (input, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onReleased(input, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    public static final EventDefinition<InputEvent.OnMouseClick> onMouseClick = Events.define(InputEvent.OnMouseClick.class, listeners -> (input, clickCount, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onClick(input, clickCount, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    //endregion


    //region mouse movement events

    public static final EventDefinition<InputEvent.OnMouseDragged> onMouseDragged = Events.define(InputEvent.OnMouseDragged.class, listeners -> (input, deltaX, deltaY, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onDragged(input, deltaX, deltaY, context));
            if (context.interrupted()) return result;
        }
        return result;
    });

    public static final EventDefinition<InputEvent.OnMouseScrolled> onMouseScrolled = Events.define(InputEvent.OnMouseScrolled.class, listeners -> (mouseX, mouseY, scrollX, scrollY, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onScrolled(mouseX, mouseY, scrollX, scrollY, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    public static final EventDefinition<InputEvent.OnMouseMoved> onMouseMoved = Events.define(InputEvent.OnMouseMoved.class, listeners -> (mouseX, mouseY, context) -> {
        for (var listener : listeners) {
            listener.onMoved(mouseX, mouseY, context);
            if (context.interrupted()) return;
        }
    });

    public static final EventDefinition<InputEvent.OnMouseEnter> onMouseEnter = Events.define(InputEvent.OnMouseEnter.class, listeners -> (mouseX, mouseY, context) -> {
        for (var listener : listeners) {
            listener.onEnter(mouseX, mouseY, context);
            if (context.interrupted()) return;
        }
    });

    public static final EventDefinition<InputEvent.OnMouseLeave> onMouseLeave = Events.define(InputEvent.OnMouseLeave.class, listeners -> (mouseX, mouseY, context) -> {
        for (var listener : listeners) {
            listener.onLeave(mouseX, mouseY, context);
            if (context.interrupted()) return;
        }
    });

    //endregion


    //region keyboard events

    public static final EventDefinition<InputEvent.OnKeyPressed> onKeyPressed = Events.define(InputEvent.OnKeyPressed.class, listeners -> (input, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onKeyPressed(input, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    public static final EventDefinition<InputEvent.OnKeyReleased> onKeyReleased = Events.define(InputEvent.OnKeyReleased.class, listeners -> (input, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onKeyReleased(input, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    public static final EventDefinition<InputEvent.OnCharTyped> onCharTyped = Events.define(InputEvent.OnCharTyped.class, listeners -> (codePoint, modifiers, context) -> {
        EventDispatch result = EventDispatch.pass;
        for (var listener : listeners) {
            result = EventDispatch.max(result, listener.onCharTyped(codePoint, modifiers, context));
            if (result == EventDispatch.consumed) context.consume();
            if (context.interrupted()) return result;
        }
        return result;
    });

    //endregion
}
