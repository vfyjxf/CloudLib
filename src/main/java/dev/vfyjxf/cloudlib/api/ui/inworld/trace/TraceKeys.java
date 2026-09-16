package dev.vfyjxf.cloudlib.api.ui.inworld.trace;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The trace chrome's style vocabulary — one {@link StyleVar} lens per
 * themeable knob. The lenses carry no state; {@link TraceStyle} reads the
 * resolved token streams off the {@code UIStyle} vars table. Values live
 * entirely in the theme css (e.g. {@code trace-chrome { --nimbus-*: … }}); a
 * missing declaration resolves to zero/empty rather than a hidden constant.
 */
public final class TraceKeys {

    private TraceKeys() {}

    // region colors

    /** Leader stroke between a panel and its world source. */
    public static final StyleVar<Integer> line = StyleVar.color("--nimbus-line");

    /** Leader stroke of the focused panel. */
    public static final StyleVar<Integer> lineFocused = StyleVar.color("--nimbus-line-focused");

    /** Dark underlay under the leader stroke. */
    public static final StyleVar<Integer> lineEdge = StyleVar.color("--nimbus-line-edge");

    /** Source-pairing hues — a comma-separated color list. */
    public static final StyleVar<int[]> srcPalette =
            StyleVar.of("--nimbus-src-palette", TraceKeys::palette, TraceKeys::paletteText);

    // endregion

    // region metrics — plain numbers and px dimensions both parse

    /** Leader-stroke core half-width in screen px. */
    public static final StyleVar<Float> lineW = floatVar("--nimbus-line-w");

    /** Leader-stroke outline half-width in screen px. */
    public static final StyleVar<Float> lineEdgeW = floatVar("--nimbus-line-edge-w");

    /** End-marker radius in screen px. */
    public static final StyleVar<Float> nodeR = floatVar("--nimbus-node-r");

    /** Connector half-width in blocks. */
    public static final StyleVar<Float> connW = floatVar("--nimbus-conn-w");

    /** Connector outline half-width in blocks. */
    public static final StyleVar<Float> connEdgeW = floatVar("--nimbus-conn-edge-w");

    /** Connector node radius in blocks. */
    public static final StyleVar<Float> connNodeR = floatVar("--nimbus-conn-node-r");

    /** Leader-line cap before color-pairing takes over. */
    public static final StyleVar<Integer> maxLeaders =
            StyleVar.of("--nimbus-max-leaders", TraceKeys::integer, Object::toString);

    /** World source-mark diamond radius in blocks. */
    public static final StyleVar<Float> srcMarkR = floatVar("--nimbus-src-mark-r");

    /** Source-mark stem half-width in blocks. */
    public static final StyleVar<Float> srcMarkW = floatVar("--nimbus-src-mark-w");

    /** Source-icon chip side length in px. */
    public static final StyleVar<Integer> srcIcon =
            StyleVar.of("--nimbus-src-icon", TraceKeys::integer, Object::toString);

    // endregion

    // region var builders & parsers

    private static StyleVar<Float> floatVar(String name) {
        return StyleVar.of(name, TraceKeys::number, Objects::toString);
    }

    /** First {@code <number>} token → float; units are accepted and ignored. */
    private static @Nullable Float number(List<ComponentValue> values, StyleParseContext ctx) {
        for (ComponentValue v : values) {
            if (v instanceof ComponentValue.Whitespace) continue;
            if (v instanceof ComponentValue.NumericValue n) return (float) n.value();
            ctx.warn("expected <number>, got " + v);
            return null;
        }
        return null;
    }

    private static @Nullable Integer integer(List<ComponentValue> values, StyleParseContext ctx) {
        Float n = number(values, ctx);
        return n == null ? null : n.intValue();
    }

    /** Every color hash in the value list → int[]; empty list is invalid. */
    private static int @Nullable [] palette(List<ComponentValue> values, StyleParseContext ctx) {
        List<Integer> out = new ArrayList<>();
        for (ComponentValue v : values) {
            if (v instanceof ComponentValue.HashValue h) {
                Integer c = parseHash(h.value(), ctx);
                if (c != null) out.add(c);
            }
        }
        if (out.isEmpty()) {
            ctx.warn("empty color list for palette");
            return null;
        }
        int[] p = new int[out.size()];
        for (int i = 0; i < p.length; i++) p[i] = out.get(i);
        return p;
    }

    /** {@code #RRGGBB} / {@code #RRGGBBAA} → ARGB int. */
    private static @Nullable Integer parseHash(String hex, StyleParseContext ctx) {
        try {
            if (hex.length() == 6) {
                return 0xFF000000 | (int) Long.parseLong(hex, 16);
            }
            if (hex.length() == 8) {
                long n = Long.parseLong(hex, 16);
                return (int) ((n & 0xFF) << 24 | n >> 8);
            }
        } catch (NumberFormatException ignored) {
        }
        ctx.warn("unparseable <color> #" + hex);
        return null;
    }

    private static String paletteText(int[] palette) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < palette.length; i++) {
            if (i > 0) sb.append(", ");
            int c = palette[i];
            int alpha = c >>> 24;
            sb.append(alpha == 0xFF ? "#%06X".formatted(c & 0xFFFFFF) : "#%06X%02X".formatted(c & 0xFFFFFF, alpha));
        }
        return sb.toString();
    }

    // endregion
}
