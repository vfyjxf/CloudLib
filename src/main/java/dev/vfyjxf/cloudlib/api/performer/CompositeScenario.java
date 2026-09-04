package dev.vfyjxf.cloudlib.api.performer;

import dev.vfyjxf.cloudlib.api.util.Namespace;

import java.util.SequencedCollection;
import java.util.function.Function;

public record CompositeScenario<T>(Namespace id, Class<T> type, Function<SequencedCollection<T>, T> merger) {

    @Override
    public String toString() {
        return "CompositeScenario{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
