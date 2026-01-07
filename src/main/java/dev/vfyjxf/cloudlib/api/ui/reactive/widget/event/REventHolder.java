package dev.vfyjxf.cloudlib.api.ui.reactive.widget.event;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Holds all event instances for a widget.
 * <p>
 * Each widget has its own REventHolder to manage event listeners.
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyWidget extends RWidget {
 *     public MyWidget() {
 *         // Register click handler
 *         events().mouseClick().register((input, ctx) -> {
 *             System.out.println("Clicked at " + ctx.mouseX() + ", " + ctx.mouseY());
 *             ctx.consume(); // Stop propagation
 *             return true;
 *         });
 *         
 *         // Register with capture phase
 *         events().mouseClick().register((input, ctx) -> {
 *             System.out.println("Capturing click");
 *             return false;
 *         }, true);
 *     }
 * }
 * }</pre>
 */
public final class REventHolder {

    // Input events
    private final EventDef.REvent<RInputEvent.OnMouseClick> mouseClick;
    private final EventDef.REvent<RInputEvent.OnMouseRelease> mouseRelease;
    private final EventDef.REvent<RInputEvent.OnMouseDrag> mouseDrag;
    private final EventDef.REvent<RInputEvent.OnMouseScroll> mouseScroll;
    private final EventDef.REvent<RInputEvent.OnMouseMove> mouseMove;
    private final EventDef.REvent<RInputEvent.OnMouseEnter> mouseEnter;
    private final EventDef.REvent<RInputEvent.OnMouseLeave> mouseLeave;
    private final EventDef.REvent<RInputEvent.OnKeyPress> keyPress;
    private final EventDef.REvent<RInputEvent.OnKeyRelease> keyRelease;
    private final EventDef.REvent<RInputEvent.OnCharTyped> charTyped;

    // Lifecycle events
    private final EventDef.REvent<RLifecycleEvent.OnAttach> attach;
    private final EventDef.REvent<RLifecycleEvent.OnDetach> detach;
    private final EventDef.REvent<RLifecycleEvent.OnUpdate> update;
    private final EventDef.REvent<RLifecycleEvent.OnRender> render;
    private final EventDef.REvent<RLifecycleEvent.OnRenderPost> renderPost;
    private final EventDef.REvent<RLifecycleEvent.OnResize> resize;
    private final EventDef.REvent<RLifecycleEvent.OnFocus> focus;
    private final EventDef.REvent<RLifecycleEvent.OnBlur> blur;
    private final EventDef.REvent<RLifecycleEvent.OnShow> show;
    private final EventDef.REvent<RLifecycleEvent.OnHide> hide;
    private final EventDef.REvent<RLifecycleEvent.OnTick> tick;

    public REventHolder() {
        // Input events
        this.mouseClick = RInputEvent.onMouseClick.create();
        this.mouseRelease = RInputEvent.onMouseRelease.create();
        this.mouseDrag = RInputEvent.onMouseDrag.create();
        this.mouseScroll = RInputEvent.onMouseScroll.create();
        this.mouseMove = RInputEvent.onMouseMove.create();
        this.mouseEnter = RInputEvent.onMouseEnter.create();
        this.mouseLeave = RInputEvent.onMouseLeave.create();
        this.keyPress = RInputEvent.onKeyPress.create();
        this.keyRelease = RInputEvent.onKeyRelease.create();
        this.charTyped = RInputEvent.onCharTyped.create();

        // Lifecycle events
        this.attach = RLifecycleEvent.onAttach.create();
        this.detach = RLifecycleEvent.onDetach.create();
        this.update = RLifecycleEvent.onUpdate.create();
        this.render = RLifecycleEvent.onRender.create();
        this.renderPost = RLifecycleEvent.onRenderPost.create();
        this.resize = RLifecycleEvent.onResize.create();
        this.focus = RLifecycleEvent.onFocus.create();
        this.blur = RLifecycleEvent.onBlur.create();
        this.show = RLifecycleEvent.onShow.create();
        this.hide = RLifecycleEvent.onHide.create();
        this.tick = RLifecycleEvent.onTick.create();
    }

    // ===== Input Event Accessors =====

    public EventDef.REvent<RInputEvent.OnMouseClick> mouseClick() {
        return mouseClick;
    }

    public EventDef.REvent<RInputEvent.OnMouseRelease> mouseRelease() {
        return mouseRelease;
    }

    public EventDef.REvent<RInputEvent.OnMouseDrag> mouseDrag() {
        return mouseDrag;
    }

    public EventDef.REvent<RInputEvent.OnMouseScroll> mouseScroll() {
        return mouseScroll;
    }

    public EventDef.REvent<RInputEvent.OnMouseMove> mouseMove() {
        return mouseMove;
    }

    public EventDef.REvent<RInputEvent.OnMouseEnter> mouseEnter() {
        return mouseEnter;
    }

    public EventDef.REvent<RInputEvent.OnMouseLeave> mouseLeave() {
        return mouseLeave;
    }

    public EventDef.REvent<RInputEvent.OnKeyPress> keyPress() {
        return keyPress;
    }

    public EventDef.REvent<RInputEvent.OnKeyRelease> keyRelease() {
        return keyRelease;
    }

    public EventDef.REvent<RInputEvent.OnCharTyped> charTyped() {
        return charTyped;
    }

    // ===== Lifecycle Event Accessors =====

    public EventDef.REvent<RLifecycleEvent.OnAttach> attach() {
        return attach;
    }

    public EventDef.REvent<RLifecycleEvent.OnDetach> detach() {
        return detach;
    }

    public EventDef.REvent<RLifecycleEvent.OnUpdate> update() {
        return update;
    }

    public EventDef.REvent<RLifecycleEvent.OnRender> render() {
        return render;
    }

    public EventDef.REvent<RLifecycleEvent.OnRenderPost> renderPost() {
        return renderPost;
    }

    public EventDef.REvent<RLifecycleEvent.OnResize> resize() {
        return resize;
    }

    public EventDef.REvent<RLifecycleEvent.OnFocus> focus() {
        return focus;
    }

    public EventDef.REvent<RLifecycleEvent.OnBlur> blur() {
        return blur;
    }

    public EventDef.REvent<RLifecycleEvent.OnShow> show() {
        return show;
    }

    public EventDef.REvent<RLifecycleEvent.OnHide> hide() {
        return hide;
    }
    
    public EventDef.REvent<RLifecycleEvent.OnTick> tick() {
        return tick;
    }

    // ===== Convenience Methods =====

    /**
     * Fire a mouse click event.
     */
    public boolean fireMouseClick(InputContext input, RUIContext ctx) {
        if (mouseClick.hasListeners()) {
            return mouseClick.invoker().onMouseClick(input, ctx);
        }
        return false;
    }

    /**
     * Fire a mouse release event.
     */
    public boolean fireMouseRelease(InputContext input, RUIContext ctx) {
        if (mouseRelease.hasListeners()) {
            return mouseRelease.invoker().onMouseRelease(input, ctx);
        }
        return false;
    }

    /**
     * Fire a mouse drag event.
     */
    public boolean fireMouseDrag(InputContext input, double deltaX, double deltaY, RUIContext ctx) {
        if (mouseDrag.hasListeners()) {
            return mouseDrag.invoker().onMouseDrag(input, deltaX, deltaY, ctx);
        }
        return false;
    }

    /**
     * Fire a mouse scroll event.
     */
    public boolean fireMouseScroll(double scrollX, double scrollY, RUIContext ctx) {
        if (mouseScroll.hasListeners()) {
            return mouseScroll.invoker().onMouseScroll(scrollX, scrollY, ctx);
        }
        return false;
    }

    /**
     * Fire a key press event.
     */
    public boolean fireKeyPress(InputContext input, RUIContext ctx) {
        if (keyPress.hasListeners()) {
            return keyPress.invoker().onKeyPress(input, ctx);
        }
        return false;
    }

    /**
     * Fire an attach event.
     */
    public void fireAttach(RUIContext ctx) {
        if (attach.hasListeners()) {
            attach.invoker().onAttach(ctx);
        }
    }

    /**
     * Fire a detach event.
     */
    public void fireDetach(RUIContext ctx) {
        if (detach.hasListeners()) {
            detach.invoker().onDetach(ctx);
        }
    }

    /**
     * Fire an update event.
     */
    public void fireUpdate(RUIContext ctx) {
        if (update.hasListeners()) {
            update.invoker().onUpdate(ctx);
        }
    }

    /**
     * Fire a render event.
     */
    public void fireRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, RUIContext ctx) {
        if (render.hasListeners()) {
            render.invoker().onRender(graphics, mouseX, mouseY, partialTick, ctx);
        }
    }

    /**
     * Fire a render post event.
     */
    public void fireRenderPost(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, RUIContext ctx) {
        if (renderPost.hasListeners()) {
            renderPost.invoker().onRenderPost(graphics, mouseX, mouseY, partialTick, ctx);
        }
    }
}
