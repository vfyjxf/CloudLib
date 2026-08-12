package dev.vfyjxf.cloudlib.api.data.snapshot;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.handle.Handle;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A value snapshot to observe the value's change.
 *
 * @param <T> the type of the value
 */
public sealed interface Snapshot<T> {

    enum State {
        changed,
        unchanged,
        illegal;

        public boolean changed() {
            return this == changed;
        }

        public boolean unchanged() {
            return this == unchanged;
        }

        public boolean illegal() {
            return this == illegal;
        }
    }

    static <T> Snapshot<T> noneOf() {
        return None.instance();
    }


    static <T> Snapshot<T> copyOf(UnaryOperator<T> copier, CheckStrategy<T> strategy) {
        return new CopyInstance<>(null, copier, strategy);
    }

    static <T> Snapshot<T> readonlyOf(T value) {
        return new Readonly<>(value);
    }

    static <T> Snapshot<T> immutableRefOf(T value, Predicate<T> strategy) {
        return new ImmutableRef<>(value, strategy);
    }

    @SafeVarargs
    static <T extends Observable> Snapshot<T> immutableRefOf(T value, T... typeCatch) {
        Checks.checkNotNull(value, "value");
        Checks.checkArgument(typeCatch.length == 0, "The typeCatch must be empty");
        return new ImmutableRef<>(value, (unused) -> value.changed());
    }

    static <T> MutableRef<T> mutableRefOf(CheckStrategy<T> strategy) {
        return new MutableRef<>(strategy);
    }

    default boolean mutable() {
        return this instanceof MutableRef<T> ||
                this instanceof CopyInstance<T>;
    }

    /**
     * @return the value of the snapshot
     * @throws IllegalStateException if the snapshot is {@link None}
     */
    T readValue() throws IllegalStateException;

    /**
     * @return the value of the snapshot, if the snapshot is {@link None} return null
     */
    @Nullable
    T value();

    State currentState(T current);

    /**
     * @return true if the state is changed, false if the state is unchanged
     */
    boolean updateState(T current);

    /**
     * Force update the state of the snapshot, normally this method used to init mutable snapshot
     *
     * @param current the current value
     */
    default void forceUpdateState(T current) {
        updateState(current);
    }

    static <T> boolean changed(Snapshot<T> instance, T current) {
        return switch (instance.currentState(current)) {
            case changed -> true;
            case unchanged -> false;
            case illegal -> throw new IllegalStateException("The snapshot has been changed illegally");
        };
    }

    /**
     * A snapshot that does not hold any value.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    enum None implements Snapshot {
        instance;

        @SuppressWarnings("unchecked")
        private static <T> Snapshot<T> instance() {
            return (Snapshot<T>) instance;
        }

        @Override
        public String toString() {
            return "None";
        }

        @Override
        public Object readValue() throws IllegalStateException {
            throw new IllegalStateException("Cannot read value in None snapshot");
        }

        @Override
        public @Nullable Object value() {
            return null;
        }

        @Override
        public boolean updateState(Object current) {
            return false;
        }

        @Override
        public State currentState(Object current) {
            return State.unchanged;
        }
    }

    /**
     * snapshot of the readonly value,the value is immutable
     */
    final class Readonly<T> implements Snapshot<T> {
        private final T value;
        private final Predicate<T> strategy;

        public Readonly(T value) {
            this.value = value;
            int hash = Objects.hashCode(value);
            this.strategy = (current -> current == this.value && Objects.hashCode(current) == hash);
        }


        public Predicate<T> strategy() {
            return strategy;

        }

        @Override
        public T readValue() throws IllegalStateException {
            return value;
        }

        @Override
        public @Nullable T value() {
            return value;
        }

        @Override
        public State currentState(T current) {
            boolean changed = value != current || !strategy.test(current);
            if (changed) return State.illegal;
            else return State.unchanged;
        }

        @Override
        public boolean updateState(T current) {
            State state = currentState(current);
            if (state == State.illegal)
                throw new IllegalStateException("The snapshot has been changed illegally");
            else return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Readonly<?> readonly = (Readonly<?>) o;
            return Objects.equals(value, readonly.value) && strategy.equals(readonly.strategy);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(value);
            result = 31 * result + strategy.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Readonly{" +
                    "value=" + value +
                    '}';
        }
    }

    /**
     * the shallow immutable reference,normally the value is a {@link Observable}
     */
    record ImmutableRef<T>(T value, Predicate<T> strategy) implements Snapshot<T> {

        public boolean observable() {
            return value instanceof Observable;
        }

        @Override
        public T readValue() throws IllegalStateException {
            return value;
        }

        @Override
        public T value() {
            return value;
        }

        public Predicate<T> strategy() {
            return strategy;
        }

        @Override
        public State currentState(T current) {
            if (value != current) return State.illegal;
            else return strategy.test(value) ? State.unchanged : State.changed;
        }

        @Override
        public boolean updateState(T current) {
            return switch (currentState(current)) {
                case changed -> true;
                case unchanged -> false;
                case illegal -> throw new IllegalStateException("The snapshot has been changed illegally");
            };
        }

        @Override
        public String toString() {
            return "ImmutableRef{" +
                    "value=" + value +
                    '}';

        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ImmutableRef<?>) obj;
            return Objects.equals(this.value, that.value) &&
                    Objects.equals(this.strategy, that.strategy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, strategy);
        }

    }

    /**
     * snapshot of the shallow mutable reference
     *
     * @param <T> the type of the value
     */
    final class MutableRef<T> implements Snapshot<T> {
        private final CheckStrategy<T> strategy;
        private T value;

        public MutableRef(CheckStrategy<T> strategy) {
            this.strategy = strategy;
        }

        public CheckStrategy<T> strategy() {
            return strategy;
        }

        @Override
        public T readValue() throws IllegalStateException {
            return value;
        }

        public T value() {
            return value;
        }

        @Override
        public State currentState(T current) {
            var changed = !strategy.matches(value, current);
            return changed ? State.changed : State.unchanged;
        }

        @Override
        public boolean updateState(T current) {
            State state = currentState(current);
            if (state.changed()) value = current;
            return state.changed();
        }

        @Override
        public void forceUpdateState(T current) {
            value = current;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MutableRef<?> that = (MutableRef<?>) o;
            return Objects.equals(value, that.value) && strategy.equals(that.strategy);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(value);
            result = 31 * result + strategy.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MutableRef{" +
                    "value=" + value +
                    '}';
        }
    }

    /**
     * snapshot based on copying the value to observe the change
     *
     * @param <T> the type of the value
     */
    final class CopyInstance<T> implements Snapshot<T> {
        private final UnaryOperator<T> copier;
        private final CheckStrategy<T> strategy;
        private T value;

        public CopyInstance(T initialValue, UnaryOperator<T> copier, CheckStrategy<T> strategy) {
            this.copier = copier;
            this.strategy = strategy;
            this.value = copier.apply(initialValue);
        }

        public UnaryOperator<T> copier() {
            return copier;
        }

        public CheckStrategy<T> strategy() {
            return strategy;
        }

        @Override
        public T readValue() throws IllegalStateException {
            return value;
        }

        @Override
        public T value() {
            return value;
        }

        private void set(T value) {
            this.value = copier.apply(value);
        }

        @Override
        public State currentState(T current) {
            var changed = !strategy.matches(value, current);
            return changed ? State.changed : State.unchanged;
        }

        @Override
        public boolean updateState(T current) {
            State state = currentState(current);
            if (state.changed()) set(current);
            return state.changed();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CopyInstance<?> that = (CopyInstance<?>) o;
            return copier.equals(that.copier) && strategy.equals(that.strategy) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = copier.hashCode();
            result = 31 * result + strategy.hashCode();
            result = 31 * result + Objects.hashCode(value);
            return result;
        }

        @Override
        public String toString() {
            return "CopyInstance{" +
                    ", value=" + value +
                    '}';
        }
    }

    /**
     * Internal adapter letting a {@link Handle} masquerade as a {@link Snapshot} so it can drive the
     * existing Expose machinery. The handle's dirty flag is the change signal; value comparison
     * happens at {@link Handle#set} time, so this is a pure dirty-flag reader/clearer.
     */
    @ApiStatus.Internal
    final class HandleSnapshot<T> implements Snapshot<T> {

        private final Handle<T> handle;

        private HandleSnapshot(Handle<T> handle) {
            this.handle = handle;
        }

        public static <T> HandleSnapshot<T> of(Handle<T> handle) {
            return new HandleSnapshot<>(handle);
        }

        @Override
        public T readValue() {
            return handle.get();
        }

        @Override
        public @Nullable T value() {
            return handle.get();
        }

        @Override
        public State currentState(T current) {
            //handle.changed() is the change signal: for a plain Handle it is dirty(); for a DiffHandle
            //it is dirty() || get().changed(), so in-place mutation of a DiffObservable value surfaces too.
            //`current` is handle.get() and is intentionally ignored.
            return handle.changed() ? State.changed : State.unchanged;
        }

        @Override
        public boolean updateState(T current) {
            boolean changed = handle.changed();
            handle.clearDirty();
            return changed;
        }

        @Override
        public void forceUpdateState(T current) {
            handle.clearDirty();
        }

        @Override
        public boolean mutable() {
            return true;
        }

        @Override
        public String toString() {
            return "HandleSnapshot{" + handle + '}';
        }
    }

}
