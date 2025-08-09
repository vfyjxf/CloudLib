package dev.vfyjxf.cloudlib.api.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * The unique key for an attachable data.
 * <p>
 * An in memory data storage system for an attachable object.
 *
 * @param <T> The type of the data
 */
public sealed interface DataType<T> permits BuiltinDataType {

    static <T> DataType<T> valueOf(ResourceLocation id, T defaultValue) {
        return new BuiltinDataType<>(id, holder -> defaultValue);
    }

    static <T> DataType<T> valueOf(ResourceLocation id, Function<@Nullable DataAttachable, T> defaultValueFunction) {
        return new BuiltinDataType<>(id, defaultValueFunction);
    }

    ResourceLocation id();

    T defaultValue(@Nullable DataAttachable holder);

}
