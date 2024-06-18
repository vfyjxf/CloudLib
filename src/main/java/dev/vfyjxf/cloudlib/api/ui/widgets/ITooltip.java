package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.widgets.ListTooltip;
import dev.vfyjxf.cloudlib.ui.widgets.TooltipEntry;
import dev.vfyjxf.cloudlib.utils.CollectionUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public interface ITooltip {

    static ITooltip.Entry entry(Component text) {
        return new TooltipEntry(text);
    }

    static ITooltip.Entry entry(TooltipComponent text) {
        return new TooltipEntry(text);
    }

    static ITooltip create(@Nullable Point mousePos, Collection<Component> texts) {
        return from(mousePos, CollectionUtils.map(texts, ITooltip::entry));
    }

    static ITooltip create(@Nullable Point mousePos, Component... texts) {
        return create(mousePos, List.of(texts));
    }

    static ITooltip create(Collection<Component> texts) {
        return create(null, texts);
    }

    static ITooltip create(Component... texts) {
        return create(List.of(texts));
    }

    static ITooltip from(@Nullable Point mousePos, Collection<Entry> entries) {
        return new ListTooltip(mousePos, entries);
    }

    static ITooltip from(@Nullable Point mousePos, Entry... entries) {
        return from(mousePos, List.of(entries));
    }

    static ITooltip from(Collection<Entry> entries) {
        return from(null, entries);
    }

    static ITooltip from(Entry... entries) {
        return from(List.of(entries));
    }

    int getX();

    int getY();

    List<Entry> entries();

    ITooltip add(Component text);

    ITooltip add(TooltipComponent component);

    ITooltip add(Supplier<Component> supplier);

    default ITooltip addAll(TooltipComponent... components) {
        for (TooltipComponent component : components) {
            add(component);
        }
        return this;
    }

    default ITooltip addAll(Component... text) {
        for (Component component : text) {
            add(component);
        }
        return this;
    }

    default ITooltip addAllTooltipComponents(Iterable<TooltipComponent> text) {
        for (TooltipComponent component : text) {
            add(component);
        }
        return this;
    }

    default ITooltip addAllTexts(Iterable<Component> text) {
        for (Component component : text) {
            add(component);
        }
        return this;
    }

    ITooltip ofAll(ITooltip other);

    ITooltip copy();

    @Nullable
    ITooltipStack<?> stack();

    ITooltip setContextStack(@Nullable ITooltipStack<?> stack);

    interface Entry {
        boolean isText();

        boolean isComponent();

        Component asText();

        TooltipComponent asComponent();
    }


}
