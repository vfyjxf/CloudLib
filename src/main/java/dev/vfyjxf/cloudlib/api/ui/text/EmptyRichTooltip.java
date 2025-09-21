package dev.vfyjxf.cloudlib.api.ui.text;

import dev.vfyjxf.cloudlib.api.ui.widget.TooltipStack;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

final class EmptyRichTooltip implements RichTooltip {

    static final RichTooltip empty = new EmptyRichTooltip();

    @Override
    public RichTooltip add(TooltipEntry entry) {
        throw new UnsupportedOperationException("This operation is not supported for EmptyRichTooltip");
    }

    @Override
    public RichTooltip add(int index, TooltipEntry entry) {
        throw new UnsupportedOperationException("This operation is not supported for EmptyRichTooltip");
    }

    @Override
    public RichTooltip add(Component text) {
        throw new UnsupportedOperationException("This operation is not supported for EmptyRichTooltip");
    }

    @Override
    public RichTooltip add(TooltipComponent component) {
        throw new UnsupportedOperationException("This operation is not supported for EmptyRichTooltip");
    }

    @Override
    public RichTooltip add(Supplier<Component> supplier) {
        throw new UnsupportedOperationException("This operation is not supported for EmptyRichTooltip");
    }

    @Override
    public RichTooltip addAll(RichTooltip other) {
        throw new UnsupportedOperationException("This operation is not supported for EmptyRichTooltip");
    }

    @Override
    public MutableList<TooltipEntry> entries() {
        return MutableLists.empty();
    }

    @Override
    public @Nullable TooltipStack<?> stack() {
        return null;
    }

    @Override
    public RichTooltip setContextStack(@Nullable TooltipStack<?> stack) {
        return null;
    }

    @Override
    public RichTooltip copy() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
