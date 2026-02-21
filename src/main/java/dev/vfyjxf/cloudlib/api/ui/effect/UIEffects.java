package dev.vfyjxf.cloudlib.api.ui.effect;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingEffect;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddleware;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollDirection;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollEffect;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.list.MutableList;

/**
 * Static DSL entry point for creating {@link Effect} instances.
 * <p>
 * Example usage:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.effect.UIEffects.*;
 *
 * widget.useEffect(autoFocus());
 * widget.useEffect(scrollable(ScrollDirection.VERTICAL));
 * }</pre>
 */
public final class UIEffects {

    private UIEffects() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Effect compose(Effect... effects) {
        var effectList = MutableLists.of(effects);
        return widget -> {
            for (Effect effect : effectList) {
                effect.apply(widget);
            }
        };
    }

    /**
     * Automatically requests focus for the widget after mounting.
     */
    public static Effect autoFocus() {
        return widget -> widget.onMount(
            (scene, context, handle) -> scene.postRender(() -> scene.requestFocus(widget))
        );
    }

    //region scroll

    /**
     * Creates a scroll effect with the given scroll state.
     * <p>
     * The scroll state holds all configuration (direction, scroll speed, scrollbar
     * appearance, smooth scrolling, etc.) and can be mutated externally to control
     * the scroll position. Content size is auto-computed from children bounds by default.
     *
     * <pre>{@code
     * ScrollState state = ScrollState.create(ScrollDirection.VERTICAL)
     *     .scrollSpeed(12)
     *     .smooth(true)
     *     .smoothSpeed(0.3f)
     *     .thumbTexture(new ColorTexture(0xFFCCCCCC));
     *
     * widget.useEffect(scrollable(state));
     *
     * // Widget handles its own scroll events:
     * widget.onMouseScrolled((mouseX, mouseY, scrollX, scrollY, context) -> {
     *     state.scrollBy(0, (float) (-scrollY * state.scrollSpeed()));
     *     return EventDispatch.consumed;
     * });
     * }</pre>
     *
     * @param state the scroll state to use
     * @return a scroll effect
     * @see ScrollEffect
     * @see ScrollState
     */
    public static ScrollEffect scrollable(ScrollState state) {
        return ScrollEffect.of(state);
    }

    /**
     * Creates a scroll effect with a new state for the given direction.
     *
     * @param direction the scroll direction
     * @return a scroll effect
     * @see ScrollEffect
     */
    public static ScrollEffect scrollable(ScrollDirection direction) {
        return ScrollEffect.of(direction);
    }

    /**
     * Creates a vertical scroll effect with default settings.
     *
     * @return a vertical scroll effect
     * @see ScrollEffect#vertical()
     */
    public static ScrollEffect verticalScroll() {
        return ScrollEffect.vertical();
    }

    /**
     * Creates a horizontal scroll effect with default settings.
     *
     * @return a horizontal scroll effect
     * @see ScrollEffect#horizontal()
     */
    public static ScrollEffect horizontalScroll() {
        return ScrollEffect.horizontal();
    }

    //endregion

    //region floating

    /**
     * Creates a floating effect that positions the target widget relative to a reference widget.
     * <p>
     * The floating widget will be absolutely positioned and its coordinates will be
     * recomputed every frame using the configured middleware pipeline.
     *
     * <pre>{@code
     * import static dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares.*;
     *
     * Widget button = ButtonWidget.of("Click me", () -> {});
     * Widget tooltip = LabelWidget.of("Tooltip text");
     *
     * tooltip.useEffect(floating(button, FloatingPlacement.bottom,
     *     offset(8),
     *     flip(),
     *     shift(4)
     * ));
     * }</pre>
     *
     * @param reference  the reference widget to anchor to
     * @param placement  the preferred placement relative to the reference
     * @param middleware ordered middleware to apply (offset, flip, shift, etc.)
     * @return a floating effect
     * @see FloatingEffect
     * @see dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares
     */
    public static FloatingEffect floating(Widget reference, FloatingPlacement placement, FloatingMiddleware... middleware) {
        return FloatingEffect.create(reference, placement, middleware);
    }

    /**
     * Creates a floating effect using the default {@link FloatingPlacement#bottom} placement.
     *
     * @param reference  the reference widget to anchor to
     * @param middleware ordered middleware to apply
     * @return a floating effect
     */
    public static FloatingEffect floating(Widget reference, FloatingMiddleware... middleware) {
        return FloatingEffect.create(reference, FloatingPlacement.bottom, middleware);
    }

    //endregion
}
