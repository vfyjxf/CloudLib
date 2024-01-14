package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Supplier;

public record SupplierTooltipEntry(Supplier<Component> supplier) implements ITooltip.Entry {

    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public boolean isComponent() {
        return false;
    }

    @Override
    public Component asText() {
        return supplier.get();
    }

    @Override
    public TooltipComponent asComponent() {
        throw new UnsupportedOperationException("Only text entries have a supplier support");
    }
}
