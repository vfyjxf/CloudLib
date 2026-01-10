package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A blueprint that renders a list of items using a builder function.
 * <p>
 * ForEach efficiently updates the list by using keys to track items.
 * <p>
 * Example:
 * <pre>{@code
 * Signal<List<String>> items = Signal.of(List.of("Apple", "Banana", "Cherry"));
 *
 * ForEach(items::get, item -> {
 *     Text(item);
 * });
 *
 * // With key function for efficient updates
 * ForEach(items::get, String::hashCode, item -> {
 *     Text(item);
 * });
 * }</pre>
 *
 * @param <T> the type of items in the list
 */
@ApiStatus.Experimental
public class ForEachBlueprint<T> implements CompositeBlueprint {

    private final Supplier<? extends Iterable<T>> itemsSupplier;
    private final Function<T, Object> keyFunction;
    private final BiFunction<T, Integer, Blueprint> itemBuilder;
    @Nullable
    private final Key key;

    private ForEachBlueprint(
        Supplier<? extends Iterable<T>> itemsSupplier,
        Function<T, Object> keyFunction,
        BiFunction<T, Integer, Blueprint> itemBuilder,
        @Nullable Key key
    ) {
        this.itemsSupplier = itemsSupplier;
        this.keyFunction = keyFunction;
        this.itemBuilder = itemBuilder;
        this.key = key;
    }

    @Override
    public List<Blueprint> getChildren() {
        List<Blueprint> children = new ArrayList<>();
        int index = 0;
        for (T item : itemsSupplier.get()) {
            Blueprint child = itemBuilder.apply(item, index);
            // Wrap with a keyed wrapper if the child doesn't have a key
            if (child.key() == null) {
                Object itemKey = keyFunction.apply(item);
                child = new KeyedWrapper(child, Key.of(itemKey));
            }
            children.add(child);
            index++;
        }
        return children;
    }

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    @Override
    public UIElement<?> createElement() {
        return new ForEachElement<>(this);
    }

    // ========== Static DSL Methods ==========

    /**
     * Creates a ForEach blueprint using item identity as key.
     *
     * @param itemsSupplier supplier for the items
     * @param itemBuilder   builder for each item (receives item)
     * @param <T>           the item type
     * @return the created blueprint
     */
    public static <T> ForEachBlueprint<T> ForEach(
        Supplier<? extends Iterable<T>> itemsSupplier,
        Function<T, Blueprint> itemBuilder
    ) {
        ForEachBlueprint<T> blueprint = new ForEachBlueprint<>(
            itemsSupplier,
            o -> o,
            (item, index) -> itemBuilder.apply(item),
            null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a ForEach blueprint with item and index.
     *
     * @param itemsSupplier supplier for the items
     * @param itemBuilder   builder for each item (receives item and index)
     * @param <T>           the item type
     * @return the created blueprint
     */
    public static <T> ForEachBlueprint<T> ForEachIndexed(
        Supplier<? extends Iterable<T>> itemsSupplier,
        BiFunction<T, Integer, Blueprint> itemBuilder
    ) {
        ForEachBlueprint<T> blueprint = new ForEachBlueprint<>(
            itemsSupplier,
            o -> o,
            itemBuilder,
            null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a ForEach blueprint with custom key function.
     *
     * @param itemsSupplier supplier for the items
     * @param keyFunction   function to extract key from item
     * @param itemBuilder   builder for each item
     * @param <T>           the item type
     * @return the created blueprint
     */
    public static <T> ForEachBlueprint<T> ForEach(
        Supplier<? extends Iterable<T>> itemsSupplier,
        Function<T, Object> keyFunction,
        Function<T, Blueprint> itemBuilder
    ) {
        ForEachBlueprint<T> blueprint = new ForEachBlueprint<>(
            itemsSupplier,
            keyFunction,
            (item, index) -> itemBuilder.apply(item),
            null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a ForEach blueprint with a scope-based builder.
     *
     * @param itemsSupplier supplier for the items
     * @param itemBuilder   builder that declares children in a scope
     * @param <T>           the item type
     * @return the created blueprint
     */
    public static <T> ForEachBlueprint<T> ForEachScoped(
        Supplier<? extends Iterable<T>> itemsSupplier,
        java.util.function.Consumer<T> itemBuilder
    ) {
        ForEachBlueprint<T> blueprint = new ForEachBlueprint<>(
            itemsSupplier,
            o -> o,
            (item, index) -> {
                List<Blueprint> children = ScopedReceiver.buildScope(() -> itemBuilder.accept(item));
                // Return a container if multiple children, or single child directly
                if (children.size() == 1) {
                    return children.get(0);
                }
                return new ColumnBlueprint(children);
            },
            null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * A wrapper that adds a key to a blueprint.
     */
    private record KeyedWrapper(Blueprint wrapped, Key wrapperKey) implements Blueprint {

        @Override
        public Key key() {
            return wrapperKey;
        }

        @Override
        public UIElement<?> createElement() {
            return wrapped.createElement();
        }

        @Override
        public boolean canUpdate(Blueprint oldBlueprint) {
            if (oldBlueprint instanceof KeyedWrapper other) {
                return wrapped.canUpdate(other.wrapped);
            }
            return wrapped.canUpdate(oldBlueprint);
        }
    }

    /**
     * Element for ForEach blueprints.
     */
    private static class ForEachElement<T> extends CompositeElement<ForEachBlueprint<T>> {

        public ForEachElement(ForEachBlueprint<T> blueprint) {
            super(blueprint);
        }
    }
}
