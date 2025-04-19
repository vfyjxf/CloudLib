package dev.vfyjxf.cloudlib.api.performer;

import net.minecraft.resources.ResourceLocation;

import java.util.SequencedCollection;
import java.util.function.Function;

public record CompositeScenario<T>(ResourceLocation id, Class<T> type, Function<SequencedCollection<T>, T> merger) {

    @Override
    public String toString() {
        return "CompositeScenario{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
