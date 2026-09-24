package dev.vfyjxf.cloudlib.api.data.serialize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.util.Checks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A named, {@link Codec}-driven serialization entry registered with {@link SerializerManagement}.
 * An external observer of a value: {@link #current()} reads it, {@link #load(Object)} writes a
 * decoded value silently. Prefer {@link #create(String, Handle, Codec)} so load is silent.
 * <p>
 * An entry whose cell holds no value yet reads null from {@link #current()} and is skipped by
 * {@link #save(HolderLookup.Provider)}.
 *
 * @param <T> the value type
 */
public interface Serialize<T> {

    static <T> Serialize<T> create(String name, Handle<T> handle, Codec<T> codec) {
        Checks.checkNotNull(handle, "handle");
        return create(name, handle::get, handle::load, codec);
    }

    static <T> Serialize<T> create(String name, Supplier<@Nullable T> getter, Consumer<T> setter, Codec<T> codec) {
        Checks.checkNotNull(name, "name");
        Checks.checkNotNull(getter, "getter");
        Checks.checkNotNull(setter, "setter");
        Checks.checkNotNull(codec, "codec");
        return new BasicSerialize<>(name, codec, getter, setter);
    }

    String name();

    Codec<T> codec();

    /**
     * @return the value to save, or null while the cell holds no value
     */
    @Nullable
    T current();

    /** Silently apply a decoded value (no listeners, no dirty). */
    void load(T value);

    /**
     * @return the encoded value, or an error carrying this entry's name if there is no value to save
     */
    default DataResult<Tag> save(HolderLookup.Provider registries) {
        @Nullable
        T value = current();
        if (value == null) {
            return DataResult.error(() -> "No value to save for serialize '" + name() + "'");
        }
        return codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value);
    }

    default DataResult<T> decode(Tag tag, HolderLookup.Provider registries) {
        return codec().parse(registries.createSerializationContext(NbtOps.INSTANCE), tag);
    }
}
