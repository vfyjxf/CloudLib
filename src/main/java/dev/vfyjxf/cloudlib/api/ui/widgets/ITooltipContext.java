package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.widgets.TooltipContext;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

public interface ITooltipContext {

    static ITooltipContext of() {
        return ITooltipContext.of(Point.ZERO);
    }

    static ITooltipContext of(Point mouse) {
        return ITooltipContext.of(mouse, null);
    }

    static ITooltipContext of(Point mouse, @Nullable TooltipFlag flag) {
        return new TooltipContext(mouse, flag);
    }

    static ITooltipContext ofMouse() {
        return ITooltipContext.of(ScreenUtil.ofMouse());
    }

    TooltipFlag flag();

    Point mousePos();
}
