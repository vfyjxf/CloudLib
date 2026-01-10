package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A layout blueprint that stacks children on top of each other.
 * <p>
 * Children are rendered in order, with later children appearing on top.
 * <p>
 * Example:
 * <pre>{@code
 * Stack(() -> {
 *     // Background
 *     Container(backgroundColor(0xFF000000), () -> {});
 *     // Foreground content
 *     Text("On top");
 * });
 * }</pre>
 */
@ApiStatus.Experimental
public record StackBlueprint(
        List<Blueprint> children,
        StackFit fit,
        ColumnBlueprint.Alignment alignment,
        @Nullable Key key
) implements CompositeBlueprint {

    /**
     * Creates a stack with default settings.
     *
     * @param children the child blueprints
     */
    public StackBlueprint(List<Blueprint> children) {
        this(children, StackFit.LOOSE, ColumnBlueprint.Alignment.START, null);
    }

    @Override
    public List<Blueprint> getChildren() {
        return children;
    }

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    // ========== Static DSL Methods ==========

    /**
     * Creates a stack with children defined in a scope.
     *
     * @param builder the builder lambda
     * @return the created blueprint
     */
    public static StackBlueprint Stack(Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        StackBlueprint blueprint = new StackBlueprint(children);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a stack with alignment.
     *
     * @param alignment the alignment for children
     * @param builder   the builder lambda
     * @return the created blueprint
     */
    public static StackBlueprint Stack(ColumnBlueprint.Alignment alignment, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        StackBlueprint blueprint = new StackBlueprint(children, StackFit.LOOSE, alignment, null);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a stack with fit and alignment.
     *
     * @param fit       how to size children
     * @param alignment the alignment for children
     * @param builder   the builder lambda
     * @return the created blueprint
     */
    public static StackBlueprint Stack(StackFit fit, ColumnBlueprint.Alignment alignment, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        StackBlueprint blueprint = new StackBlueprint(children, fit, alignment, null);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a stack with a key.
     *
     * @param key     the key
     * @param builder the builder lambda
     * @return the created blueprint
     */
    public static StackBlueprint Stack(Key key, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        StackBlueprint blueprint = new StackBlueprint(children, StackFit.LOOSE, ColumnBlueprint.Alignment.START, key);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * How to size children in a stack.
     */
    public enum StackFit {
        /**
         * Children can be any size up to the stack's size.
         */
        LOOSE,

        /**
         * Children are forced to fill the stack.
         */
        EXPAND,

        /**
         * Stack sizes itself to the largest child.
         */
        PASSTHROUGH
    }
}
