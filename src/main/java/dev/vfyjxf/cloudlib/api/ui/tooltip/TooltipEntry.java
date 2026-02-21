package dev.vfyjxf.cloudlib.api.ui.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Supplier;

public sealed interface TooltipEntry {

    //region factory

    static TooltipEntry text(Component text) {
        return new TextEntry(text);
    }

    static TooltipEntry component(TooltipComponent component) {
        return new ComponentEntry(component);
    }

    static TooltipEntry dynamic(Supplier<Component> provider) {
        return new DynamicEntry(provider);
    }

    //endregion

    //region types

    record TextEntry(Component text) implements TooltipEntry {}

    record ComponentEntry(TooltipComponent component) implements TooltipEntry {}

    record DynamicEntry(Supplier<Component> provider) implements TooltipEntry {}

    //endregion

}
