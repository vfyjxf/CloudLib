package dev.vfyjxf.cloudlib.api.ui.effect;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;

/**
 * Base interface for all effects.
 * <p>
 * An Effect represents a composable side effect or behavior that can be applied to a widget.
 */
@FunctionalInterface
public interface Effect {

    /**
     * Applies this effect to the given widget.
     *
     * @param widget the widget to apply the effect to
     */
    void apply(Widget widget);
}
