package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
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
    record LayerSnapshot<T>(
            Snapshot<T> snapshot,
            ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder
    ) {
        public T current() {
            return valueSupplier.get();
        }
    }

    @ApiStatus.Internal
    <T> LayerSnapshot<T> layerSnapshot();

    @Override
    default <T> boolean changed() {
        LayerSnapshot<T> layerSnapshot = layerSnapshot();
        Snapshot<T> snapshot = layerSnapshot.snapshot();
        return switch (Snapshot.currentState(snapshot, layerSnapshot.current())) {
            case UNCHANGED -> false;
            case CHANGED -> true;
            case ILLEGAL -> {
                boolean readonly = snapshot instanceof Snapshot.Readonly;
                throw new IllegalStateException("Illegally modifity a " + (readonly ? "readonly" : "immutable reference") + " snapshot(id:" + id() + " name:" + name() + ")");
            }
        };
    }

    //endregion

    //region for client

    void whenReceive(Consumer<E> consumer);

    //endregion

}
