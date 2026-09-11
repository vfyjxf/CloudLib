package dev.vfyjxf.cloudlib.api.data.serialize;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default {@link Serialize} implementation.
 */
@ApiStatus.Internal
final class BasicSerialize<T> implements Serialize<T> {

    private final String name;
    private final Codec<T> codec;
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    BasicSerialize(String name, Codec<T> codec, Supplier<T> getter, Consumer<T> setter) {
        this.name = Checks.checkNotNull(name, "name");
        this.codec = Checks.checkNotNull(codec, "codec");
        this.getter = Checks.checkNotNull(getter, "getter");
        this.setter = Checks.checkNotNull(setter, "setter");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @Contract(pure = true)
    public Codec<T> codec() {
        return codec;
    }

    @Override
    @Contract(pure = true)
    public T current() {
        return getter.get();
    }

    @Override
    public void load(T value) {
        setter.accept(value);
    }

    @Override
    public String toString() {
        return "Serialize{name='" + name + "', codec=" + codec + '}';
    }
}
