package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Supplier;

public record LangEntry(String key, String value) {

    public MutableComponent get() {
        return Component.translatable(this.key);
    }

    public MutableComponent get(Object... args) {
        return Component.translatable(this.key, args);
    }

    public Supplier<Component> supplier(Object... args) {
        return () -> Component.translatable(this.key, args);
    }

}
