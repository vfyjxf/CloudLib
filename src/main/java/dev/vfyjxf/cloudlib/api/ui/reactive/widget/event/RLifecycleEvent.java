package dev.vfyjxf.cloudlib.api.ui.reactive.widget.event;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Lifecycle events for reactive widgets.
 * <p>
 * These events use broadcast propagation - they are sent to all descendants
 * and cannot be stopped.
 */
public interface RLifecycleEvent extends RWidgetEvent {

    // ===== Attach =====

    EventDef<OnAttach> onAttach = EventDef.broadcast(OnAttach.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onAttach(ctx);
        }
    });

    @FunctionalInterface
    interface OnAttach extends RLifecycleEvent {
        void onAttach(RUIContext ctx);
    }

    // ===== Detach =====

    EventDef<OnDetach> onDetach = EventDef.broadcast(OnDetach.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onDetach(ctx);
        }
    });

    @FunctionalInterface
    interface OnDetach extends RLifecycleEvent {
        void onDetach(RUIContext ctx);
    }

    // ===== Update =====

    EventDef<OnUpdate> onUpdate = EventDef.broadcast(OnUpdate.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onUpdate(ctx);
        }
    });

    @FunctionalInterface
    interface OnUpdate extends RLifecycleEvent {
        void onUpdate(RUIContext ctx);
    }

    // ===== Render =====

    EventDef<OnRender> onRender = EventDef.direct(OnRender.class, listeners -> (graphics, mouseX, mouseY, partialTick, ctx) -> {
        for (var listener : listeners) {
            listener.onRender(graphics, mouseX, mouseY, partialTick, ctx);
        }
    });

    @FunctionalInterface
    interface OnRender extends RLifecycleEvent {
        void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, RUIContext ctx);
    }

    // ===== Render Post =====

    EventDef<OnRenderPost> onRenderPost = EventDef.direct(OnRenderPost.class, listeners -> (graphics, mouseX, mouseY, partialTick, ctx) -> {
        for (var listener : listeners) {
            listener.onRenderPost(graphics, mouseX, mouseY, partialTick, ctx);
        }
    });

    @FunctionalInterface
    interface OnRenderPost extends RLifecycleEvent {
        void onRenderPost(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, RUIContext ctx);
    }

    // ===== Resize =====

    EventDef<OnResize> onResize = EventDef.broadcast(OnResize.class, listeners -> (oldWidth, oldHeight, newWidth, newHeight, ctx) -> {
        for (var listener : listeners) {
            listener.onResize(oldWidth, oldHeight, newWidth, newHeight, ctx);
        }
    });

    @FunctionalInterface
    interface OnResize extends RLifecycleEvent {
        void onResize(int oldWidth, int oldHeight, int newWidth, int newHeight, RUIContext ctx);
    }

    // ===== Focus =====

    EventDef<OnFocus> onFocus = EventDef.direct(OnFocus.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onFocus(ctx);
        }
    });

    @FunctionalInterface
    interface OnFocus extends RLifecycleEvent {
        void onFocus(RUIContext ctx);
    }

    // ===== Blur =====

    EventDef<OnBlur> onBlur = EventDef.direct(OnBlur.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onBlur(ctx);
        }
    });

    @FunctionalInterface
    interface OnBlur extends RLifecycleEvent {
        void onBlur(RUIContext ctx);
    }

    // ===== Show =====

    EventDef<OnShow> onShow = EventDef.broadcast(OnShow.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onShow(ctx);
        }
    });

    @FunctionalInterface
    interface OnShow extends RLifecycleEvent {
        void onShow(RUIContext ctx);
    }

    // ===== Hide =====

    EventDef<OnHide> onHide = EventDef.broadcast(OnHide.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onHide(ctx);
        }
    });

    @FunctionalInterface
    interface OnHide extends RLifecycleEvent {
        void onHide(RUIContext ctx);
    }
    
    // ===== Tick =====
    
    /**
     * Called every game tick (20 times per second).
     * Use for animations and time-based updates.
     */
    EventDef<OnTick> onTick = EventDef.broadcast(OnTick.class, listeners -> (ctx) -> {
        for (var listener : listeners) {
            listener.onTick(ctx);
        }
    });

    @FunctionalInterface
    interface OnTick extends RLifecycleEvent {
        void onTick(RUIContext ctx);
    }
}
