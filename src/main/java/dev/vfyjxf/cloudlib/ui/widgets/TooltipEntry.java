package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record TooltipEntry(Object tooltip) implements ITooltip.Entry {
    public TooltipEntry(Object tooltip) {
        this.tooltip = tooltip;

        if (!(tooltip instanceof Component) && !(tooltip instanceof TooltipComponent)) {
            throw new IllegalArgumentException("tooltip must be a Component or ClientTextComponent");
        }
    }


    @Override
    public boolean isText() {
        return tooltip instanceof Component;
    }


    @Override
    public boolean isComponent() {
        return tooltip instanceof TooltipComponent;
    }

    @Override
    public Component asText() {
        return ((Component) tooltip);
    }

    @Override
    public TooltipComponent asComponent() {
        return ((TooltipComponent) tooltip);
    }


}
