package dev.vfyjxf.cloudlib.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiGraphics.class, remap = false)
public interface GuiGraphicsAccessor {

    @Accessor(remap = false)
    void setTooltipStack(ItemStack stack);
}
