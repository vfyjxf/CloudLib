package dev.vfyjxf.cloudlib.api.network.expose.common;

import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.expose.ValueSupplier;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Function;

public interface LayerExpose<E> extends ExposeCommon {

    static <S_UP, S_EXPORT, S_HOLDER> Expose<S_EXPORT> create(
            Function<S_HOLDER, S_UP> upstreamGetter,
            FlowEncoder<S_UP> encoder,
            FlowEncoder<S_EXPORT> decoder

    ) {
        throw new NotImplementedException();
    }

    //region identity and debug info

    /**
     * @return the debug name of this value
     */
    String name();

    /**
     * The id of this value,it represents the type of this value in the data stream.
     *
     * @return the id of this value
     */
    short id();

    //endregion

    //region snapshot

    @ApiStatus.Internal
    record LayerSnapshot<T>(
            Snapshot<T> snapshot,
            ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder
    ) {}

    @ApiStatus.Internal
    <T> LayerSnapshot<T> layerSnapshot();

    default <T> boolean changed() {
        LayerSnapshot<T> layerSnapshot = layerSnapshot();
        Snapshot<T> snapshot = layerSnapshot.snapshot();
        return switch (Snapshot.currentState(snapshot, layerSnapshot.valueSupplier.get())) {
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
