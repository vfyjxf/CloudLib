package dev.vfyjxf.cloudlib.api.actor;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

public record MergeableActorKey<T>(ResourceLocation id, Class<T> type, Function<List<T>, T> merger) {

    @Override
    public String toString() {
        return "MergeableActorKey{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
