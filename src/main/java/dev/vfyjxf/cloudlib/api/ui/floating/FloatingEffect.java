package dev.vfyjxf.cloudlib.api.ui.floating;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.effect.Effect;
import dev.vfyjxf.cloudlib.api.ui.layout.LayoutScope;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An {@link Effect} that positions a widget relative to a reference widget using
 * the floating positioning engine.
 * <p>
 * When applied, this effect:
 * <ul>
 *   <li>Sets the floating widget to absolute positioning so it doesn't affect the flow layout</li>
 *   <li>Registers a {@link dev.vfyjxf.cloudlib.api.ui.layout.LayoutHandler LayoutHandler} that
 *       computes the floating position during every layout pass</li>
 *   <li>Runs the configured {@link FloatingMiddleware} pipeline (offset, flip, shift, etc.)</li>
 *   <li>Writes the computed coordinates into the {@link LayoutScope}</li>
 * </ul>
 *
 * <h3>Layout integration</h3>
 * <p>
 * The effect uses {@link UIStyles#positionAbsolute()} to take the widget out of
 * the Taffy flow layout, then installs a {@code LayoutHandler} via
 * {@link Widget#onLayout}.  During each {@code applyLayout()} pass the handler
 * reads the Taffy-computed size, runs the floating positioning engine, converts
 * the scene-space result to a parent-relative position, and writes it back into
 * the scope.  No {@code postLayout} loop or manual lifecycle management is needed.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares.*;
 *
 * Widget reference = ButtonWidget.of("Hover me", () -> {});
 * Widget tooltip = LabelWidget.of("Tooltip content");
 *
 * tooltip.useEffect(FloatingEffect.create(reference, FloatingPlacement.bottom,
 *     offset(8),
 *     flip(),
 *     shift(4)
 * ));
 * }</pre>
 *
 * @see FloatingPositioning
 * @see FloatingMiddleware
 * @see FloatingMiddlewares
 */
public final class FloatingEffect implements Effect {

    //region state

    private final Widget reference;
    private final FloatingPlacement placement;
    private final List<FloatingMiddleware> middleware;

    /**
     * The last computed position result, available for external reading.
     */
    private @Nullable FloatingPositioning.PositionResult lastResult;

    //endregion

    //region factory

    /**
     * Creates a floating effect that positions the target widget relative to the reference.
     *
     * @param reference  the reference widget to anchor to
     * @param placement  the preferred placement
     * @param middleware ordered middleware to apply (nulls are filtered out)
     * @return the effect
     */
    public static FloatingEffect create(Widget reference, FloatingPlacement placement, FloatingMiddleware... middleware) {
        return new FloatingEffect(reference, placement,
            Arrays.stream(middleware).filter(m -> m != null).toList());
    }

    /**
     * Creates a floating effect that positions the target widget relative to the reference.
     *
     * @param reference  the reference widget to anchor to
     * @param placement  the preferred placement
     * @param middleware ordered middleware to apply
     * @return the effect
     */
    public static FloatingEffect create(Widget reference, FloatingPlacement placement, List<FloatingMiddleware> middleware) {
        return new FloatingEffect(reference, placement, middleware);
    }

    private FloatingEffect(Widget reference, FloatingPlacement placement, List<FloatingMiddleware> middleware) {
        this.reference = reference;
        this.placement = placement;
        this.middleware = middleware;
    }

    //endregion

    //region accessors

    /**
     * @return the last computed position result, or null if not yet computed
     */
    public @Nullable FloatingPositioning.PositionResult lastResult() {
        return lastResult;
    }

    /**
     * Reads a value from the last middleware data.
     *
     * @param middlewareName the middleware name (e.g. "offset", "flip", "arrow")
     * @param key           the data key
     * @return the value, or null
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T data(String middlewareName, String key) {
        if (lastResult == null) return null;
        Map<String, Object> data = lastResult.middlewareData().get(middlewareName);
        if (data == null) return null;
        return (T) data.get(key);
    }

    //endregion

    @Override
    public void apply(Widget widget) {
        widget.useStyle(UIStyles.positionAbsolute());
        widget.onLayout(this::resolveLayout);
    }

    //region layout resolution

    private void resolveLayout(Widget widget, LayoutScope scope) {
        scope.useTaffy();

        // Skip if reference or floating has no meaningful size yet
        if (reference.width() <= 0 || reference.height() <= 0) return;
        if (scope.width() <= 0 || scope.height() <= 0) return;

        // Reference rect in scene space
        Pos refPos = reference.absolutePos();
        Rect referenceRect = new Rect(refPos.x(), refPos.y(), reference.width(), reference.height());

        // Floating rect — only size matters, position is what we compute
        Rect floatingRect = new Rect(0, 0, (int) scope.width(), (int) scope.height());

        // Boundary: root widget bounds (typically the full screen)
        var root = widget.scene().root();
        Rect boundary = new Rect(0, 0, root.width(), root.height());

        // Run the positioning engine
        FloatingPositioning.PositionResult result = FloatingPositioning.compute(
            referenceRect, floatingRect, boundary,
            placement, middleware
        );
        lastResult = result;

        // Convert scene-space result to parent-relative layout position
        var parent = widget.parent();
        if (parent != null) {
            FloatPos parentLocal = parent.sceneToLocal(result.x(), result.y());
            scope.setPosition(
                (float) (parentLocal.x + parent.viewport().contentOffsetX()),
                (float) (parentLocal.y + parent.viewport().contentOffsetY())
            );
        } else {
            scope.setPosition((float) result.x(), (float) result.y());
        }
    }

    //endregion
}
