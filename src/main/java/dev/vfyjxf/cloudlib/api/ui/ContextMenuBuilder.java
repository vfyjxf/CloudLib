package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import net.minecraft.network.chat.Component;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ContextMenuBuilder {

    public static ContextMenuBuilder builder() {
        return new ContextMenuBuilder();
    }

    private final MutableList<EntryBuilder> menuEntries = Lists.mutable.empty();

    private ContextMenuBuilder() {
    }

    @Contract("_,_-> this")
    public ContextMenuBuilder single(Component text, InputEvent.OnMouseClicked onMouseClicked) {
        return this;
    }

    @Contract("_,_-> this")
    public ContextMenuBuilder single(LangEntry langEntry, InputEvent.OnMouseClicked onMouseClicked) {
        return single(langEntry.get(), onMouseClicked);
    }

    @Contract("_,_-> this")
    public ContextMenuBuilder group(Component text, Consumer<EntryBuilder> builder) {
        return this;
    }

    @Contract("_,_-> this")
    public ContextMenuBuilder group(LangEntry langEntry, Consumer<EntryBuilder> builder) {
        return group(langEntry.get(), builder);
    }

    @ApiStatus.Internal
    public boolean isEmpty() {
        return false;
    }

    public static class EntryBuilder {
        private Component text;
        private @Nullable InputEvent.OnMouseClicked onMouseClicked;
        private MutableList<EntryBuilder> children = Lists.mutable.empty();


        protected EntryBuilder(Component text, @Nullable InputEvent.OnMouseClicked onMouseClicked) {
            this.text = text;
            this.onMouseClicked = onMouseClicked;
        }

        @Contract("_,_-> this")
        public EntryBuilder single(Component text, InputEvent.OnMouseClicked onMouseClicked) {
            return this;
        }

        @Contract("_,_-> this")
        public EntryBuilder single(LangEntry langEntry, InputEvent.OnMouseClicked onMouseClicked) {
            return single(langEntry.get(), onMouseClicked);
        }

        @Contract("_,_-> this")
        public EntryBuilder group(Component text, Consumer<EntryBuilder> builder) {
            return this;
        }

        @Contract("_,_-> this")
        public EntryBuilder group(LangEntry langEntry, Consumer<EntryBuilder> builder) {
            return group(langEntry.get(), builder);
        }

    }

}
