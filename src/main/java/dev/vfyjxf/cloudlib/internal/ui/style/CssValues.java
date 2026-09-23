package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.ui.style.Shadow;
import dev.vfyjxf.taffy.style.CalcExpression;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts parsed {@link ComponentValue}s into taffy/CloudLib value objects.
 * <p>
 * The supported value grammar is deliberately exactly what taffy-java can express:
 * lengths are {@code px} only, percentages are fractions, {@code calc()} supports
 * the {@code percent ± length}, {@code percent × n}, and {@code 100% ± px} shapes
 * taffy models — anything else returns null and the caller drops the declaration.
 */
public final class CssValues {

    private CssValues() {}

    // region lengths
    /** {@code <length>} — only {@code px} (and unitless zero) are valid for taffy. */
    public static @Nullable Float lengthPx(ComponentValue v) {
        if (v instanceof ComponentValue.NumericValue n) {
            return switch (n.kind()) {
                case dimension -> n.unit().equalsIgnoreCase("px") ? (float) n.value() : null;
                case number -> n.value() == 0 ? 0f : null; // unitless zero only
                case percentage -> null;
            };
        }
        return null;
    }

    /** {@code <length-percentage>} — px, %, or a supported calc() shape. */
    public static @Nullable LengthPercentage lengthPercentage(ComponentValue v) {
        if (v instanceof ComponentValue.NumericValue n) {
            return switch (n.kind()) {
                case dimension -> n.unit().equalsIgnoreCase("px") ? LengthPercentage.length((float) n.value()) : null;
                case percentage -> LengthPercentage.percent((float) (n.value() / 100.0));
                case number -> n.value() == 0 ? LengthPercentage.ZERO : null;
            };
        }
        if (v instanceof ComponentValue.Function fn && fn.name().equalsIgnoreCase("calc")) {
            CalcExpression calc = calc(fn.args());
            return calc != null ? LengthPercentage.calc(calc) : null;
        }
        return null;
    }

    /** {@code <length-percentage> | auto | intrinsic sizes}. */
    public static @Nullable LengthPercentageAuto lengthAuto(ComponentValue v) {
        if (v instanceof ComponentValue.Ident id) {
            return switch (id.value().toLowerCase(Locale.ROOT)) {
                case "auto" -> LengthPercentageAuto.AUTO;
                case "min-content" -> LengthPercentageAuto.MIN_CONTENT;
                case "max-content" -> LengthPercentageAuto.MAX_CONTENT;
                case "fit-content" -> LengthPercentageAuto.FIT_CONTENT;
                case "stretch" -> LengthPercentageAuto.STRETCH;
                default -> null;
            };
        }
        LengthPercentage lp = lengthPercentage(v);
        return lp != null ? LengthPercentageAuto.from(lp) : null;
    }

    /** {@code <dimension>} — a full taffy dimension (px/%/calc/auto/intrinsics/stretch). */
    public static @Nullable TaffyDimension dimension(ComponentValue v) {
        if (v instanceof ComponentValue.Ident id) {
            return switch (id.value().toLowerCase(Locale.ROOT)) {
                case "auto" -> TaffyDimension.AUTO;
                case "min-content" -> TaffyDimension.MIN_CONTENT;
                case "max-content" -> TaffyDimension.MAX_CONTENT;
                case "fit-content" -> TaffyDimension.FIT_CONTENT;
                case "stretch" -> TaffyDimension.STRETCH;
                default -> null;
            };
        }
        if (v instanceof ComponentValue.Function fn && fn.name().equalsIgnoreCase("calc")) {
            CalcExpression calc = calc(fn.args());
            return calc != null ? TaffyDimension.calc(calc) : null;
        }
        LengthPercentage lp = lengthPercentage(v);
        return lp != null ? TaffyDimension.from(lp) : null;
    }

    /**
     * {@code calc()} restricted to taffy's {@link CalcExpression} shapes:
     * {@code % ± px}, {@code % × n}, {@code 100% ± px}, {@code % + %}.
     */
    public static @Nullable CalcExpression calc(List<ComponentValue> args) {
        List<ComponentValue> flat = args.stream().filter(c -> c != ComponentValue.Whitespace.instance).toList();
        if (flat.size() == 1 && flat.get(0) instanceof ComponentValue.NumericValue n) {
            return switch (n.kind()) {
                case percentage -> CalcExpression.percentMultipliedBy((float) (n.value() / 100.0), 1);
                case dimension ->
                    n.unit().equalsIgnoreCase("px") ? CalcExpression.percentPlusLength(0, (float) n.value()) : null;
                case number -> n.value() == 0 ? CalcExpression.addPercents(0, 0) : null;
            };
        }
        if (flat.size() == 3
                && flat.get(1) instanceof ComponentValue.Delim op
                && (op.value() == '+' || op.value() == '-')) {
            ComponentValue lhs = flat.get(0);
            ComponentValue rhs = flat.get(2);
            boolean minus = op.value() == '-';
            if (lhs instanceof ComponentValue.NumericValue ln && rhs instanceof ComponentValue.NumericValue rn) {
                boolean lpct = ln.kind() == ComponentValue.NumericKind.percentage;
                boolean rpct = rn.kind() == ComponentValue.NumericKind.percentage;
                boolean lpx = ln.kind() == ComponentValue.NumericKind.dimension && ln.unit().equalsIgnoreCase("px");
                boolean rpx = rn.kind() == ComponentValue.NumericKind.dimension && rn.unit().equalsIgnoreCase("px");
                if (lpct && rpx) {
                    double p = ln.value() / 100.0;
                    return minus
                            ? (p == 1.0
                                    ? CalcExpression.fullMinusLength((float) rn.value())
                                    : CalcExpression.percentMinusLength((float) p, (float) rn.value()))
                            : (p == 1.0
                                    ? CalcExpression.fullPlusLength((float) rn.value())
                                    : CalcExpression.percentPlusLength((float) p, (float) rn.value()));
                }
                if (lpct && rpct) {
                    return minus
                            ? null
                            : CalcExpression.addPercents((float) (ln.value() / 100.0), (float) (rn.value() / 100.0));
                }
                if (lpx && rpct && !minus) {
                    return CalcExpression.percentPlusLength((float) (rn.value() / 100.0), (float) ln.value());
                }
            }
        }
        // % * n or % / n
        if (flat.size() == 3
                && flat.get(1) instanceof ComponentValue.Delim op
                && flat.get(0) instanceof ComponentValue.NumericValue pn
                && pn.kind() == ComponentValue.NumericKind.percentage
                && flat.get(2) instanceof ComponentValue.NumericValue num
                && num.kind() == ComponentValue.NumericKind.number) {
            double p = pn.value() / 100.0;
            return switch (op.value()) {
                case '*' -> CalcExpression.percentMultipliedBy((float) p, (float) num.value());
                case '/' -> CalcExpression.percentDividedBy((float) p, (float) num.value());
                default -> null;
            };
        }
        return null;
    }

    // endregion

    // region colors
    /** {@code <color>} — #hex, rgb()/rgba(), color(), or a CSS named color. */
    public static @Nullable Integer color(ComponentValue v) {
        if (v instanceof ComponentValue.HashValue h) {
            return parseHex(h.value());
        }
        if (v instanceof ComponentValue.Ident id) {
            Integer named = namedColors.get(id.value().toLowerCase(Locale.ROOT));
            if (named != null) {
                return named;
            }
            return switch (id.value().toLowerCase(Locale.ROOT)) {
                case "transparent" -> 0x00000000;
                case "currentcolor", "current-color" -> null; // resolved by caller context
                default -> null;
            };
        }
        if (v instanceof ComponentValue.Function fn) {
            return switch (fn.name().toLowerCase(Locale.ROOT)) {
                case "rgb", "rgba" -> rgb(fn.args());
                case "color" -> singleColorArg(fn.args());
                default -> null;
            };
        }
        return null;
    }

    /** The extension {@code color(<color>)} — identity wrapper for var()-held colors. */
    private static @Nullable Integer singleColorArg(List<ComponentValue> args) {
        return args.stream()
                .filter(c -> c != ComponentValue.Whitespace.instance && !(c instanceof ComponentValue.Delim))
                .findFirst().map(CssValues::color).orElse(null);
    }

    private static @Nullable Integer rgb(List<ComponentValue> args) {
        List<ComponentValue> flat = args.stream().filter(
            c -> c != ComponentValue.Whitespace.instance && !(c instanceof ComponentValue.Delim d && d.value() == ',')
        ).toList();
        // rgb(r g b / a) or rgb(r,g,b,a)
        double[] chan = new double[4];
        int i = 0;
        int aIndex = -1;
        for (int k = 0; k < flat.size(); k++) {
            ComponentValue c = flat.get(k);
            if (c instanceof ComponentValue.Delim d && d.value() == '/') {
                aIndex = k;
                continue;
            }
            if (c instanceof ComponentValue.NumericValue n && i < 4) {
                chan[i++] = n.kind() == ComponentValue.NumericKind.percentage ? n.value() * 255.0 / 100.0 : n.value();
            }
        }
        if (i < 3) {
            return null;
        }
        int r = (int) Math.round(chan[0]);
        int g = (int) Math.round(chan[1]);
        int b = (int) Math.round(chan[2]);
        int a = i >= 4 ? (int) Math.round(chan[3] <= 1.0 ? chan[3] * 255 : chan[3]) : 255;
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /**
     * {@code box-shadow: none | <off-x> <off-y> [blur] [color]} → a
     * {@link Shadow} value.
     */
    public static @Nullable Shadow shadow(List<ComponentValue> values) {
        List<ComponentValue> flat = values.stream().filter(c -> c != ComponentValue.Whitespace.instance).toList();
        if (flat.isEmpty()) return null;
        if (flat.size() == 1 && flat.get(0) instanceof ComponentValue.Ident id && id.value().equalsIgnoreCase("none")) {
            return Shadow.none;
        }
        List<Float> lengths = new ArrayList<>();
        Integer color = null;
        for (ComponentValue c : flat) {
            Float l = lengthPx(c);
            if (l != null) {
                lengths.add(l);
            } else {
                Integer col = color(c);
                if (col != null) color = col;
            }
        }
        if (lengths.size() < 2) return null;
        float ox = lengths.get(0), oy = lengths.get(1);
        float blur = lengths.size() > 2 ? lengths.get(2) : 0f;
        return new Shadow(ox, oy, blur, color != null ? color : 0x80000000);
    }

    /** {@code aspect-ratio: <n> | <n> / <n>} */
    public static @Nullable Float aspectRatio(List<ComponentValue> values) {
        List<ComponentValue> flat = values.stream().filter(c -> c != ComponentValue.Whitespace.instance).toList();
        if (flat.size() == 1 && flat.get(0) instanceof ComponentValue.NumericValue n) {
            return (float) n.value();
        }
        if (flat.size() == 3
                && flat.get(0) instanceof ComponentValue.NumericValue w
                && flat.get(2) instanceof ComponentValue.NumericValue h
                && flat.get(1) instanceof ComponentValue.Delim d
                && d.value() == '/') {
            return h.value() != 0 ? (float) (w.value() / h.value()) : null;
        }
        return null;
    }

    /**
     * {@code <grid-placement>} — {@code auto | <int> | span <int> | <name> [<int>]}.
     * Consumes a single token for auto/line, or the pair for span/named forms.
     */
    public static @Nullable GridPlacement placement(List<ComponentValue> values, int[] consumed) {
        List<ComponentValue> flat = values.stream().filter(c -> c != ComponentValue.Whitespace.instance).toList();
        if (flat.isEmpty()) return null;
        ComponentValue first = flat.get(0);
        if (first instanceof ComponentValue.Ident id) {
            String name = id.value();
            if (name.equalsIgnoreCase("auto")) {
                return GridPlacement.auto();
            }
            if (name.equalsIgnoreCase("span") && flat.size() > 1) {
                ComponentValue n = flat.get(1);
                if (n instanceof ComponentValue.NumericValue num && num.integer()) {
                    consumed[0] = 2;
                    return GridPlacement.span((int) num.value());
                }
                if (n instanceof ComponentValue.Ident name2) {
                    consumed[0] = 2;
                    return GridPlacement.namedSpan(name2.value(), 1);
                }
                return null;
            }
            // named line, optionally with index
            if (flat.size() > 1 && flat.get(1) instanceof ComponentValue.NumericValue num && num.integer()) {
                consumed[0] = 2;
                return GridPlacement.namedLine(name, (int) num.value());
            }
            return GridPlacement.namedLine(name);
        }
        if (first instanceof ComponentValue.NumericValue num && num.integer()) {
            return GridPlacement.line((int) num.value());
        }
        return null;
    }

    /** Single-token grid-placement parser (auto / line / named line). */
    public static @Nullable GridPlacement placement(List<ComponentValue> values) {
        return placement(values, new int[1]);
    }

    /** Parses {@code rgb}/{@code rgba}/{@code rrggbb}/{@code rrggbbaa} hex text. */
    public static @Nullable Integer parseHex(String hex) {
        int len = hex.length();
        try {
            return switch (len) {
                case 3 -> {
                    int v = Integer.parseInt(hex, 16);
                    int r = (v >> 8) & 0xF;
                    int g = (v >> 4) & 0xF;
                    int b = v & 0xF;
                    yield 0xFF000000 | (r * 0x11 << 16) | (g * 0x11 << 8) | b * 0x11;
                }
                case 4 -> {
                    int v = Integer.parseInt(hex, 16);
                    int r = (v >> 12) & 0xF;
                    int g = (v >> 8) & 0xF;
                    int b = (v >> 4) & 0xF;
                    int a = v & 0xF;
                    yield (a * 0x11 << 24) | (r * 0x11 << 16) | (g * 0x11 << 8) | b * 0x11;
                }
                case 6 -> 0xFF000000 | Integer.parseInt(hex, 16);
                case 8 -> {
                    // CSS #RRGGBBAA → Minecraft ARGB (alpha to the top byte)
                    long v = Long.parseLong(hex, 16);
                    yield (int) ((v & 0xFF) << 24 | (v >>> 8));
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** CSS named colors — the standard 148-keyword table. */
    private static final Map<String, Integer> namedColors = Map.ofEntries(
        Map.entry("black", 0xFF000000),
        Map.entry("silver", 0xFFC0C0C0),
        Map.entry("gray", 0xFF808080),
        Map.entry("white", 0xFFFFFFFF),
        Map.entry("maroon", 0xFF800000),
        Map.entry("red", 0xFFFF0000),
        Map.entry("purple", 0xFF800080),
        Map.entry("fuchsia", 0xFFFF00FF),
        Map.entry("green", 0xFF008000),
        Map.entry("lime", 0xFF00FF00),
        Map.entry("olive", 0xFF808000),
        Map.entry("yellow", 0xFFFFFF00),
        Map.entry("navy", 0xFF000080),
        Map.entry("blue", 0xFF0000FF),
        Map.entry("teal", 0xFF008080),
        Map.entry("aqua", 0xFF00FFFF),
        Map.entry("orange", 0xFFFFA500),
        Map.entry("aliceblue", 0xFFF0F8FF),
        Map.entry("antiquewhite", 0xFFFAEBD7),
        Map.entry("aquamarine", 0xFF7FFFD4),
        Map.entry("azure", 0xFFF0FFFF),
        Map.entry("beige", 0xFFF5F5DC),
        Map.entry("bisque", 0xFFFFE4C4),
        Map.entry("blanchedalmond", 0xFFFFEBCD),
        Map.entry("blueviolet", 0xFF8A2BE2),
        Map.entry("brown", 0xFFA52A2A),
        Map.entry("burlywood", 0xFFDEB887),
        Map.entry("cadetblue", 0xFF5F9EA0),
        Map.entry("chartreuse", 0xFF7FFF00),
        Map.entry("chocolate", 0xFFD2691E),
        Map.entry("coral", 0xFFFF7F50),
        Map.entry("cornflowerblue", 0xFF6495ED),
        Map.entry("cornsilk", 0xFFFFF8DC),
        Map.entry("crimson", 0xFFDC143C),
        Map.entry("cyan", 0xFF00FFFF),
        Map.entry("darkblue", 0xFF00008B),
        Map.entry("darkcyan", 0xFF008B8B),
        Map.entry("darkgoldenrod", 0xFFB8860B),
        Map.entry("darkgray", 0xFFA9A9A9),
        Map.entry("darkgreen", 0xFF006400),
        Map.entry("darkkhaki", 0xFFBDB76B),
        Map.entry("darkmagenta", 0xFF8B008B),
        Map.entry("darkolivegreen", 0xFF556B2F),
        Map.entry("darkorange", 0xFFFF8C00),
        Map.entry("darkorchid", 0xFF9932CC),
        Map.entry("darkred", 0xFF8B0000),
        Map.entry("darksalmon", 0xFFE9967A),
        Map.entry("darkseagreen", 0xFF8FBC8F),
        Map.entry("darkslateblue", 0xFF483D8B),
        Map.entry("darkslategray", 0xFF2F4F4F),
        Map.entry("darkturquoise", 0xFF00CED1),
        Map.entry("darkviolet", 0xFF9400D3),
        Map.entry("deeppink", 0xFFFF1493),
        Map.entry("deepskyblue", 0xFF00BFFF),
        Map.entry("dimgray", 0xFF696969),
        Map.entry("dodgerblue", 0xFF1E90FF),
        Map.entry("firebrick", 0xFFB22222),
        Map.entry("floralwhite", 0xFFFFFAF0),
        Map.entry("forestgreen", 0xFF228B22),
        Map.entry("gainsboro", 0xFFDCDCDC),
        Map.entry("ghostwhite", 0xFFF8F8FF),
        Map.entry("gold", 0xFFFFD700),
        Map.entry("goldenrod", 0xFFDAA520),
        Map.entry("greenyellow", 0xFFADFF2F),
        Map.entry("honeydew", 0xFFF0FFF0),
        Map.entry("hotpink", 0xFFFF69B4),
        Map.entry("indianred", 0xFFCD5C5C),
        Map.entry("indigo", 0xFF4B0082),
        Map.entry("ivory", 0xFFFFFFF0),
        Map.entry("khaki", 0xFFF0E68C),
        Map.entry("lavender", 0xFFE6E6FA),
        Map.entry("lavenderblush", 0xFFFFF0F5),
        Map.entry("lawngreen", 0xFF7CFC00),
        Map.entry("lemonchiffon", 0xFFFFFACD),
        Map.entry("lightblue", 0xFFADD8E6),
        Map.entry("lightcoral", 0xFFF08080),
        Map.entry("lightcyan", 0xFFE0FFFF),
        Map.entry("lightgoldenrodyellow", 0xFFFAFAD2),
        Map.entry("lightgray", 0xFFD3D3D3),
        Map.entry("lightgreen", 0xFF90EE90),
        Map.entry("lightpink", 0xFFFFB6C1),
        Map.entry("lightsalmon", 0xFFFFA07A),
        Map.entry("lightseagreen", 0xFF20B2AA),
        Map.entry("lightskyblue", 0xFF87CEFA),
        Map.entry("lightslategray", 0xFF778899),
        Map.entry("lightsteelblue", 0xFFB0C4DE),
        Map.entry("lightyellow", 0xFFFFFFE0),
        Map.entry("limegreen", 0xFF32CD32),
        Map.entry("linen", 0xFFFAF0E6),
        Map.entry("magenta", 0xFFFF00FF),
        Map.entry("mediumaquamarine", 0xFF66CDAA),
        Map.entry("mediumblue", 0xFF0000CD),
        Map.entry("mediumorchid", 0xFFBA55D3),
        Map.entry("mediumpurple", 0xFF9370DB),
        Map.entry("mediumseagreen", 0xFF3CB371),
        Map.entry("mediumslateblue", 0xFF7B68EE),
        Map.entry("mediumspringgreen", 0xFF00FA9A),
        Map.entry("mediumturquoise", 0xFF48D1CC),
        Map.entry("mediumvioletred", 0xFFC71585),
        Map.entry("midnightblue", 0xFF191970),
        Map.entry("mintcream", 0xFFF5FFFA),
        Map.entry("mistyrose", 0xFFFFE4E1),
        Map.entry("moccasin", 0xFFFFE4B5),
        Map.entry("navajowhite", 0xFFFFDEAD),
        Map.entry("oldlace", 0xFFFDF5E6),
        Map.entry("olivedrab", 0xFF6B8E23),
        Map.entry("orangered", 0xFFFF4500),
        Map.entry("orchid", 0xFFDA70D6),
        Map.entry("palegoldenrod", 0xFFEEE8AA),
        Map.entry("palegreen", 0xFF98FB98),
        Map.entry("paleturquoise", 0xFFAFEEEE),
        Map.entry("palevioletred", 0xFFDB7093),
        Map.entry("papayawhip", 0xFFFFEFD5),
        Map.entry("peachpuff", 0xFFFFDAB9),
        Map.entry("peru", 0xFFCD853F),
        Map.entry("pink", 0xFFFFC0CB),
        Map.entry("plum", 0xFFDDA0DD),
        Map.entry("powderblue", 0xFFB0E0E6),
        Map.entry("rosybrown", 0xFFBC8F8F),
        Map.entry("royalblue", 0xFF4169E1),
        Map.entry("saddlebrown", 0xFF8B4513),
        Map.entry("salmon", 0xFFFA8072),
        Map.entry("sandybrown", 0xFFF4A460),
        Map.entry("seagreen", 0xFF2E8B57),
        Map.entry("seashell", 0xFFFFF5EE),
        Map.entry("sienna", 0xFFA0522D),
        Map.entry("skyblue", 0xFF87CEEB),
        Map.entry("slateblue", 0xFF6A5ACD),
        Map.entry("slategray", 0xFF708090),
        Map.entry("snow", 0xFFFFFAFA),
        Map.entry("springgreen", 0xFF00FF7F),
        Map.entry("steelblue", 0xFF4682B4),
        Map.entry("tan", 0xFFD2B48C),
        Map.entry("thistle", 0xFFD8BFD8),
        Map.entry("tomato", 0xFFFF6347),
        Map.entry("turquoise", 0xFF40E0D0),
        Map.entry("violet", 0xFFEE82EE),
        Map.entry("wheat", 0xFFF5DEB3),
        Map.entry("whitesmoke", 0xFFF5F5F5),
        Map.entry("yellowgreen", 0xFF9ACD32),
        Map.entry("rebeccapurple", 0xFF663399)
    );
    // endregion
}
