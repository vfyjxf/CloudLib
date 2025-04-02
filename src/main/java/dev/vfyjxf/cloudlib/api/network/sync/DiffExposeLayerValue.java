package dev.vfyjxf.cloudlib.api.network.sync;

import java.util.function.Consumer;

public interface DiffExposeLayerValue<EXPORT> extends ExposeLayerValue<EXPORT> {
    void consumeDiff(Consumer<EXPORT> diffConsumer);
}
