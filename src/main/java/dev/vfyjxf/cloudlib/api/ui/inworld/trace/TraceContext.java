package dev.vfyjxf.cloudlib.api.ui.inworld.trace;

import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * An extensible context carrier handed to {@link Tracer}s (and cursors) next to
 * the {@link TraceSource}: a typed-key map implementations fill with whatever
 * they need beyond the source itself.
 * <p>
 * Keys are identity-based — two keys with the same name are distinct — so
 * implementations can define private keys in their own classes without
 * coordinating names. The keys declared here are the conventional ones the
 * bundled cursors rely on; they are conveniences, not a closed set.
 */
public final class TraceContext {

    /**
     * A typed context key. Create with {@link #of(String)}; the name is for
     * debugging only, equality is identity.
     */
    public static final class Key<T> {

        private final String name;

        private Key(String name) {
            this.name = name;
        }

        public static <T> Key<T> of(String name) {
            return new Key<>(name);
        }

        @Override
        public String toString() {
            return "Key(" + name + ')';
        }
    }

    // region conventional keys

    /** The client level the trace resolves against. */
    public static final Key<ClientLevel> level = Key.of("level");

    /** The frame's partial tick. */
    public static final Key<Float> partialTick = Key.of("partialTick");

    /** The frame's world ↔ screen conversion. */
    public static final Key<Projection> projection = Key.of("projection");

    /** The camera's world position. */
    public static final Key<Vec3> cameraPos = Key.of("cameraPos");

    // endregion

    private final Map<Key<?>, Object> values = new HashMap<>();

    private TraceContext() {}

    /** A fresh, empty context. */
    public static TraceContext create() {
        return new TraceContext();
    }

    /** Sets {@code key} to {@code value}; a null value removes the key. */
    public <T> TraceContext with(Key<T> key, @Nullable T value) {
        if (value == null) values.remove(key);
        else values.put(key, value);
        return this;
    }

    public boolean has(Key<?> key) {
        return values.containsKey(key);
    }

    /** The value bound to {@code key}, or null when absent. */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(Key<T> key) {
        return (T) values.get(key);
    }
}
