package dev.vfyjxf.cloudlib.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.Arrays;

public final class Codecs {
    private Codecs() {}

    public static <E extends Enum<E>> Codec<E> lowerCaseEnum(Class<E> type) {
        return Codec.STRING.comapFlatMap(
                name -> Arrays.stream(type.getEnumConstants())
                              .filter(value -> value.name().equals(name))
                              .findFirst()
                              .map(DataResult::success)
                              .orElseGet(() -> DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name)),
                Enum::name
        );
    }

    public static <T> Codec<ImmutableList<T>> immutableList(Codec<T> elementCodec) {
        return elementCodec.listOf().xmap(
                Lists.immutable::ofAll,
                list -> list.stream().toList()
        );
    }
}
