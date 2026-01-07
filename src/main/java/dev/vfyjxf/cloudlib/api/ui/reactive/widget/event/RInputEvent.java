package dev.vfyjxf.cloudlib.api.ui.reactive.widget.event;

import dev.vfyjxf.cloudlib.api.ui.InputContext;

/**
 * Input events for reactive widgets.
 * <p>
 * These events follow the capture-bubble model:
 * <ul>
 *   <li><b>Capture phase</b>: root → target (can intercept)</li>
 *   <li><b>Target phase</b>: at target widget</li>
 *   <li><b>Bubble phase</b>: target → root (can handle after)</li>
 * </ul>
 * <p>
 * All input events use {@link InputContext} for input state.
 */
public interface RInputEvent extends RWidgetEvent {

    // ===== Mouse Click =====

    EventDef<OnMouseClick> onMouseClick = EventDef.bubbling(OnMouseClick.class, listeners -> (input, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onMouseClick(input, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnMouseClick extends RInputEvent {
        boolean onMouseClick(InputContext input, RUIContext ctx);
    }

    // ===== Mouse Release =====

    EventDef<OnMouseRelease> onMouseRelease = EventDef.bubbling(OnMouseRelease.class, listeners -> (input, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onMouseRelease(input, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnMouseRelease extends RInputEvent {
        boolean onMouseRelease(InputContext input, RUIContext ctx);
    }

    // ===== Mouse Drag =====

    EventDef<OnMouseDrag> onMouseDrag = EventDef.bubbling(OnMouseDrag.class, listeners -> (input, deltaX, deltaY, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onMouseDrag(input, deltaX, deltaY, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnMouseDrag extends RInputEvent {
        boolean onMouseDrag(InputContext input, double deltaX, double deltaY, RUIContext ctx);
    }

    // ===== Mouse Scroll =====

    EventDef<OnMouseScroll> onMouseScroll = EventDef.bubbling(OnMouseScroll.class, listeners -> (scrollX, scrollY, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onMouseScroll(scrollX, scrollY, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnMouseScroll extends RInputEvent {
        boolean onMouseScroll(double scrollX, double scrollY, RUIContext ctx);
    }

    // ===== Mouse Move =====

    EventDef<OnMouseMove> onMouseMove = EventDef.bubbling(OnMouseMove.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onMouseMove(ctx);
            if (ctx.isPropagationStopped()) return;
        }
    });

    @FunctionalInterface
    interface OnMouseMove extends RInputEvent {
        void onMouseMove(RUIContext ctx);
    }

    // ===== Mouse Enter (non-bubbling) =====

    EventDef<OnMouseEnter> onMouseEnter = EventDef.direct(OnMouseEnter.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onMouseEnter(ctx);
        }
    });

    @FunctionalInterface
    interface OnMouseEnter extends RInputEvent {
        void onMouseEnter(RUIContext ctx);
    }

    // ===== Mouse Leave (non-bubbling) =====

    EventDef<OnMouseLeave> onMouseLeave = EventDef.direct(OnMouseLeave.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onMouseLeave(ctx);
        }
    });

    @FunctionalInterface
    interface OnMouseLeave extends RInputEvent {
        void onMouseLeave(RUIContext ctx);
    }

    // ===== Key Press =====

    EventDef<OnKeyPress> onKeyPress = EventDef.bubbling(OnKeyPress.class, listeners -> (input, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyPress(input, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnKeyPress extends RInputEvent {
        boolean onKeyPress(InputContext input, RUIContext ctx);
    }

    // ===== Key Release =====

    EventDef<OnKeyRelease> onKeyRelease = EventDef.bubbling(OnKeyRelease.class, listeners -> (input, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onKeyRelease(input, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnKeyRelease extends RInputEvent {
        boolean onKeyRelease(InputContext input, RUIContext ctx);
    }

    // ===== Char Typed =====

    EventDef<OnCharTyped> onCharTyped = EventDef.bubbling(OnCharTyped.class, listeners -> (character, modifiers, ctx) -> {
        boolean result = false;
        for (var listener : listeners) {
            result |= listener.onCharTyped(character, modifiers, ctx);
            if (ctx.isPropagationStopped()) return result;
        }
        return result;
    });

    @FunctionalInterface
    interface OnCharTyped extends RInputEvent {
        boolean onCharTyped(char character, int modifiers, RUIContext ctx);
    }
}
