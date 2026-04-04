package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public record LangEntry(String key, @Nullable String value) {

    public LangEntry {
        Objects.requireNonNull(key, "key");
    }

    public static LangEntry reference(String key) {
        return new LangEntry(key, null);
    }

    public boolean hasDefaultValue() {
        return value != null;
    }

    public MutableComponent get() {
        return component();
    }

    public MutableComponent get(Object... args) {
        return component(args);
    }

    public MutableComponent component(Object... args) {
        return Component.translatable(this.key, args);
    }

    public MutableComponent colored(int color, Object... args) {
        return component(args).withColor(color);
    }

    public String string(Object... args) {
        return component(args).getString();
    }

    public Supplier<Component> supplier(Object... args) {
        return () -> component(args);
    }

    public LangEntry suffix(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return this;
        }
        return reference(this.key + "." + suffix);
    }

}
