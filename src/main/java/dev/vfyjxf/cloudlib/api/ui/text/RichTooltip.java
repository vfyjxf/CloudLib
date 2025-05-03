package dev.vfyjxf.cloudlib.api.ui.text;

import com.mojang.datafixers.util.Either;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.Tooltip;
import dev.vfyjxf.cloudlib.api.ui.widget.TooltipStack;
import dev.vfyjxf.cloudlib.ui.ListRichTooltip;
import dev.vfyjxf.cloudlib.ui.TooltipEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

//TODO:decide should we add scroll tooltip like rich tooltip in black myth wukong
//TODO:Refactor:move to RichText
//FIXME:
public interface RichTooltip {

    static Tooltip of(Component text) {
        return new TooltipEntry(text);
    }

    static Tooltip of(TooltipComponent component) {
        return new TooltipEntry(component);
    }

    static RichTooltip create(@Nullable Pos mousePos, List<Component> texts) {
        return from(mousePos, texts.stream().map(RichTooltip::of).toList());
    }

    static RichTooltip create(@Nullable Pos mousePos, Component... texts) {
        return create(mousePos, List.of(texts));
    }

    static RichTooltip create(List<Component> texts) {
        return create(null, texts);
    }

    static RichTooltip create(Component... texts) {
        return create(List.of(texts));
    }

    static RichTooltip from(@Nullable Pos mousePos, List<Tooltip> tooltips) {
        return new ListRichTooltip(mousePos, tooltips);
    }

    static RichTooltip from(@Nullable Pos mousePos, Tooltip... tooltips) {
        return from(mousePos, List.of(tooltips));
    }

    static RichTooltip from(List<Tooltip> tooltips) {
        return from(null, tooltips);
    }

    static RichTooltip from(Tooltip... tooltips) {
        return from(List.of(tooltips));
    }

    int getPosX();

    int getPosY();

    MutableList<Tooltip> tooltips();

    default MutableList<Either<FormattedText, TooltipComponent>> guiTooltips() {
        MutableList<Either<FormattedText, TooltipComponent>> result = Lists.mutable.empty();
        for (Tooltip tooltip : tooltips()) {
            if (tooltip.isText()) result.add(Either.left(tooltip.asText()));
            else result.add(Either.right(tooltip.asComponent()));
        }
        return result;
    }

    @Contract("_ -> this")
    RichTooltip tooltip(Tooltip tooltip);

    @Contract("_ -> this")
    default RichTooltip tooltip(Collection<Tooltip> tooltips) {
        for (Tooltip tooltip : tooltips) {
            tooltip(tooltip);
        }
        return this;
    }

    default RichTooltip tooltip(Tooltip... tooltips) {
        for (Tooltip tooltip : tooltips) {
            tooltip(tooltip);
        }
        return this;
    }

    RichTooltip add(Component text);

    RichTooltip add(TooltipComponent component);

    RichTooltip add(Supplier<Component> supplier);

    default RichTooltip addAll(TooltipComponent... components) {
        for (TooltipComponent component : components) {
            add(component);
        }
        return this;
    }

    default RichTooltip addAll(Component... text) {
        for (Component component : text) {
            add(component);
        }
        return this;
    }

    default RichTooltip addAllTooltipComponents(Iterable<TooltipComponent> text) {
        for (TooltipComponent component : text) {
            add(component);
        }
        return this;
    }

    default RichTooltip addAllTexts(Iterable<Component> text) {
        for (Component component : text) {
            add(component);
        }
        return this;
    }

    RichTooltip ofAll(RichTooltip other);

    RichTooltip copy();

    @Nullable
    TooltipStack<?> stack();

    @Contract("_ -> this")
    RichTooltip setContextStack(@Nullable TooltipStack<?> stack);

    MutableList<Either<FormattedText, TooltipComponent>> toVanilla();

    boolean isEmpty();

}
