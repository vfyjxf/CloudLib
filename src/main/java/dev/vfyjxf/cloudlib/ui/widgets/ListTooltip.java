package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltipStack;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class ListTooltip implements ITooltip {

    private Point mousePos;
    private MutableList<ITooltip.Entry> entries;
    private ITooltipStack<?> stack = null;

    public ListTooltip(@Nullable Point mousePos, Collection<? extends Entry> entries) {
        this.mousePos = mousePos;
        if (this.mousePos == null) {
            this.mousePos = ScreenUtil.ofMouse();
        }
        this.entries = Lists.mutable.ofAll(entries);
    }


    @Override
    public int getX() {
        return mousePos.x;
    }

    @Override
    public int getY() {
        return mousePos.y;
    }

    @Override
    public List<Entry> entries() {
        return entries;
    }

    @Override
    public ITooltip add(Component text) {
        entries.add(new TooltipEntry(text));
        return this;
    }

    @Override
    public ITooltip add(TooltipComponent component) {
        entries.add(new TooltipEntry(component));
        return this;
    }

    @Override
    public ITooltip add(Supplier<Component> supplier) {
        entries.add(new SupplierTooltipEntry(supplier));
        return this;
    }

    @Override
    public ITooltip ofAll(ITooltip other) {
        entries.addAll(other.entries());
        return this;
    }


    @Override
    public ITooltip copy() {
        ListTooltip tooltip = new ListTooltip(mousePos, entries);
        tooltip.setContextStack(stack);
        return tooltip;
    }

    @Override
    public ITooltipStack<?> stack() {
        return stack;
    }

    @Override
    public ITooltip setContextStack(@Nullable ITooltipStack<?> stack) {
        this.stack = stack;
        return this;
    }
}
