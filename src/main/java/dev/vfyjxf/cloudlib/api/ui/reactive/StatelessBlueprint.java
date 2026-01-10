package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A Blueprint that builds its content without reactive state tracking.
 * <p>
 * StatelessBlueprint is similar to Flutter's StatelessWidget. The builder
 * function is executed during build, but unlike {@link StatefulBlueprint},
 * no reactive state dependencies are tracked. This means the component
 * will only rebuild when its parent explicitly updates it with a new blueprint.
 * <p>
 * Use StatelessBlueprint for:
 * <ul>
 *   <li>Pure presentational components that don't depend on any Signal</li>
 *   <li>Components whose output depends only on their configuration (props)</li>
 *   <li>Performance optimization when you know the component doesn't need reactive updates</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * // A stateless greeting component - only rebuilds when parent updates it
 * StatelessBlueprint greeting = StatelessBlueprint.of(() -> {
 *     return Text("Hello, World!");
 * });
 *
 * // A parameterized stateless component
 * StatelessBlueprint userCard(String name, int age) {
 *     return StatelessBlueprint.of(() -> {
 *         return Column(() -> {
 *             Text("Name: " + name);
 *             Text("Age: " + age);
 *         });
 *     });
 * }
 * }</pre>
 *
 * @see StatefulBlueprint for components that track reactive state
 * @see StableBlueprint for components that never rebuild
 */
@ApiStatus.Experimental
public class StatelessBlueprint implements Blueprint {

    private final Supplier<Blueprint> builder;
    @Nullable
    private final Key key;

    private StatelessBlueprint(Supplier<Blueprint> builder, @Nullable Key key) {
        this.builder = builder;
        this.key = key;
    }

    /**
     * Creates a new stateless blueprint with the given builder.
     *
     * @param builder the builder function that produces child blueprints
     * @return a new stateless blueprint
     */
    public static StatelessBlueprint of(Supplier<Blueprint> builder) {
        return new StatelessBlueprint(builder, null);
    }

    /**
     * Creates a new stateless blueprint with a key.
     *
     * @param key     the key for this blueprint
     * @param builder the builder function
     * @return a new stateless blueprint
     */
    public static StatelessBlueprint of(Key key, Supplier<Blueprint> builder) {
        return new StatelessBlueprint(builder, key);
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
        return new StatelessElement<>(this);
    }
}
