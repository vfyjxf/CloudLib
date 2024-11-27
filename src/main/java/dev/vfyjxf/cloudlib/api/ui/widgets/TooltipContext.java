package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public record TooltipContext(Pos mousePos, @Nullable TooltipFlag flag) {

    static TooltipContext of() {
        return TooltipContext.of(Pos.ZERO);
    }

    static TooltipContext of(Pos mouse) {
        return TooltipContext.of(mouse, null);
    }

    static TooltipContext of(Pos mouse, @Nullable TooltipFlag flag) {
        return new TooltipContext(mouse, flag);
    }

    static TooltipContext ofMouse() {
        return TooltipContext.of(ScreenUtil.ofMouse());
    }

    public TooltipContext(Pos mousePos, @Nullable TooltipFlag flag) {
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