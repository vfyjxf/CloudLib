package dev.vfyjxf.cloudlib.api.ui.tooltip;

import dev.vfyjxf.cloudlib.api.text.RichText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Vanilla-side carrier for a rich text tooltip entry. Registered with the client
 * tooltip component factories to be rendered by the rich text pipeline.
 */
public record RichTextTooltipComponent(RichText text) implements TooltipComponent {}
