package dev.vfyjxf.cloudlib.api.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

record BuiltinDataType<T>(
        ResourceLocation id,
        Function<@Nullable DataAttachable, T> defaultValueFunction
) implements DataType<T> {
    @Override
    public T defaultValue(DataAttachable holder) {return defaultValueFunction.apply(holder);}
}
