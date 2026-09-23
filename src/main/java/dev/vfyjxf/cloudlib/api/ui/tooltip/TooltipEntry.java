package dev.vfyjxf.cloudlib.api.ui.tooltip;

import dev.vfyjxf.cloudlib.api.text.RichText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Supplier;

public sealed interface TooltipEntry {

    // region factory

    static TooltipEntry text(Component text) {
        return new TextEntry(text);
    }

    static TooltipEntry component(TooltipComponent component) {
        return new ComponentEntry(component);
    }

    static TooltipEntry dynamic(Supplier<Component> provider) {
        return new DynamicEntry(provider);
    }

    /**
     * A rich text block inside a tooltip. Purely textual documents degrade to a
     * vanilla component line; rich content is rendered by the rich text pipeline.
     */
    static TooltipEntry richText(RichText text) {
        return new RichTextEntry(text);
    }

    // endregion

    // region types

    record TextEntry(Component text) implements TooltipEntry {}

    record ComponentEntry(TooltipComponent component) implements TooltipEntry {}

    record DynamicEntry(Supplier<Component> provider) implements TooltipEntry {}

    record RichTextEntry(RichText text) implements TooltipEntry {}

    // endregion

}
