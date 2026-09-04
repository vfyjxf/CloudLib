package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#display}.
 * <p>
 * The display modes supported by taffy include:
 * <ul>
 *   <li>{@link TaffyDisplay#DEFAULT} - default display mode</li>
 *   <li>{@link TaffyDisplay#NONE} - element is not rendered</li>
 *   <li>{@link TaffyDisplay#BLOCK} - block-level element</li>
 *   <li>{@link TaffyDisplay#FLEX} - flex container</li>
 *   <li>{@link TaffyDisplay#GRID} - grid container</li>
 * </ul>
 *
 * @see TaffyStyle#display
 * @see TaffyDisplay
 */
public record DisplayProperty(TaffyDisplay display) implements LayoutProperty {

    public static final StyleType<TaffyDisplay> type = StyleType.of("display", () -> TaffyDisplay.DEFAULT);

    public DisplayProperty(TaffyDisplay display) {
        this.display = Objects.requireNonNull(display, "display");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.display = display;
    }

    @Override
    public String toString() {
        return display.toString();
    }

}
