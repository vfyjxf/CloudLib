package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public interface Tooltip {
    boolean isText();

    boolean isComponent();

    Component asText();

    TooltipComponent asComponent();
}