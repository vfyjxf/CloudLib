package dev.vfyjxf.cloudlib.api.ui.reactive.widget.event;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * Context for reactive UI operations.
 * <p>
 * Provides access to commonly used rendering and UI state,
 * as well as event propagation control.
 */
public final class RUIContext {

    private final RWidget target;
    private final @Nullable GuiGraphics graphics;
    private final @Nullable Font font;
    private final int mouseX;
    private final int mouseY;
    private final float partialTick;

    // Event propagation state
    private boolean cancelled = false;
    private boolean propagationStopped = false;

    private RUIContext(RWidget target, @Nullable GuiGraphics graphics, @Nullable Font font,
                       int mouseX, int mouseY, float partialTick) {
        this.target = target;
        this.graphics = graphics;
        this.font = font;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTick = partialTick;
    }

    /**
     * Creates a context for event handling (no graphics).
     */
    public static RUIContext forEvent(RWidget target, int mouseX, int mouseY) {
        return new RUIContext(target, null, null, mouseX, mouseY, 0);
    }

    /**
     * Creates a context for rendering.
     */
    public static RUIContext forRender(RWidget target, GuiGraphics graphics, Font font,
                                       int mouseX, int mouseY, float partialTick) {
        return new RUIContext(target, graphics, font, mouseX, mouseY, partialTick);
    }

    /**
     * Creates an empty context.
     */
    public static RUIContext empty(RWidget target) {
        return new RUIContext(target, null, null, 0, 0, 0);
    }

    // ===== Event Control =====

    /**
     * Cancel the event (prevents default behavior).
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * Stop propagation to other widgets.
     */
    public void stopPropagation() {
        propagationStopped = true;
    }

    /**
     * Cancel and stop propagation.
     */
    public void consume() {
        cancelled = true;
        propagationStopped = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isPropagationStopped() {
        return propagationStopped;
    }

    // ===== Getters =====

    /**
     * The widget that is the target of this context.
     */
    public RWidget target() {
        return target;
    }

    /**
     * The graphics context for rendering. May be null if not in render phase.
     */
    public @Nullable GuiGraphics graphics() {
        return graphics;
    }

    /**
     * The font for text rendering. May be null if not in render phase.
     */
    public @Nullable Font font() {
        return font;
    }

    /**
     * Current mouse X position.
     */
    public int mouseX() {
        return mouseX;
    }

    /**
     * Current mouse Y position.
     */
    public int mouseY() {
        return mouseY;
    }

    /**
     * Partial tick for smooth animations.
     */
    public float partialTick() {
        return partialTick;
    }

    /**
     * Check if mouse is over the target widget.
     */
    public boolean isHovered() {
        return target.contains(mouseX, mouseY);
    }

    /**
     * Get mouse position relative to target widget.
     */
    public int relativeMouseX() {
        return mouseX - target.getX();
    }

    /**
     * Get mouse position relative to target widget.
     */
    public int relativeMouseY() {
        return mouseY - target.getY();
    }
}
