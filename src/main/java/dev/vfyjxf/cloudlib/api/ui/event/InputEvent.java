package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.event.context.InterruptibleContext;
import dev.vfyjxf.cloudlib.api.ui.InputContext;

public interface InputEvent extends WidgetEvent {


    //region mouse click callbacks

    @FunctionalInterface
    interface OnMouseClicked extends InputEvent {
        EventDispatch onClicked(InputContext input, BubbleContext context);
    }

    @FunctionalInterface
    interface OnMouseReleased extends InputEvent {
        EventDispatch onReleased(InputContext input, BubbleContext context);
    }

    interface OnMouseClick extends InputEvent {
        EventDispatch onClick(InputContext input, int clickCount, BubbleContext context);
    }

    //endregion


    //region mouse movement callbacks

    @FunctionalInterface
    interface OnMouseDragged extends InputEvent {
        EventDispatch onDragged(InputContext input, double deltaX, double deltaY, BubbleContext context);
    }

    @FunctionalInterface
    interface OnMouseScrolled extends InputEvent {
        EventDispatch onScrolled(double mouseX, double mouseY, double scrollX, double scrollY, BubbleContext context);
    }

    @FunctionalInterface
    interface OnMouseMoved extends InputEvent {
        void onMoved(double mouseX, double mouseY, InterruptibleContext context);
    }

    @FunctionalInterface
    interface OnMouseEnter extends InputEvent {
        void onEnter(double mouseX, double mouseY, InterruptibleContext context);
    }

    @FunctionalInterface
    interface OnMouseLeave extends InputEvent {
        void onLeave(double mouseX, double mouseY, InterruptibleContext context);
    }

    //endregion


    //region keyboard callbacks

    @FunctionalInterface
    interface OnKeyPressed extends InputEvent {
        EventDispatch onKeyPressed(InputContext input, BubbleContext context);
    }

    @FunctionalInterface
    interface OnKeyReleased extends InputEvent {
        EventDispatch onKeyReleased(InputContext input, BubbleContext context);
    }

    interface OnCharTyped extends InputEvent {
        EventDispatch onCharTyped(char codePoint, int modifiers, BubbleContext context);
    }

    //endregion
}
