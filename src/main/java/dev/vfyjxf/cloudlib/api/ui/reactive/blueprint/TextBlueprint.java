package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.LeafElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A leaf blueprint that displays text.
 * <p>
 * TextBlueprint is a simple, non-composite blueprint that renders
 * a text string. It can accept either a static string or a reactive
 * supplier for dynamic text.
 * <p>
 * Example:
 * <pre>{@code
 * // Static text
 * Text("Hello World");
 * 
 * // Reactive text
 * Signal<Integer> count = Signal.of(0);
 * Text(() -> "Count: " + count.get());
 * }</pre>
 */
@ApiStatus.Experimental
public record TextBlueprint(
        Supplier<String> textSupplier,
        int color,
        @Nullable Key key
) implements Blueprint {

    /**
     * Default text color (white).
     */
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    /**
     * Creates a text blueprint with static text.
     *
     * @param text the text to display
     */
    public TextBlueprint(String text) {
        this(() -> text, DEFAULT_COLOR, null);
    }

    /**
     * Creates a text blueprint with static text and color.
     *
     * @param text  the text to display
     * @param color the color (ARGB)
     */
    public TextBlueprint(String text, int color) {
        this(() -> text, color, null);
    }

    /**
     * Creates a text blueprint with a reactive text supplier.
     *
     * @param textSupplier the supplier for dynamic text
     */
    public TextBlueprint(Supplier<String> textSupplier) {
        this(textSupplier, DEFAULT_COLOR, null);
    }

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    @Override
    public UIElement<?> createElement() {
        return new LeafElement<>(this);
    }

    /**
     * Gets the current text value.
     *
     * @return the text
     */
    public String getText() {
        return textSupplier.get();
    }

    /**
     * Creates a copy of this blueprint with a different key.
     *
     * @param newKey the new key
     * @return a new TextBlueprint with the specified key
     */
    public TextBlueprint withKey(@Nullable Key newKey) {
        return new TextBlueprint(textSupplier, color, newKey);
    }

    // ========== Static DSL Methods ==========

    /**
     * Creates a static text blueprint and adds it to the current scope.
     *
     * @param text the text to display
     * @return the created blueprint
     */
    public static TextBlueprint Text(String text) {
        TextBlueprint blueprint = new TextBlueprint(text);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a static text blueprint with color and adds it to the current scope.
     *
     * @param text  the text to display
     * @param color the color (ARGB)
     * @return the created blueprint
     */
    public static TextBlueprint Text(String text, int color) {
        TextBlueprint blueprint = new TextBlueprint(text, color);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a reactive text blueprint and adds it to the current scope.
     *
     * @param textSupplier the supplier for dynamic text
     * @return the created blueprint
     */
    public static TextBlueprint Text(Supplier<String> textSupplier) {
        TextBlueprint blueprint = new TextBlueprint(textSupplier);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a reactive text blueprint with color and adds it to the current scope.
     *
     * @param textSupplier the supplier for dynamic text
     * @param color        the color (ARGB)
     * @return the created blueprint
     */
    public static TextBlueprint Text(Supplier<String> textSupplier, int color) {
        TextBlueprint blueprint = new TextBlueprint(textSupplier, color, null);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a text blueprint with a key.
     *
     * @param key  the key
     * @param text the text to display
     * @return the created blueprint
     */
    public static TextBlueprint Text(Key key, String text) {
        TextBlueprint blueprint = new TextBlueprint(() -> text, DEFAULT_COLOR, key);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }
}
