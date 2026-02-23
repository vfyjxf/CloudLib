package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

/**
 * @param <T> the type of the exposed value
 */
public non-sealed interface Expose<T> extends ExposeCommon {

    //region factory

    static <T> Expose<T> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            UnaryFlowHandler<T> exposeCodec
    ) {
        return new StandardExpose<>(
                name,
                id,
                snapshot,
                valueSupplier,
                exposeCodec,
                exposeCodec
        );
    }

    static <T> Expose<T> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<T> decoder
    ) {
        return new StandardExpose<>(
                name,
                id,
                snapshot,
                valueSupplier,
                encoder,
                decoder
        );
    }

    //endregion

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

    /**
     * @return the snapshot of this expose
     */
    @Contract(pure = true)
    Snapshot<T> snapshot();


    /**
     * @return the previous value of this Expose
     * @throws IllegalStateException if the snapshot is {@link dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot.None}
     */
    default @UnknownNullability T previous() throws IllegalStateException {
        return snapshot().readValue();
    }

    @Override
    default boolean changed() {
        var snapshot = snapshot();
        return switch (snapshot.currentState(current())) {
            case UNCHANGED -> false;
            case CHANGED -> true;
            case ILLEGAL -> {
                boolean readonly = snapshot instanceof Snapshot.Readonly;
                throw new IllegalStateException("Illegally modifity a " + (readonly ? "readonly" : "immutable reference") + " snapshot(id:" + id() + " name:" + name() + ")");
            }
        };
    }

    /**
     * @return the current value of this Expose
     */
    @Contract(pure = true)
    T current();

    //endregion

    //region client usage

    /**
     * register a consumer to be called when the value is received from the server.
     *
     * @param consumer the consumer to be called when the value is received
     */
    void whenReceive(Consumer<T> consumer);


    //endregion
}
