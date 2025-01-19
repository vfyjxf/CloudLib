package dev.vfyjxf.cloudlib.api.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.Tag;

import java.util.function.Function;

/**
 * The unique key for a attachable data
 *
 * @param <T> The type of the data
 */
public interface DataKey<T> {

    static DataKey<Integer> ofInt(String key, int defaultValue) {
        return new BuiltinDataImpl.IntDataKey(key, defaultValue);
    }

    static <T extends Enum<T>> DataKey<T> ofEnum(String key, T defaultValue, Class<T> enumClass) {
        return new BuiltinDataImpl.EnumDataKey<>(key, defaultValue, enumClass);
    }

    static DataKey<String> ofString(String key, String defaultValue) {
        return new BuiltinDataImpl.StringDataKey(key, defaultValue);
    }

    static <T> DataKey<T> o(String key, Function<DataAttachable, T> defaultValueProvider, Codec<T> codec) {
        return new BuiltinDataImpl.DataKeyImpl<>(key, defaultValueProvider, codec);
    }

    String key();

    T defaultValue(DataAttachable holder);

    Tag save(T value);

    T load(Tag data);

}
