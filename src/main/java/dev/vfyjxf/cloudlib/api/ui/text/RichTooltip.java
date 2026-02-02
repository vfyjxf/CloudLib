package dev.vfyjxf.cloudlib.api.ui.text;

import com.mojang.datafixers.util.Either;
import dev.vfyjxf.cloudlib.api.ui.widget.TooltipStack;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

//TODO:redesign tooltip and rich text
public sealed interface RichTooltip permits EmptyRichTooltip, ListRichTooltip {

    //region factory

    static RichTooltip empty() {
        return EmptyRichTooltip.empty;
    }

    static RichTooltip create() {
        return new ListRichTooltip();
    }

    static RichTooltip from(Component... texts) {
        var richTooltip = new ListRichTooltip();
        for (var text : texts) {
            richTooltip.add(text);
        }
        return richTooltip;
    }

    static RichTooltip fromTexts(Iterable<? extends Component> texts) {
        var richTooltip = new ListRichTooltip();
        for (var text : texts) {
            richTooltip.add(new TooltipEntry.TextEntry(text));
        }
        return richTooltip;
    }

    static RichTooltip from(TooltipComponent... components) {
        var richTooltip = new ListRichTooltip();
        for (var component : components) {
            richTooltip.add(component);
        }
        return richTooltip;
    }

    static RichTooltip fromComponents(Iterable<? extends TooltipComponent> components) {
        var richTooltip = new ListRichTooltip();
        for (var component : components) {
            richTooltip.add(component);
        }
        return richTooltip;
    }

    static RichTooltip from(TooltipEntry... entries) {
        var richTooltip = new ListRichTooltip();
        for (var entry : entries) {
            richTooltip.add(entry);
        }
        return richTooltip;
    }

    static RichTooltip fromEntries(Iterable<? extends TooltipEntry> entries) {
        var richTooltip = new ListRichTooltip();
        for (var entry : entries) {
            richTooltip.add(entry);
        }
        return richTooltip;
    }

    //endregion

    //region build

    RichTooltip add(TooltipEntry entry);

    RichTooltip add(int index, TooltipEntry entry);

    RichTooltip add(Component text);

    RichTooltip add(TooltipComponent component);

    RichTooltip add(Supplier<Component> supplier);

    default RichTooltip addAll(Component... texts) {
        for (Component text : texts) add(text);
        return this;
    }

    default RichTooltip addAllTexts(Iterable<? extends Component> texts) {
        for (Component text : texts) add(text);
        return this;
    }

    default RichTooltip addAll(TooltipComponent... components) {
        for (TooltipComponent component : components) add(component);
        return this;
    }

    default RichTooltip addAllComponents(Iterable<? extends TooltipComponent> components) {
        for (TooltipComponent component : components) add(component);
        return this;
    }

    default RichTooltip addAll(TooltipEntry... entries) {
        for (TooltipEntry entry : entries) add(entry);
        return this;
    }

    default RichTooltip addAllEntries(Iterable<? extends TooltipEntry> entries) {
        for (TooltipEntry entry : entries) add(entry);
        return this;
    }

    RichTooltip addAll(RichTooltip other);

    //endregion

    //region context

    MutableList<TooltipEntry> entries();

    @Nullable
    TooltipStack<?> stack();

    @Contract("_ -> this")
    RichTooltip setContextStack(@Nullable TooltipStack<?> stack);

    //endregion

    //region util

    RichTooltip copy();

    default MutableList<Either<FormattedText, TooltipComponent>> toVanilla() {
        MutableList<Either<FormattedText, TooltipComponent>> result = MutableLists.empty();
        for (var entry : entries()) {
            result.add(
                switch (entry) {
                    case TooltipEntry.TextEntry(var text) -> Either.left(text);
                    case TooltipEntry.TextProvider(var provider) -> Either.left(provider.get());
                    case TooltipEntry.ComponentEntry(var component) -> Either.right(component);
                }
            );
        }
        return result;
    }

    boolean isEmpty();

    //endregion

}