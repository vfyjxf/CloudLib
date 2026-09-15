package dev.vfyjxf.cloudlib.api.ui.style;

import org.jetbrains.annotations.Nullable;

/**
 * Listener for style property changes.
 * <p>
 * This interface allows widgets to react to specific property changes during style application.
 * Listeners are registered via {@link StyleContext#addChangeListener(StyleType, StyleChangeListener)}.
 * <p>
 * Example usage:
 * <pre>{@code
 * styleContext.addChangeListener(ZIndexProperty.type, (oldValue, newValue) -> {
 *     if (!Objects.equals(oldValue, newValue)) {
 *         // handle change
 *     }
 * });
 * }</pre>
 *
 * @param <T> the type of the property value
 */
@FunctionalInterface
public interface StyleChangeListener<T> {

    /**
     * Called when a property value changes.
     *
     * @param oldValue the previous value (may be null if not previously set)
     * @param newValue the new value being applied
     */
    void onChanged(@Nullable T oldValue, T newValue);
}
