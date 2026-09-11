package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Push-based reactive value cell: the single source of truth for a piece of state.
 * {@link #set} compares (via a {@link CheckStrategy}) and, on change, stores, marks dirty and fires
 * listeners immediately — change detection at write time, not on every read.
 * <p>
 * Three write modes: {@link #set} (server mutate: store + dirty + fire), {@link #apply} (client
 * receive: store + fire, no dirty), {@link #load} (NBT: store only). The dirty flag is consumed by
 * tick-based observers (e.g. Expose sync via the internal {@code HandleSnapshot} adapter).
 *
 * @param <T> the value type
 */
public interface Handle<T> extends ReadOnlyHandle<T> {

    static <T> Handle<T> of(@Nullable T initial, CheckStrategy<T> strategy) {
        Checks.checkNotNull(strategy, "strategy");
        return new BasicHandle<>(initial, strategy);
    }

    static <T> Handle<T> of(@Nullable T initial) {
        return new BasicHandle<>(initial, CheckStrategy.equals());
    }

    static <T> Handle<T> of() {
        return new BasicHandle<>(null, CheckStrategy.equals());
    }

    /** Store, mark dirty, fire listeners; no-op if unchanged. Authoritative (server-side) mutation. */
    void set(T value);

    /** Store and fire listeners without marking dirty. For applying a value received from the server. */
    void apply(T value);

    /** Store only — no dirty change, no listeners. For silent NBT/disk initialization. */
    void load(T value);

    @Contract(pure = true)
    boolean dirty();

    void clearDirty();

    @Override
    @Contract(pure = true)
    default boolean changed() {
        return dirty();
    }

    @Contract(pure = true)
    CheckStrategy<T> strategy();

    @Contract(pure = true)
    default ReadOnlyHandle<T> readOnly() {
        return this;
    }
}
