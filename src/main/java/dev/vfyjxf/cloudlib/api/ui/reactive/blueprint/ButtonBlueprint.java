package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.LeafElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A leaf blueprint that represents a clickable button.
 * <p>
 * Example:
 * <pre>{@code
 * Button("Click me", () -> {
 *     System.out.println("Clicked!");
 * });
 * }</pre>
 */
@ApiStatus.Experimental
public record ButtonBlueprint(
        String label,
        Runnable onClick,
        boolean enabled,
        @Nullable Key key
) implements Blueprint {

    /**
     * Creates a button with default settings.
     *
     * @param label   the button label
     * @param onClick the click handler
     */
    public ButtonBlueprint(String label, Runnable onClick) {
        this(label, onClick, true, null);
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

    // ========== Static DSL Methods ==========

    /**
     * Creates a button and adds it to the current scope.
     *
     * @param label   the button label
     * @param onClick the click handler
     * @return the created blueprint
     */
    public static ButtonBlueprint Button(String label, Runnable onClick) {
        ButtonBlueprint blueprint = new ButtonBlueprint(label, onClick);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a button with enabled state.
     *
     * @param label   the button label
     * @param enabled whether the button is enabled
     * @param onClick the click handler
     * @return the created blueprint
     */
    public static ButtonBlueprint Button(String label, boolean enabled, Runnable onClick) {
        ButtonBlueprint blueprint = new ButtonBlueprint(label, onClick, enabled, null);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a button with a key.
     *
     * @param key     the key
     * @param label   the button label
     * @param onClick the click handler
     * @return the created blueprint
     */
    public static ButtonBlueprint Button(Key key, String label, Runnable onClick) {
        ButtonBlueprint blueprint = new ButtonBlueprint(label, onClick, true, key);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }
}
