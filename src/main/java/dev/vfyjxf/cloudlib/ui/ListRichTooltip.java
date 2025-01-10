package dev.vfyjxf.cloudlib.ui;

import com.mojang.datafixers.util.Either;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.Tooltip;
import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.TooltipStack;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

public class ListRichTooltip implements RichTooltip {

    private Pos mousePos;
    private final MutableList<Tooltip> tooltips;
    private TooltipStack<?> stack = null;

    public ListRichTooltip(@Nullable Pos mousePos, Collection<? extends Tooltip> entries) {
        this.mousePos = mousePos;
        if (this.mousePos == null) {
            this.mousePos = ScreenUtil.ofMouse();
        }
        this.tooltips = Lists.mutable.ofAll(entries);
    }


    @Override
    public int getPosX() {
        return mousePos.x;
    }

    @Override
    public int getPosY() {
        return mousePos.y;
    }

    @Override
    public MutableList<Tooltip> tooltips() {
        return tooltips;
    }

    @Override
    public RichTooltip tooltip(Tooltip tooltip) {
        tooltips.add(tooltip);
        return this;
    }

    @Override
    public RichTooltip tooltip(Collection<Tooltip> tooltips) {
        this.tooltips.addAll(tooltips);
        return this;
    }

    @Override
    public RichTooltip add(Component text) {
        tooltips.add(new TooltipEntry(text));
        return this;
    }

    @Override
    public RichTooltip add(TooltipComponent component) {
        tooltips.add(new TooltipEntry(component));
        return this;
    }

    @Override
    public RichTooltip add(Supplier<Component> supplier) {
        tooltips.add(new SupplierTooltipEntry(supplier));
        return this;
    }

    @Override
    public RichTooltip ofAll(RichTooltip other) {
        tooltips.addAll(other.tooltips());
        return this;
    }


    @Override
    public RichTooltip copy() {
        ListRichTooltip tooltip = new ListRichTooltip(mousePos, tooltips);
        tooltip.setContextStack(stack);
        return tooltip;
    }

    @Override
    public TooltipStack<?> stack() {
        return stack;
    }

    @Override
    public RichTooltip setContextStack(@Nullable TooltipStack<?> stack) {
        this.stack = stack;
        return this;
    }

    @Override
    public MutableList<Either<FormattedText, TooltipComponent>> toVanilla() {
        return tooltips.collect(tooltip -> {
            if (tooltip.isComponent()) {
                return Either.right(tooltip.asComponent());
            } else {
                return Either.left(tooltip.asText());
            }
        });

    }

    @Override
    public boolean isEmpty() {
        return tooltips.isEmpty();
    }
}
