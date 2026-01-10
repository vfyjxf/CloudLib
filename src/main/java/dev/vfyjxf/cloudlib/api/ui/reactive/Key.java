package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A key that uniquely identifies a Blueprint within its parent.
 * <p>
 * Keys are used during reconciliation to match old and new blueprints.
 * When a blueprint has a key, it will be matched with the old blueprint
 * that has the same key, allowing for efficient updates and reordering.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Using a value key
 * Text("Item", Key.of(item.id()));
 * 
 * // Using a unique key (for truly unique widgets)
 * Text("Unique item", Key.unique());
 * }</pre>
 */
@ApiStatus.Experimental
public sealed interface Key {

    /**
     * Creates a value-based key.
     *
     * @param value the value to use as key (compared by equals)
     * @return a new key
     */
    static Key of(Object value) {
        return new ValueKey(value);
    }

    /**
     * Creates a unique key that will never match another key.
     *
     * @return a new unique key
     */
    static Key unique() {
        return new UniqueKey();
    }

    /**
     * A key based on a value, using equals() for comparison.
     */
    record ValueKey(Object value) implements Key {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ValueKey other)) return false;
            return Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return "Key(" + value + ")";
        }
    }

    /**
     * A key that uses identity comparison - each instance is unique.
     */
    final class UniqueKey implements Key {
        @Override
        public String toString() {
            return "UniqueKey@" + Integer.toHexString(System.identityHashCode(this));
        }
    }
}
