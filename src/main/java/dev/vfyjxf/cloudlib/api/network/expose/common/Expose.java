package dev.vfyjxf.cloudlib.api.network.expose.common;

import dev.vfyjxf.cloudlib.api.network.expose.ValueSupplier;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

/**
 * @param <T> the type of the exposed value
 */
public interface Expose<T> extends ExposeCommon{

    //region factory

    static <T> Expose<T> create(String name, short id, Snapshot<T> snapshot, ValueSupplier<T> valueSupplier) {
        return new CommonExpose<>(name, id, snapshot, valueSupplier);

    }

    //endregion

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

    /**
     * @return the snapshot of this expose
     */
    @Contract(pure = true)
    Snapshot<T> snapshot();

    default @UnknownNullability T previous() {
        return Snapshot.getValue(snapshot());
    }

    default boolean changed() {
        var snapshot = snapshot();
        return switch (Snapshot.currentState(snapshot, current())) {
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

    @ApiStatus.Internal
    ValueSupplier<T> supplier();

    //endregion

    //region client usage

    void whenReceive(Consumer<T> consumer);


    //endregion
}
