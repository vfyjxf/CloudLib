package dev.vfyjxf.cloudlib.api.text;

import net.minecraft.network.chat.Component;

/**
 * Adapts any vanilla {@link Component} (literal, translatable, keybind, score, ...)
 * into the rich text flow. The component keeps its own styles; translation and
 * sibling structure are resolved through the vanilla component pipeline at layout
 * time.
 */
public record ComponentNode(Component component) implements RichNode {

    public ComponentNode {
        if (component == null) throw new NullPointerException("component");
    }
}
