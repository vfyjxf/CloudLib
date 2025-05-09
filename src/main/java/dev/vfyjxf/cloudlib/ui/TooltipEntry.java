package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record TooltipEntry(Object tooltip) implements Tooltip {
    public TooltipEntry(Object tooltip) {
        this.tooltip = tooltip;

        if (!(tooltip instanceof Component) && !(tooltip instanceof TooltipComponent)) {
            throw new IllegalArgumentException("richTooltip must be a Component or ClientTextComponent");
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
