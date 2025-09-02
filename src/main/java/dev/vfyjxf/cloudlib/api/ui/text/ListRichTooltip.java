package dev.vfyjxf.cloudlib.api.ui.text;

import dev.vfyjxf.cloudlib.api.ui.widget.TooltipStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

final class ListRichTooltip implements RichTooltip {

    private final MutableList<TooltipEntry> entries;
    private TooltipStack<?> stack = null;


    ListRichTooltip() {
        this.entries = Lists.mutable.empty();
    }

    ListRichTooltip(MutableList<TooltipEntry> entries) {
        this.entries = entries;
    }

    //region build

    @Override
    public RichTooltip add(TooltipEntry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public RichTooltip add(int index, TooltipEntry entry) {
        entries.add(index, entry);
        return this;
    }

    @Override
    public RichTooltip add(Component text) {
        entries.add(new TooltipEntry.TextEntry(text));
        return this;
    }

    @Override
    public RichTooltip add(TooltipComponent component) {
        entries.add(new TooltipEntry.ComponentEntry(component));
        return this;
    }

    @Override
    public RichTooltip add(Supplier<Component> supplier) {
        entries.add(new TooltipEntry.TextProvider(supplier));
        return this;
    }

    @Override
    public RichTooltip addAll(RichTooltip other) {
        ListRichTooltip toCopy = (ListRichTooltip) other;
        entries.addAll(toCopy.entries);
        return this;
    }

    //endregion

    //region context

    @Override
    public MutableList<TooltipEntry> entries() {
        return entries;
    }

    @Override
    public @Nullable TooltipStack<?> stack() {
        return stack;
    }

    @Override
    public RichTooltip setContextStack(@Nullable TooltipStack<?> stack) {
        this.stack = stack;
        return this;
    }

    //endregion

    //region util

    @Override
    public RichTooltip copy() {
        return new ListRichTooltip(Lists.mutable.ofAll(entries));
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    //endregion
}
