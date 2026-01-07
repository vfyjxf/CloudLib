package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.Style;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for reactive widgets.
 * <p>
 * Unlike the main Widget system, RWidget is designed specifically for
 * the reactive rendering pipeline and ElementTree integration.
 * <p>
 * Features:
 * <ul>
 *   <li>Lightweight - minimal overhead</li>
 *   <li>Direct rendering - no complex layout system</li>
 *   <li>Style-based configuration</li>
 *   <li>Designed for fine-grained updates</li>
 *   <li>Capture-bubble event model for interaction events</li>
 *   <li>Broadcast model for lifecycle events</li>
 * </ul>
 * 
 * <h2>Event Model</h2>
 * <p>
 * Interaction events (mouse, scroll, keyboard) follow the capture-bubble model:
 * <ul>
 *   <li><b>Capture phase</b>: Event travels from root to target</li>
 *   <li><b>Target phase</b>: Event reaches the target widget</li>
 *   <li><b>Bubble phase</b>: Event travels from target back to root</li>
 * </ul>
 * <p>
 * Lifecycle events (attach, detach, update) use broadcast propagation:
 * all descendants receive the event.
 * 
 * <h2>Event Usage</h2>
 * <pre>{@code
 * widget.events().mouseClick().register((input, ctx) -> {
 *     System.out.println("Clicked!");
 *     ctx.consume(); // Stop propagation
 *     return true;
 * });
 * }</pre>
 */
public abstract class RWidget {

    protected int x, y;
    protected int width, height;
    protected @Nullable Style style;
    protected boolean visible = true;
    protected @Nullable RWidget parent;

    // Event holder
    private final REventHolder events = new REventHolder();

    // ===== Position & Size =====

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // ===== Parent =====

    public @Nullable RWidget getParent() {
        return parent;
    }

    public void setParent(@Nullable RWidget parent) {
        this.parent = parent;
    }

    // ===== Events =====

    /**
     * Get the event holder for this widget.
     * Use this to register event listeners.
     */
    public REventHolder events() {
        return events;
    }

    // ===== Style =====

    public void setStyle(@Nullable Style style) {
        this.style = style;
    }

    public @Nullable Style getStyle() {
        return style;
    }

    protected int getStyleColor(int defaultColor) {
        if (style != null) {
            var color = style.get(Style.Color.class);
            if (color != null) return color.value();
        }
        return defaultColor;
    }

    protected int getStyleBackground(int defaultColor) {
        if (style != null) {
            var bg = style.get(Style.Background.class);
            if (bg != null) return bg.color();
        }
        return defaultColor;
    }

    // ===== Visibility =====

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    // ===== Rendering =====

    /**
     * Renders this widget.
     *
     * @param graphics the graphics context
     * @param font     the font
     * @param mouseX   mouse X position
     * @param mouseY   mouse Y position
     * @param delta    partial tick
     */
    public abstract void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta);

    /**
     * Measures this widget's preferred size.
     * Called before layout.
     *
     * @param font the font for text measurement
     * @return [width, height]
     */
    public abstract int[] measure(Font font);

    // ===== Event Dispatching =====

    /**
     * Dispatch a bubbling event through capture → target → bubble phases.
     *
     * @param input   the input context
     * @param mouseX  mouse X
     * @param mouseY  mouse Y
     * @param handler the handler to invoke at each phase
     * @return true if the event was handled
     */
    protected boolean dispatchBubblingEvent(InputContext input, int mouseX, int mouseY, 
                                            BubblingEventHandler handler) {
        RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
        List<RWidget> path = buildPathFromRoot();

        // Capture phase: root → target
        for (int i = 0; i < path.size() - 1; i++) {
            if (ctx.isPropagationStopped()) break;
            handler.handle(path.get(i), input, ctx, true);
        }

        // Target phase
        if (!ctx.isPropagationStopped()) {
            handler.handle(this, input, ctx, true);  // Capture handlers
            if (!ctx.isPropagationStopped()) {
                handler.handle(this, input, ctx, false); // Bubble handlers
            }
        }

        // Bubble phase: target → root
        if (!ctx.isPropagationStopped()) {
            for (int i = path.size() - 2; i >= 0; i--) {
                if (ctx.isPropagationStopped()) break;
                handler.handle(path.get(i), input, ctx, false);
            }
        }

        return ctx.isCancelled();
    }

    @FunctionalInterface
    protected interface BubblingEventHandler {
        void handle(RWidget widget, InputContext input, RUIContext ctx, boolean capture);
    }

    /**
     * Builds the path from root to this widget.
     */
    protected List<RWidget> buildPathFromRoot() {
        List<RWidget> path = new ArrayList<>();
        RWidget current = this;
        while (current != null) {
            path.addFirst(current);
            current = current.parent;
        }
        return path;
    }

    /**
     * Broadcasts a lifecycle event to all children.
     * Override in container widgets.
     */
    protected void broadcastToChildren(RUIContext ctx, BroadcastHandler handler) {
        // Base implementation does nothing - override in containers
    }

    @FunctionalInterface
    protected interface BroadcastHandler {
        void handle(RWidget widget, RUIContext ctx);
    }

    // ===== Hit Testing =====

    /**
     * Checks if a point is inside this widget.
     */
    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    // ===== Input Handling =====

    /**
     * Called when mouse is clicked on this widget.
     *
     * @return true if handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;

        InputContext input = InputContext.fromMouse(mouseX, mouseY, button);
        RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
        
        return events.fireMouseClick(input, ctx) || ctx.isCancelled();
    }

    /**
     * Called when mouse is released over this widget.
     *
     * @return true if handled
     */
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (!visible) return false;

        InputContext input = InputContext.fromMouse(mouseX, mouseY, button, true);
        RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
        
        return events.fireMouseRelease(input, ctx) || ctx.isCancelled();
    }

    /**
     * Called when mouse is scrolled over this widget.
     *
     * @return true if handled
     */
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible) return false;

        RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
        return events.fireMouseScroll(scrollX, scrollY, ctx) || ctx.isCancelled();
    }

    /**
     * Called when a key is pressed.
     *
     * @return true if handled
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        InputContext input = InputContext.fromKeyboard(keyCode, scanCode, modifiers, 0, 0);
        RUIContext ctx = RUIContext.forEvent(this, 0, 0);
        
        return events.fireKeyPress(input, ctx) || ctx.isCancelled();
    }

    // ===== Lifecycle =====

    /**
     * Called when widget is attached to the render tree.
     * Broadcasts Attach event to all descendants.
     */
    public void onAttach() {
        RUIContext ctx = RUIContext.empty(this);
        events.fireAttach(ctx);
        broadcastToChildren(ctx, (widget, c) -> widget.onAttach());
    }

    /**
     * Called when widget is detached from the render tree.
     * Broadcasts Detach event to all descendants.
     */
    public void onDetach() {
        RUIContext ctx = RUIContext.empty(this);
        events.fireDetach(ctx);
        broadcastToChildren(ctx, (widget, c) -> widget.onDetach());
    }

    /**
     * Called when widget needs to update its state.
     * Broadcasts Update event to all descendants.
     */
    public void update() {
        RUIContext ctx = RUIContext.empty(this);
        events.fireUpdate(ctx);
        broadcastToChildren(ctx, (widget, c) -> widget.update());
    }
}
