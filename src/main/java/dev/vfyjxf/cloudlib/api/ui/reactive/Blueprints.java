package dev.vfyjxf.cloudlib.api.ui.reactive;

import dev.vfyjxf.cloudlib.api.ui.reactive.blueprint.*;
import dev.vfyjxf.cloudlib.api.ui.reactive.state.Signal;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static DSL entry points for building reactive UI.
 * <p>
 * Import this class statically to use the DSL:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.Blueprints.*;
 * 
 * Blueprint ui = Column(() -> {
 *     Text("Hello");
 *     Row(() -> {
 *         Button("Click", () -> {});
 *         Text("World");
 *     });
 * });
 * }</pre>
 * <p>
 * The DSL uses thread-local scopes to collect children declared within lambda blocks.
 * This enables a declarative, React/Compose-like syntax without explicit child lists.
 */
@ApiStatus.Experimental
public final class Blueprints {

    private Blueprints() {}

    // ========== Layout Blueprints ==========

    /**
     * Creates a vertical layout (column).
     */
    public static ColumnBlueprint Column(Runnable builder) {
        return ColumnBlueprint.Column(builder);
    }

    /**
     * Creates a vertical layout with spacing.
     */
    public static ColumnBlueprint Column(double spacing, Runnable builder) {
        return ColumnBlueprint.Column(spacing, builder);
    }

    /**
     * Creates a vertical layout with alignment.
     */
    public static ColumnBlueprint Column(
            ColumnBlueprint.Alignment mainAxis,
            ColumnBlueprint.Alignment crossAxis,
            Runnable builder
    ) {
        return ColumnBlueprint.Column(mainAxis, crossAxis, builder);
    }

    /**
     * Creates a horizontal layout (row).
     */
    public static RowBlueprint Row(Runnable builder) {
        return RowBlueprint.Row(builder);
    }

    /**
     * Creates a horizontal layout with spacing.
     */
    public static RowBlueprint Row(double spacing, Runnable builder) {
        return RowBlueprint.Row(spacing, builder);
    }

    /**
     * Creates a horizontal layout with alignment.
     */
    public static RowBlueprint Row(
            ColumnBlueprint.Alignment mainAxis,
            ColumnBlueprint.Alignment crossAxis,
            Runnable builder
    ) {
        return RowBlueprint.Row(mainAxis, crossAxis, builder);
    }

    /**
     * Creates a stacked layout.
     */
    public static StackBlueprint Stack(Runnable builder) {
        return StackBlueprint.Stack(builder);
    }

    /**
     * Creates a container with uniform padding.
     */
    public static ContainerBlueprint Container(Runnable builder) {
        return ContainerBlueprint.Container(builder);
    }

    /**
     * Creates a container with padding.
     */
    public static ContainerBlueprint Container(double padding, Runnable builder) {
        return ContainerBlueprint.Container(padding, builder);
    }

    /**
     * Creates a sized container.
     */
    public static ContainerBlueprint SizedContainer(double width, double height, Runnable builder) {
        return ContainerBlueprint.SizedContainer(width, height, builder);
    }

    /**
     * Creates a container builder for full configuration.
     */
    public static ContainerBlueprint.Builder container() {
        return ContainerBlueprint.container();
    }

    // ========== Leaf Blueprints ==========

    /**
     * Creates a text blueprint with static text.
     */
    public static TextBlueprint Text(String text) {
        return TextBlueprint.Text(text);
    }

    /**
     * Creates a text blueprint with static text and color.
     */
    public static TextBlueprint Text(String text, int color) {
        return TextBlueprint.Text(text, color);
    }

    /**
     * Creates a text blueprint with reactive text.
     */
    public static TextBlueprint Text(Supplier<String> textSupplier) {
        return TextBlueprint.Text(textSupplier);
    }

    /**
     * Creates a text blueprint with reactive text and color.
     */
    public static TextBlueprint Text(Supplier<String> textSupplier, int color) {
        return TextBlueprint.Text(textSupplier, color);
    }

    /**
     * Creates a text blueprint with a key.
     */
    public static TextBlueprint Text(Key key, String text) {
        return TextBlueprint.Text(key, text);
    }

    /**
     * Creates a button blueprint.
     */
    public static ButtonBlueprint Button(String label, Runnable onClick) {
        return ButtonBlueprint.Button(label, onClick);
    }

    /**
     * Creates a button blueprint with enabled state.
     */
    public static ButtonBlueprint Button(String label, boolean enabled, Runnable onClick) {
        return ButtonBlueprint.Button(label, enabled, onClick);
    }

    // ========== Control Flow Blueprints ==========

    /**
     * Creates a conditional blueprint (if-then).
     */
    public static ConditionalBlueprint If(Supplier<Boolean> condition, Runnable thenBuilder) {
        return ConditionalBlueprint.If(condition, thenBuilder);
    }

    /**
     * Creates a conditional blueprint (if-then-else).
     */
    public static ConditionalBlueprint If(
            Supplier<Boolean> condition,
            Runnable thenBuilder,
            Runnable elseBuilder
    ) {
        return ConditionalBlueprint.If(condition, thenBuilder, elseBuilder);
    }

    /**
     * Creates a ForEach blueprint for rendering lists.
     */
    public static <T> ForEachBlueprint<T> ForEach(
            Supplier<? extends Iterable<T>> items,
            Function<T, Blueprint> itemBuilder
    ) {
        return ForEachBlueprint.ForEach(items, itemBuilder);
    }

    /**
     * Creates a ForEach blueprint with custom key function.
     */
    public static <T> ForEachBlueprint<T> ForEach(
            Supplier<? extends Iterable<T>> items,
            Function<T, Object> keyFunction,
            Function<T, Blueprint> itemBuilder
    ) {
        return ForEachBlueprint.ForEach(items, keyFunction, itemBuilder);
    }

    /**
     * Creates a ForEach blueprint with index.
     */
    public static <T> ForEachBlueprint<T> ForEachIndexed(
            Supplier<? extends Iterable<T>> items,
            BiFunction<T, Integer, Blueprint> itemBuilder
    ) {
        return ForEachBlueprint.ForEachIndexed(items, itemBuilder);
    }

    /**
     * Creates a ForEach blueprint with scoped builder.
     */
    public static <T> ForEachBlueprint<T> ForEachScoped(
            Supplier<? extends Iterable<T>> items,
            java.util.function.Consumer<T> itemBuilder
    ) {
        return ForEachBlueprint.ForEachScoped(items, itemBuilder);
    }

    // ========== Stateful Blueprint ==========

    /**
     * Creates a stateful blueprint with automatic state tracking.
     * When used inside a scope (e.g., Column), automatically adds itself to the scope.
     */
    public static StatefulBlueprint Stateful(Supplier<Blueprint> builder) {
        StatefulBlueprint blueprint = StatefulBlueprint.of(builder);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a stateful blueprint with a key.
     * When used inside a scope, automatically adds itself to the scope.
     */
    public static StatefulBlueprint Stateful(Key key, Supplier<Blueprint> builder) {
        StatefulBlueprint blueprint = StatefulBlueprint.of(key, builder);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    // ========== Stateless Blueprint ==========

    /**
     * Creates a stateless blueprint that does NOT track reactive dependencies.
     * <p>
     * Unlike {@link #Stateful}, reading from Signals during build will NOT
     * cause automatic rebuilds. The component only rebuilds when its parent
     * explicitly updates it with a new blueprint.
     * <p>
     * Use for pure presentational components that don't need reactive updates.
     */
    public static StatelessBlueprint Stateless(Supplier<Blueprint> builder) {
        StatelessBlueprint blueprint = StatelessBlueprint.of(builder);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a stateless blueprint with a key.
     */
    public static StatelessBlueprint Stateless(Key key, Supplier<Blueprint> builder) {
        StatelessBlueprint blueprint = StatelessBlueprint.of(key, builder);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    // ========== Stable Blueprint ==========

    /**
     * Creates a stable blueprint that NEVER rebuilds after the first build.
     * <p>
     * This is an aggressive optimization for truly static content.
     * After the initial build, ALL rebuild requests are ignored:
     * <ul>
     *   <li>Parent updates are ignored</li>
     *   <li>markNeedsBuild() does nothing</li>
     *   <li>Reactive state changes have no effect</li>
     * </ul>
     * <p>
     * <b>WARNING:</b> Use only for content that will never change!
     */
    public static StableBlueprint Stable(Supplier<Blueprint> builder) {
        StableBlueprint blueprint = StableBlueprint.of(builder);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a stable blueprint with a key.
     */
    public static StableBlueprint Stable(Key key, Supplier<Blueprint> builder) {
        StableBlueprint blueprint = StableBlueprint.of(key, builder);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    // ========== State Creation ==========

    /**
     * Creates a new signal with the given initial value.
     */
    public static <T> Signal<T> signal(T initialValue) {
        return Signal.of(initialValue);
    }

    /**
     * Creates a new empty signal.
     */
    public static <T> Signal<T> signal() {
        return Signal.empty();
    }

    // ========== Keys ==========

    /**
     * Creates a value-based key.
     */
    public static Key key(Object value) {
        return Key.of(value);
    }

    /**
     * Creates a unique key.
     */
    public static Key uniqueKey() {
        return Key.unique();
    }
}
