package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Action performed while the mouse hovers over an interactive fragment of a
 * laid-out rich text.
 */
public sealed interface HoverAction {

    /**
     * Context handed to {@link Callback} handlers: mouse position in widget-local
     * coordinates.
     */
    record Context(double mouseX, double mouseY) {}

    /**
     * Shows a CloudLib {@link Tooltip} via the scene tooltip pipeline.
     */
    record ShowTooltip(Tooltip tooltip) implements HoverAction {}

    /**
     * Shows plain text lines via the scene tooltip pipeline.
     */
    record ShowText(Component text) implements HoverAction {}

    /**
     * Invokes a user callback on hover.
     */
    record Callback(Consumer<Context> handler) implements HoverAction {}

    /**
     * Adapts a vanilla {@link HoverEvent}; converted to a tooltip following
     * vanilla semantics where possible.
     */
    record Vanilla(HoverEvent event) implements HoverAction {}

    static HoverAction tooltip(Tooltip tooltip) {
        return new ShowTooltip(tooltip);
    }

    static HoverAction text(Component text) {
        return new ShowText(text);
    }

    static HoverAction of(Consumer<Context> handler) {
        return new Callback(handler);
    }

    static HoverAction vanilla(HoverEvent event) {
        return new Vanilla(event);
    }

    /**
     * Picks the action of {@code node} if present, otherwise the inherited one.
     */
    static @Nullable HoverAction resolve(@Nullable HoverAction own, @Nullable HoverAction inherited) {
        return own != null ? own : inherited;
    }
}
