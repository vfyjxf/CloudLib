package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.Shadow;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleApply;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffyPoint;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@link StyleApply} factories for the builtin keys.
 * <p>
 * Appliers write straight into {@code StyleContext}'s slots — taffy fields are
 * public mutable, so an edge-slot applier is a field write through a getter:
 * {@code edge("padding", left)} → {@code ctx.layoutStyle().padding.left = v}.
 */
public final class StyleApplies {

    private StyleApplies() {}

    // region generic writers

    /** Writes a value into a taffy style field. */
    public static <T> StyleApply<T> layout(BiConsumer<TaffyStyle, T> write) {
        return (ctx, v) -> write.accept(ctx.layoutStyle(), v);
    }

    /** Writes a value into the visual context. */
    public static <T> StyleApply<T> visual(BiConsumer<VisualContext, T> write) {
        return (ctx, v) -> write.accept(ctx.visualContext(), v);
    }

    /** Stores a value as a visual-context custom property under the key id. */
    public static <T> StyleApply<T> customProp(String name) {
        return (ctx, v) -> ctx.visualContext().setProperty(name, v);
    }

    // endregion

    // region geometry slots

    /** Applies to one edge of a {@link TaffyRect} field on the taffy style. */
    public static <T> StyleApply<T> edge(Function<TaffyStyle, TaffyRect<T>> rect, Edge e) {
        return (ctx, v) -> {
            TaffyRect<T> r = rect.apply(ctx.layoutStyle());
            switch (e) {
                case top -> r.top = v;
                case right -> r.right = v;
                case bottom -> r.bottom = v;
                case left -> r.left = v;
            }
        };
    }

    /** Applies to one slot of a {@link TaffySize} field on the taffy style. */
    public static <T> StyleApply<T> size(Function<TaffyStyle, TaffySize<T>> size, boolean width) {
        return (ctx, v) -> {
            TaffySize<T> s = size.apply(ctx.layoutStyle());
            if (width) {
                s.width = v;
            } else {
                s.height = v;
            }
        };
    }

    /** Applies to one slot of a {@link TaffyPoint} field on the taffy style. */
    public static <T> StyleApply<T> point(Function<TaffyStyle, TaffyPoint<T>> point, boolean x) {
        return (ctx, v) -> {
            TaffyPoint<T> p = point.apply(ctx.layoutStyle());
            if (x) {
                p.x = v;
            } else {
                p.y = v;
            }
        };
    }

    /** Applies to one slot of a {@link TaffyLine} field on the taffy style. */
    public static <T> StyleApply<T> line(Function<TaffyStyle, TaffyLine<T>> line, boolean start) {
        return (ctx, v) -> {
            TaffyLine<T> l = line.apply(ctx.layoutStyle());
            if (start) {
                l.start = v;
            } else {
                l.end = v;
            }
        };
    }

    // endregion

    // region composite visual setters

    /** Visual border: patches only the color, keeping current thickness. */
    public static final StyleApply<Integer> borderColor =
            (ctx, v) -> ctx.visualContext().border(ctx.visualContext().borderWidth(), v);

    /** Visual border: patches only the thickness, keeping current color. */
    public static final StyleApply<Float> borderWidth =
            (ctx, v) -> ctx.visualContext().border(v, ctx.visualContext().borderColor());

    /** Drop shadow → {@link VisualContext#setShadow}. */
    public static final StyleApply<Shadow> shadow =
            (ctx, v) -> ctx.visualContext().setShadow(v.offsetX(), v.offsetY(), v.blurRadius(), v.color());

    /** Scene layer → stored on the context (read by scene compositing). */
    public static final StyleApply<SceneLayer> sceneLayer = (ctx, v) -> {};

    // endregion
}
