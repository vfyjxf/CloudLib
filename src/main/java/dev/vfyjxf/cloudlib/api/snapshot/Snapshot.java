package dev.vfyjxf.cloudlib.api.snapshot;

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
        CHANGED,
        UNCHANGED,
        ILLEGAL
    }

    static <T> Snapshot<T> noneOf() {
        return None.instance();
    }

    static <T> Snapshot<T> copyOf(T initValue, UnaryOperator<T> copier, CheckStrategy<T> strategy) {
        return new CopyInstance<>(initValue, copier, strategy);
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

    static <T> Snapshot<T> mutableRefOf(CheckStrategy<T> strategy) {
        return new MutableRef<>(strategy);
    }

    static <T> T readValue(Snapshot<T> snapshot) throws IllegalStateException {
        return switch (snapshot) {
            case None ignored -> throw new IllegalStateException("Cannot read value in None snapshot");
            case Readonly<T> ref -> ref.value;
            case ImmutableRef<T> ref -> ref.value;
            case MutableRef<T> ref -> ref.value;
            case CopyInstance<T> ref -> ref.value;
        };
    }

    static <T> @Nullable T getValue(Snapshot<T> snapshot) {
        return switch (snapshot) {
            case None ignored -> null;
            case Readonly<T> ref -> ref.value;
            case ImmutableRef<T> ref -> ref.value;
            case MutableRef<T> ref -> ref.value;
            case CopyInstance<T> ref -> ref.value;
        };
    }

    static <T> State currentState(Snapshot<T> instance, T current) {
        return switch (instance) {
            case None ignored -> State.UNCHANGED;
            case Readonly<T> snapshot -> {
                boolean changed = snapshot.value != current || !snapshot.strategy.test(current);
                if (changed) yield State.ILLEGAL;
                else yield State.UNCHANGED;
            }
            case ImmutableRef<T> snapshot -> {
                if (snapshot.value != current) yield State.ILLEGAL;
                else yield snapshot.strategy.test(snapshot.value) ? State.UNCHANGED : State.CHANGED;
            }
            case MutableRef<T> snapshot -> {
                var changed = !snapshot.strategy.matches(snapshot.value, current);
                if (changed) snapshot.value = current;
                yield changed ? State.CHANGED : State.UNCHANGED;
            }
            case CopyInstance<T> snapshot -> {
                var changed = !snapshot.strategy.matches(snapshot.value, current);
                if (changed) snapshot.set(current);
                yield changed ? State.CHANGED : State.UNCHANGED;
            }
        };
    }

    static <T> boolean changed(Snapshot<T> instance, T current) {
        return switch (currentState(instance, current)) {
            case CHANGED -> true;
            case UNCHANGED -> false;
            case ILLEGAL -> throw new IllegalStateException("The snapshot has been changed illegally");
        };
    }

    /**
     * A snapshot that does not hold any value.
     */
    enum None implements Snapshot<Object> {
        INSTANCE;

        @SuppressWarnings("unchecked")
        private static <T> Snapshot<T> instance() {
            return (Snapshot<T>) INSTANCE;
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    /**
     * snapshot of the readonly value
     *
     * @param <T> the type of the value
     */
    final class Readonly<T> implements Snapshot<T> {
        private final T value;
        private final Predicate<T> strategy;
        private final int hash;

        public Readonly(T value) {
            this.value = value;
            this.hash = Objects.hashCode(value);
            this.strategy = (current -> current == this.value && Objects.hashCode(current) == this.hash);
        }

        public T value() {
            return value;
        }

        public Predicate<T> strategy() {
            return strategy;
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
     *
     * @param <T> the type of the value
     */
    record ImmutableRef<T>(T value, Predicate<T> strategy) implements Snapshot<T> {

        public boolean observable() {
            return value instanceof Observable;
        }

        @Override
        public String toString() {
            return "ImmutableRef{" +
                    "value=" + value +
                    '}';
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

        public T value() {
            return value;
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

        public T value() {
            return value;
        }

        public void set(T value) {
            this.value = copier.apply(value);
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

}
