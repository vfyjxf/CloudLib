package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A layout blueprint that arranges children vertically in a column.
 * <p>
 * Example:
 * <pre>{@code
 * Column(() -> {
 *     Text("First");
 *     Text("Second");
 *     Text("Third");
 * });
 * }</pre>
 */
@ApiStatus.Experimental
public record ColumnBlueprint(
        List<Blueprint> children,
        Alignment mainAxisAlignment,
        Alignment crossAxisAlignment,
        double spacing,
        @Nullable Key key
) implements CompositeBlueprint {

    /**
     * Creates a column with default settings.
     *
     * @param children the child blueprints
     */
    public ColumnBlueprint(List<Blueprint> children) {
        this(children, Alignment.START, Alignment.START, 0, null);
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
     * Creates a column with children defined in a scope.
     *
     * @param builder the builder lambda
     * @return the created column blueprint
     */
    public static ColumnBlueprint Column(Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ColumnBlueprint blueprint = new ColumnBlueprint(children);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a column with spacing.
     *
     * @param spacing the spacing between children
     * @param builder the builder lambda
     * @return the created column blueprint
     */
    public static ColumnBlueprint Column(double spacing, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ColumnBlueprint blueprint = new ColumnBlueprint(
                children, Alignment.START, Alignment.START, spacing, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a column with alignment settings.
     *
     * @param mainAxisAlignment  the main axis (vertical) alignment
     * @param crossAxisAlignment the cross axis (horizontal) alignment
     * @param builder            the builder lambda
     * @return the created column blueprint
     */
    public static ColumnBlueprint Column(
            Alignment mainAxisAlignment,
            Alignment crossAxisAlignment,
            Runnable builder
    ) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ColumnBlueprint blueprint = new ColumnBlueprint(
                children, mainAxisAlignment, crossAxisAlignment, 0, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a column with full configuration.
     *
     * @param mainAxisAlignment  the main axis (vertical) alignment
     * @param crossAxisAlignment the cross axis (horizontal) alignment
     * @param spacing            the spacing between children
     * @param builder            the builder lambda
     * @return the created column blueprint
     */
    public static ColumnBlueprint Column(
            Alignment mainAxisAlignment,
            Alignment crossAxisAlignment,
            double spacing,
            Runnable builder
    ) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ColumnBlueprint blueprint = new ColumnBlueprint(
                children, mainAxisAlignment, crossAxisAlignment, spacing, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a column with a key.
     *
     * @param key     the key
     * @param builder the builder lambda
     * @return the created column blueprint
     */
    public static ColumnBlueprint Column(Key key, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ColumnBlueprint blueprint = new ColumnBlueprint(
                children, Alignment.START, Alignment.START, 0, key
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Alignment options for layout blueprints.
     */
    public enum Alignment {
        START,
        CENTER,
        END,
        SPACE_BETWEEN,
        SPACE_AROUND,
        SPACE_EVENLY
    }
}
