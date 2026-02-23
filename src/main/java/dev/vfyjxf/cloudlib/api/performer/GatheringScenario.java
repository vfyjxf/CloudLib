package dev.vfyjxf.cloudlib.api.performer;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

//TODO:design a better way to gather return values
public record GatheringScenario<T, R>(ResourceLocation id, Class<T> type, Function<List<R>, List<R>> gather) {

    @Override
    public String toString() {
        return "GatheringScenario{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
