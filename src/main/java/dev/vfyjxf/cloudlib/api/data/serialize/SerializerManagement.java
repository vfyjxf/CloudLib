package dev.vfyjxf.cloudlib.api.data.serialize;

import dev.vfyjxf.cloudlib.util.Checks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

/**
 * Coordinates {@link Serialize} entries, keyed by stable {@code String} name (not a runtime id).
 * {@link #saveAll} encodes each entry into a {@link CompoundTag}; {@link #loadAll} decodes and
 * silently applies each present entry.
 */
public final class SerializerManagement {

    private final MutableMap<String, Serialize<?>> serializes = Maps.mutable.empty();

    public <T> Serialize<T> register(Serialize<T> serialize) {
        Checks.checkNotNull(serialize, "serialize");
        if (serializes.containsKey(serialize.name())) {
            throw new IllegalArgumentException("Serialize with name '" + serialize.name() + "' already exists");
        }
        serializes.put(serialize.name(), serialize);
        return serialize;
    }

    public boolean hasSerializers() {
        return !serializes.isEmpty();
    }

    public void saveAll(CompoundTag target, HolderLookup.Provider registries) {
        for (Serialize<?> serialize : serializes) {
            serialize.save(registries).result().ifPresent(tag -> target.put(serialize.name(), tag));
        }
    }

    public void loadAll(CompoundTag source, HolderLookup.Provider registries) {
        for (Serialize<?> serialize : serializes) {
            if (source.contains(serialize.name())) {
                applyOne(serialize, source.get(serialize.name()), registries);
            }
        }
    }

    private static <T> void applyOne(Serialize<T> serialize, Tag tag, HolderLookup.Provider registries) {
        serialize.decode(tag, registries).result().ifPresent(serialize::load);
    }
}
