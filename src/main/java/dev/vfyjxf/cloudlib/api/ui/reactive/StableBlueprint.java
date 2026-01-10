package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A Blueprint that is marked as completely stable and will NEVER rebuild.
 * <p>
 * StableBlueprint is an optimization hint that tells the reconciliation system
 * this component's output is constant and will never change. Once built,
 * the element will skip ALL rebuild requests, including:
 * <ul>
 *   <li>Parent updates (the blueprint is ignored during reconciliation)</li>
 *   <li>Manual markNeedsBuild() calls</li>
 *   <li>Any reactive state changes</li>
 * </ul>
 * <p>
 * This is similar to React's constant elements or Flutter's const widgets.
 * Use StableBlueprint when you have truly static content that will never
 * change for the lifetime of the component.
 * <p>
 * <b>WARNING:</b> If you wrap content that actually needs to update,
 * those updates will be silently ignored. Use with caution!
 * <p>
 * Example:
 * <pre>{@code
 * // A static header that never changes
 * StableBlueprint header = StableBlueprint.of(() -> {
 *     return Column(() -> {
 *         Text("Application Title");
 *         Text("Version 1.0.0");
 *     });
 * });
 *
 * // Static icon that never changes
 * StableBlueprint icon = StableBlueprint.of(() -> {
 *     return Image("assets/logo.png");
 * });
 * }</pre>
 *
 * @see StatelessBlueprint for components that don't track state but can be updated
 * @see StatefulBlueprint for components that track reactive state
 */
@ApiStatus.Experimental
public class StableBlueprint implements Blueprint {

    private final Supplier<Blueprint> builder;
    @Nullable
    private final Key key;

    private StableBlueprint(Supplier<Blueprint> builder, @Nullable Key key) {
        this.builder = builder;
        this.key = key;
    }

    /**
     * Creates a new stable blueprint with the given builder.
     * <p>
     * The builder is executed exactly once during the first build.
     * After that, the component is considered immutable.
     *
     * @param builder the builder function that produces child blueprints
     * @return a new stable blueprint
     */
    public static StableBlueprint of(Supplier<Blueprint> builder) {
        return new StableBlueprint(builder, null);
    }

    /**
     * Creates a new stable blueprint with a key.
     *
     * @param key     the key for this blueprint
     * @param builder the builder function
     * @return a new stable blueprint
     */
    public static StableBlueprint of(Key key, Supplier<Blueprint> builder) {
        return new StableBlueprint(builder, key);
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
        return new StableElement<>(this);
    }
}
