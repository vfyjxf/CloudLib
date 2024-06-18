package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltipContext;
import dev.vfyjxf.cloudlib.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record TooltipContext(Point mousePos, @Nullable TooltipFlag flag) implements ITooltipContext {

    public TooltipContext(Point mousePos, @Nullable TooltipFlag flag) {
        this.mousePos = Objects.requireNonNull(mousePos);
        this.flag = flag;
    }

    @Override
    public TooltipFlag flag() {
        if (flag == null)
            return (Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
        else return flag;
    }
}
