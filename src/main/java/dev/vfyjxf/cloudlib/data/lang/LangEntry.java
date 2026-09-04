package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A typed reference to a translation key. Entries are pure references: the actual texts
 * live in the distributed yaml lang files (hand-written under {@code src/main/lang} or
 * produced by {@link DistributedLangProvider} under {@code src/generated/lang}) and are
 * packed into vanilla json lang files at build time.
 *
 * <p>Constants of this type are exposed by the generated key class (see the cloudLang
 * gradle plugin), never as raw strings.
 */
public record LangEntry(String key) {

    public LangEntry {
        Objects.requireNonNull(key, "key");
    }

    public static LangEntry of(String key) {
        return new LangEntry(key);
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
        return of(this.key + "." + suffix);
    }

}
