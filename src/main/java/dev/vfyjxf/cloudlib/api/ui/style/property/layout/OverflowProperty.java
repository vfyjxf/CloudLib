package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleContext;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Layout property that maps to taffy {@link TaffyStyle#overflow}.
 * <p>
 * In taffy overflow is stored as a 2D point: {@code x} and {@code y}.
 * This property uses a single {@link StyleType} and may update either axis independently.
 * <p>
 * The overflow values supported by taffy include:
 * <ul>
 *   <li>{@link Overflow#VISIBLE} - content is not clipped</li>
 *   <li>{@link Overflow#HIDDEN} - content is clipped without scrollbars</li>
 *   <li>{@link Overflow#CLIP} - content is clipped without scrollbars (same as HIDDEN in taffy)</li>
 *   <li>{@link Overflow#SCROLL} - content is clipped with scrollbars</li>
 * </ul>
 *
 * @see TaffyStyle#overflow
 * @see Overflow
 */
public record OverflowProperty(@Nullable Overflow x, @Nullable Overflow y) implements LayoutProperty {

    public static final StyleType<TaffyPoint<Overflow>> type = StyleType.of(
        "overflow",
        () -> TaffyPoint.all(Overflow.VISIBLE),
        (context, overflow) -> {
            context.layoutStyle().overflow.x = overflow.x;
            context.layoutStyle().overflow.y = overflow.y;
        }
    );

    @Override
    public StyleType<?> type() {
        return type;
    }

    /**
     * Sets overflow for both x and y.
     */
    public OverflowProperty(Overflow overflow) {
        this(requireNonNull(overflow, "overflow"), overflow);
    }

    /**
     * Sets overflow for both x and y from a taffy Point value.
     */
    public OverflowProperty(TaffyPoint<Overflow> overflow) {
        this(requireNonNull(overflow, "overflow").x, requireNonNull(overflow, "overflow").y);
    }

    @Override
    public void apply(StyleContext context) {
        TaffyPoint<Overflow> current = context.get(type);

        Overflow nextX = x != null ? x : current.x;
        Overflow nextY = y != null ? y : current.y;

        TaffyPoint<Overflow> merged = TaffyPoint.all(Overflow.VISIBLE);
        merged.x = nextX;
        merged.y = nextY;

        context.set(type, merged);
        context.applyGeneric(this);
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        if (x != null) {
            style.overflow.x = x;
        }
        if (y != null) {
            style.overflow.y = y;
        }
    }

    @Override
    public String toString() {
        String sx = x == null ? "<keep>" : x.toString();
        String sy = y == null ? "<keep>" : y.toString();
        return "x=" + sx + ", y=" + sy;
    }
}
