package dev.vfyjxf.cloudlib.api.data;

import net.minecraft.nbt.CompoundTag;

/**
 * The unique key for a attachable data
 *
 * @param <T> The type of the data
 */
public interface DataKey<T> {

    String key();

    T defaultValue();

    void save(T value, CompoundTag data);

    T load(CompoundTag data);

}
