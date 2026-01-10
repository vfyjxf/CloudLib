package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A layout blueprint that arranges children horizontally in a row.
 * <p>
 * Example:
 * <pre>{@code
 * Row(() -> {
 *     Text("Left");
 *     Text("Center");
 *     Text("Right");
 * });
 * }</pre>
 */
@ApiStatus.Experimental
public record RowBlueprint(
        List<Blueprint> children,
        ColumnBlueprint.Alignment mainAxisAlignment,
        ColumnBlueprint.Alignment crossAxisAlignment,
        double spacing,
        @Nullable Key key
) implements CompositeBlueprint {

    /**
     * Creates a row with default settings.
     *
     * @param children the child blueprints
     */
    public RowBlueprint(List<Blueprint> children) {
        this(children, ColumnBlueprint.Alignment.START, ColumnBlueprint.Alignment.START, 0, null);
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
     * Creates a row with children defined in a scope.
     *
     * @param builder the builder lambda
     * @return the created row blueprint
     */
    public static RowBlueprint Row(Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        RowBlueprint blueprint = new RowBlueprint(children);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a row with spacing.
     *
     * @param spacing the spacing between children
     * @param builder the builder lambda
     * @return the created row blueprint
     */
    public static RowBlueprint Row(double spacing, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        RowBlueprint blueprint = new RowBlueprint(
                children, ColumnBlueprint.Alignment.START, ColumnBlueprint.Alignment.START, spacing, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a row with alignment settings.
     *
     * @param mainAxisAlignment  the main axis (horizontal) alignment
     * @param crossAxisAlignment the cross axis (vertical) alignment
     * @param builder            the builder lambda
     * @return the created row blueprint
     */
    public static RowBlueprint Row(
            ColumnBlueprint.Alignment mainAxisAlignment,
            ColumnBlueprint.Alignment crossAxisAlignment,
            Runnable builder
    ) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        RowBlueprint blueprint = new RowBlueprint(
                children, mainAxisAlignment, crossAxisAlignment, 0, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a row with full configuration.
     *
     * @param mainAxisAlignment  the main axis (horizontal) alignment
     * @param crossAxisAlignment the cross axis (vertical) alignment
     * @param spacing            the spacing between children
     * @param builder            the builder lambda
     * @return the created row blueprint
     */
    public static RowBlueprint Row(
            ColumnBlueprint.Alignment mainAxisAlignment,
            ColumnBlueprint.Alignment crossAxisAlignment,
            double spacing,
            Runnable builder
    ) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        RowBlueprint blueprint = new RowBlueprint(
                children, mainAxisAlignment, crossAxisAlignment, spacing, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a row with a key.
     *
     * @param key     the key
     * @param builder the builder lambda
     * @return the created row blueprint
     */
    public static RowBlueprint Row(Key key, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        RowBlueprint blueprint = new RowBlueprint(
                children, ColumnBlueprint.Alignment.START, ColumnBlueprint.Alignment.START, 0, key
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }
}
