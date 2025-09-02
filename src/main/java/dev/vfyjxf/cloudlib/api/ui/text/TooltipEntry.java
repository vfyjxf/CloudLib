package dev.vfyjxf.cloudlib.api.ui.text;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Supplier;

public sealed interface TooltipEntry {

    static TooltipEntry of(Component text) {
        return new TextEntry(text);
    }

    static TooltipEntry of(TooltipComponent component) {
        return new ComponentEntry(component);
    }

    record TextEntry(Component text) implements TooltipEntry {}

    record TextProvider(Supplier<Component> provider) implements TooltipEntry {}

    record ComponentEntry(TooltipComponent component) implements TooltipEntry {}


}

