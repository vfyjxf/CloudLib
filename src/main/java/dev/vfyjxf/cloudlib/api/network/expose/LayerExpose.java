package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

public non-sealed interface LayerExpose<E> extends ExposeCommon {

    static <T, E> LayerExpose<E> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowHandler<T, E> codec
    ) {
        return new StandardLayerExpose<>(
                name,
                id,
                snapshot,
                valueSupplier,
                codec,
                codec
        );
    }

    static <T, E> LayerExpose<E> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder
    ) {
        return new StandardLayerExpose<>(
                name,
                id,
                snapshot,
                valueSupplier,
                encoder,
                decoder
        );
    }

    //region identity and debug info

    /**
     * @return the debug name of this value
     */
    @Override
    String name();

    /**
     * The id of this value,it represents the type of this value in the data stream.
     *
     * @return the id of this value
     */
    @Override
    short id();

    //endregion

    //region snapshot

    @ApiStatus.Internal
    Snapshot<?> snapshot();

    @Override
    <T> boolean changed();

    //endregion

    //region for client

    void whenReceive(Consumer<E> consumer);

    //endregion

}
