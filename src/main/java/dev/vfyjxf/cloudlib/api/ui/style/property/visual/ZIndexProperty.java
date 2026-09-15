package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;

/**
 * Visual property for setting the z-index of a widget.
 * <p>
 * Z-index controls the rendering order among sibling widgets (children of the same parent).
 * Lower values render first (appear behind), higher values render last (appear on top).
 * <p>
 * Note: Z-index only affects sibling ordering. Parent widgets always render before their children.
 */
public record ZIndexProperty(int zIndex) implements VisualProperty {

    //region types

    public static final StyleType<Integer> type = StyleType.of(
            "zIndex",
            () -> 0,
            (ctx, value) -> ctx.visualContext().setZIndex(value)
    );

    //endregion

    //region VisualProperty implementation

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void apply(StyleContext context) {
        // Use set() to trigger change listeners
        context.set(type, zIndex);
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setZIndex(zIndex);
    }

    //endregion

    @Override
    public String toString() {
        return "zIndex(" + zIndex + ")";
    }
}
