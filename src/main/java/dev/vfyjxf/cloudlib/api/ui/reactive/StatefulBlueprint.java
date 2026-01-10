package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A Blueprint that builds its content dynamically using a builder function.
 * <p>
 * StatefulBlueprint is similar to Flutter's StatefulWidget or React's functional
 * components. The builder function is executed during build, and any reactive
 * state accessed during build is automatically tracked.
 * <p>
 * When any tracked state changes, the element is marked for rebuild and the
 * builder function is re-executed to produce new child blueprints.
 * <p>
 * Example:
 * <pre>{@code
 * StatefulBlueprint counter = StatefulBlueprint.of(() -> {
 *     Signal<Integer> count = Signal.of(0);
 *     return Column(() -> {
 *         Text("Count: " + count.get());
 *         Button("Increment", () -> count.update(n -> n + 1));
 *     });
 * });
 * }</pre>
 */
@ApiStatus.Experimental
public class StatefulBlueprint implements Blueprint {

    private final Supplier<Blueprint> builder;
    @Nullable
    private final Key key;

    private StatefulBlueprint(Supplier<Blueprint> builder, @Nullable Key key) {
        this.builder = builder;
        this.key = key;
    }

    /**
     * Creates a new stateful blueprint with the given builder.
     *
     * @param builder the builder function that produces child blueprints
     * @return a new stateful blueprint
     */
    public static StatefulBlueprint of(Supplier<Blueprint> builder) {
        return new StatefulBlueprint(builder, null);
    }

    /**
     * Creates a new stateful blueprint with a key.
     *
     * @param key     the key for this blueprint
     * @param builder the builder function
     * @return a new stateful blueprint
     */
    public static StatefulBlueprint of(Key key, Supplier<Blueprint> builder) {
        return new StatefulBlueprint(builder, key);
    }

    /**
     * Gets the builder function.
     *
     * @return the builder
     */
    public Supplier<Blueprint> getBuilder() {
        return builder;
    }

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    @Override
    public UIElement<?> createElement() {
        return new StatefulElement<>(this);
    }
}
