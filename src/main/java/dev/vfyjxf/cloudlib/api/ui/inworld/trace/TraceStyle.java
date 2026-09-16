package dev.vfyjxf.cloudlib.api.ui.inworld.trace;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.Themes;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import org.jetbrains.annotations.Nullable;

/**
 * Resolved trace chrome — the full theme pipeline applied to a phantom
 * {@code <trace-chrome>} node. Values come from
 * {@code Theme.resolve(node)} → cascade → the node's resolved {@code --*} vars
 * read through {@link TraceKeys}' {@link StyleVar} lenses, so themes style the
 * trace with the same machinery they style widgets with; anything a theme
 * doesn't declare resolves to zero/empty.
 * <p>
 * Snapshot: re-resolves whenever the active theme instance changes
 * ({@code Themes.setActive} / resource reload).
 */
public final class TraceStyle {

    /**
     * Phantom node the chrome resolves against — never mounted, never
     * rendered. Its class-derived styleTag is the selector themes use:
     * {@code trace-chrome { --nimbus-line: … }}.
     */
    private static final class TraceChrome extends Widget {}

    private static final Widget node = new TraceChrome();

    public final int line;
    public final int lineFocused;
    public final int lineEdge;
    public final int[] srcPalette;
    public final float lineW;
    public final float lineEdgeW;
    public final float nodeR;
    public final float connW;
    public final float connEdgeW;
    public final float connNodeR;
    public final int maxLeaders;
    public final float srcMarkR;
    public final float srcMarkW;
    public final int srcIcon;

    private TraceStyle(UIStyle style) {
        line = nz(read(style, TraceKeys.line));
        lineFocused = nz(read(style, TraceKeys.lineFocused));
        lineEdge = nz(read(style, TraceKeys.lineEdge));
        srcPalette = nz(read(style, TraceKeys.srcPalette));
        lineW = nz(read(style, TraceKeys.lineW));
        lineEdgeW = nz(read(style, TraceKeys.lineEdgeW));
        nodeR = nz(read(style, TraceKeys.nodeR));
        connW = nz(read(style, TraceKeys.connW));
        connEdgeW = nz(read(style, TraceKeys.connEdgeW));
        connNodeR = nz(read(style, TraceKeys.connNodeR));
        maxLeaders = nz(read(style, TraceKeys.maxLeaders));
        srcMarkR = nz(read(style, TraceKeys.srcMarkR));
        srcMarkW = nz(read(style, TraceKeys.srcMarkW));
        srcIcon = nz(read(style, TraceKeys.srcIcon));
    }

    private static <T> @Nullable T read(UIStyle style, StyleVar<T> var) {
        return style.var(var);
    }

    // lenses carry no fallback — an undeclared property means the theme never
    // styled the trace; zero/empty keeps us off the screen rather than
    // silently resurrecting a hardcoded look
    private static int nz(@Nullable Integer v) {
        return v == null ? 0 : v;
    }

    private static float nz(@Nullable Float v) {
        return v == null ? 0f : v;
    }

    private static int[] nz(int @Nullable [] v) {
        return v == null ? new int[0] : v;
    }

    // region theme snapshot

    private static @Nullable Theme seen;
    private static @Nullable TraceStyle cached;

    /** Style for the active theme — re-resolved only when the theme changes. */
    public static TraceStyle get() {
        Theme active = Themes.active();
        if (active != seen) {
            seen = active;
            UIStyle style = active == null ? UIStyle.empty : active.resolve(node);
            cached = new TraceStyle(style);
        }
        return cached;
    }

    // endregion
}
