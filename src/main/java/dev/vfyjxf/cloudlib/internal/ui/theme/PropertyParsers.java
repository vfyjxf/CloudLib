package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.AlignContentProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.AlignItemsProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.AlignSelfProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.AspectRatioProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.BorderProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.BoxSizingProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.DirectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.DisplayProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexBasisProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexDirectionProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexGrowProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexShrinkProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.FlexWrapProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.GapProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.InsetProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.ItemIsReplacedProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.ItemIsTableProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.JustifyContentProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.JustifyItemsProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.JustifySelfProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.MarginProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.OverflowProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.PaddingProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.PositionTypeProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.ScrollbarWidthProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.SizeConstraintProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.SizeProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.TextAlignProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.BackgroundProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.IconProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.ShadowProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.TextColorProperty;
import dev.vfyjxf.cloudlib.api.ui.style.property.visual.ZIndexProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.GradientTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.SpriteTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TiledTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.internal.css.ComponentValue;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The CSS property-name → {@link StyleProperty} table.
 * <p>
 * Every entry converts a resolved component-value list into a property via the
 * same factory surface {@code UIStyles} exposes. Unknown properties and values
 * outside the taffy vocabulary are reported to the warning sink and dropped.
 * <p>
 * Aliases normalize the two naming styles: CSS kebab-case wins, camelCase
 * ({@code textColor}, {@code zIndex}) is accepted for symmetry with the code API.
 */
public final class PropertyParsers {

    // region table

    /** Warn sink — the loader funnels these into the theme parse log. */
    public record Context(String themeId, String selector, Consumer<String> warn) {}

    @FunctionalInterface
    public interface Parser {
        @Nullable
        StyleProperty parse(Context ctx, List<ComponentValue> values);
    }

    private static final Map<String, Parser> table = new java.util.LinkedHashMap<>();

    static {
        registerEdges();
        registerBoxModel();
        registerFlex();
        registerGrid();
        registerVisual();
    }

    public static @Nullable StyleProperty parse(Context ctx, String property, List<ComponentValue> values) {
        Parser parser = table.get(normalize(property));
        if (parser == null) {
            ctx.warn().accept("unknown property '" + property + "'");
            return null;
        }
        List<ComponentValue> flat = values.stream()
                .filter(c -> c != ComponentValue.Whitespace.instance)
                .toList();
        if (flat.isEmpty()) {
            return null;
        }
        if (flat.size() == 1
                && flat.get(0) instanceof ComponentValue.Ident id
                && id.value().equalsIgnoreCase("inherit")) {
            // resolved upstream by the cascade — nothing to emit here
            return null;
        }
        try {
            return parser.parse(ctx, flat);
        } catch (RuntimeException e) {
            ctx.warn().accept("invalid value for '" + property + "': " + e.getMessage());
            return null;
        }
    }

    /** Kebab + camel both map to the same key. */
    private static String normalize(String property) {
        return property.toLowerCase(Locale.ROOT);
    }

    private static void reg(String name, Parser parser) {
        table.put(name, parser);
    }

    // endregion

    // region helpers
    private static @Nullable ComponentValue single(List<ComponentValue> values) {
        return values.size() == 1 ? values.get(0) : null;
    }

    private static @Nullable LengthPercentage lp(ComponentValue v) {
        return ThemeValues.lengthPercentage(v);
    }

    private static @Nullable LengthPercentageAuto lpa(ComponentValue v) {
        return ThemeValues.lengthAuto(v);
    }

    private static @Nullable TaffyDimension dim(ComponentValue v) {
        return ThemeValues.dimension(v);
    }

    private static @Nullable Integer color(ComponentValue v) {
        return ThemeValues.color(v);
    }

    private static <E extends Enum<E>> @Nullable E enumValue(ComponentValue v, Class<E> type, Map<String, E> aliases) {
        if (v instanceof ComponentValue.Ident id) {
            E hit = aliases.get(id.value().toLowerCase(Locale.ROOT));
            if (hit != null) {
                return hit;
            }
            try {
                return Enum.valueOf(type, id.value().replace('-', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static Map<String, TaffyDisplay> display = Map.of(
            "block",
            TaffyDisplay.BLOCK,
            "flex",
            TaffyDisplay.FLEX,
            "grid",
            TaffyDisplay.GRID,
            "none",
            TaffyDisplay.NONE);

    private static Map<String, TaffyPosition> position =
            Map.of("relative", TaffyPosition.RELATIVE, "absolute", TaffyPosition.ABSOLUTE);

    private static Map<String, Overflow> overflow = Map.of(
            "visible", Overflow.VISIBLE, "clip", Overflow.CLIP, "hidden", Overflow.HIDDEN, "scroll", Overflow.SCROLL);

    private static Map<String, BoxSizing> boxSizing =
            Map.of("border-box", BoxSizing.BORDER_BOX, "content-box", BoxSizing.CONTENT_BOX);

    private static Map<String, FlexDirection> flexDirection = Map.of(
            "row",
            FlexDirection.ROW,
            "row-reverse",
            FlexDirection.ROW_REVERSE,
            "column",
            FlexDirection.COLUMN,
            "column-reverse",
            FlexDirection.COLUMN_REVERSE);

    private static Map<String, FlexWrap> flexWrap = Map.of(
            "nowrap",
            FlexWrap.NO_WRAP,
            "no-wrap",
            FlexWrap.NO_WRAP,
            "wrap",
            FlexWrap.WRAP,
            "wrap-reverse",
            FlexWrap.WRAP_REVERSE);

    private static Map<String, AlignItems> alignItems = Map.ofEntries(
            Map.entry("flex-start", AlignItems.FLEX_START),
            Map.entry("start", AlignItems.FLEX_START),
            Map.entry("flex-end", AlignItems.FLEX_END),
            Map.entry("end", AlignItems.FLEX_END),
            Map.entry("center", AlignItems.CENTER),
            Map.entry("baseline", AlignItems.BASELINE),
            Map.entry("stretch", AlignItems.STRETCH));

    private static Map<String, AlignContent> alignContent = Map.ofEntries(
            Map.entry("flex-start", AlignContent.FLEX_START),
            Map.entry("start", AlignContent.FLEX_START),
            Map.entry("flex-end", AlignContent.FLEX_END),
            Map.entry("end", AlignContent.FLEX_END),
            Map.entry("center", AlignContent.CENTER),
            Map.entry("stretch", AlignContent.STRETCH),
            Map.entry("space-between", AlignContent.SPACE_BETWEEN),
            Map.entry("space-around", AlignContent.SPACE_AROUND),
            Map.entry("space-evenly", AlignContent.SPACE_EVENLY));

    private static Map<String, JustifyContent> justifyContent = Map.ofEntries(
            Map.entry("flex-start", JustifyContent.FLEX_START),
            Map.entry("start", JustifyContent.FLEX_START),
            Map.entry("flex-end", JustifyContent.FLEX_END),
            Map.entry("end", JustifyContent.FLEX_END),
            Map.entry("center", JustifyContent.CENTER),
            Map.entry("stretch", JustifyContent.STRETCH),
            Map.entry("space-between", JustifyContent.SPACE_BETWEEN),
            Map.entry("space-around", JustifyContent.SPACE_AROUND),
            Map.entry("space-evenly", JustifyContent.SPACE_EVENLY));

    private static Map<String, TextAlign> textAlign = Map.of(
            "auto",
            TextAlign.AUTO,
            "left",
            TextAlign.LEFT,
            "right",
            TextAlign.RIGHT,
            "center",
            TextAlign.CENTER,
            "justify",
            TextAlign.JUSTIFY,
            "justify-all",
            TextAlign.JUSTIFY_ALL,
            "start",
            TextAlign.START,
            "end",
            TextAlign.END);

    private static Map<String, TaffyDirection> direction = Map.of(
            "inherit",
            TaffyDirection.INHERIT,
            "ltr",
            TaffyDirection.LTR,
            "rtl",
            TaffyDirection.RTL,
            "default",
            TaffyDirection.DEFAULT);

    // endregion

    // region edge boxes
    /** padding/margin/border/inset and their physical sides + logical halves. */
    private static void registerEdges() {
        edgeFamily("padding", (rect, mask, type) -> paddingProp(rect, mask, type));
        edgeFamily("margin", (rect, mask, type) -> marginProp(rect, mask, type));
        edgeFamily("border", (rect, mask, type) -> borderProp(rect, mask, type));
        edgeFamily("inset", (rect, mask, type) -> insetProp(rect, mask, type));
    }

    private interface EdgeFactory {
        @Nullable
        StyleProperty make(List<ComponentValue> edges, String side, Context ctx);
    }

    private static void edgeFamily(String base, EdgeFactory factory) {
        String[] sides = {"", "-top", "-right", "-bottom", "-left", "-horizontal", "-vertical"};
        for (String side : sides) {
            reg(base + side, (ctx, values) -> factory.make(values, side, ctx));
        }
    }

    private static @Nullable StyleProperty paddingProp(List<ComponentValue> values, String side, Context ctx) {
        List<LengthPercentage> edges = edgeValues(values, ThemeValues::lengthPercentage);
        if (edges == null) return null;
        return switch (side) {
            case "" ->
                switch (edges.size()) {
                    case 1 -> new PaddingProperty(edges.get(0));
                    case 2 -> new PaddingProperty(edges.get(0), edges.get(1));
                    case 3 -> PaddingProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(1));
                    default -> PaddingProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(3));
                };
            case "-top" -> PaddingProperty.top(edges.get(0));
            case "-right" -> PaddingProperty.right(edges.get(0));
            case "-bottom" -> PaddingProperty.bottom(edges.get(0));
            case "-left" -> PaddingProperty.left(edges.get(0));
            case "-horizontal" -> PaddingProperty.horizontal(edges.get(0));
            default -> PaddingProperty.vertical(edges.get(0));
        };
    }

    private static @Nullable StyleProperty marginProp(List<ComponentValue> values, String side, Context ctx) {
        List<LengthPercentageAuto> edges = edgeValues(values, ThemeValues::lengthAuto);
        if (edges == null) return null;
        return switch (side) {
            case "" ->
                switch (edges.size()) {
                    case 1 -> new MarginProperty(edges.get(0));
                    case 2 -> new MarginProperty(edges.get(0), edges.get(1));
                    case 3 -> MarginProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(1));
                    default -> MarginProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(3));
                };
            case "-top" -> MarginProperty.top(edges.get(0));
            case "-right" -> MarginProperty.right(edges.get(0));
            case "-bottom" -> MarginProperty.bottom(edges.get(0));
            case "-left" -> MarginProperty.left(edges.get(0));
            case "-horizontal" -> MarginProperty.horizontal(edges.get(0));
            default -> MarginProperty.vertical(edges.get(0));
        };
    }

    private static @Nullable StyleProperty borderProp(List<ComponentValue> values, String side, Context ctx) {
        List<LengthPercentage> edges = edgeValues(values, ThemeValues::lengthPercentage);
        if (edges == null) return null;
        return switch (side) {
            case "" ->
                switch (edges.size()) {
                    case 1 -> new BorderProperty(edges.get(0));
                    case 2 -> new BorderProperty(edges.get(0), edges.get(1));
                    case 3 -> BorderProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(1));
                    default -> BorderProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(3));
                };
            case "-top" -> BorderProperty.top(edges.get(0));
            case "-right" -> BorderProperty.right(edges.get(0));
            case "-bottom" -> BorderProperty.bottom(edges.get(0));
            case "-left" -> BorderProperty.left(edges.get(0));
            case "-horizontal" -> BorderProperty.horizontal(edges.get(0));
            default -> BorderProperty.vertical(edges.get(0));
        };
    }

    private static @Nullable StyleProperty insetProp(List<ComponentValue> values, String side, Context ctx) {
        List<LengthPercentageAuto> edges = edgeValues(values, ThemeValues::lengthAuto);
        if (edges == null) return null;
        return switch (side) {
            case "" ->
                switch (edges.size()) {
                    case 1 -> new InsetProperty(edges.get(0));
                    case 2 -> new InsetProperty(edges.get(0), edges.get(1));
                    case 3 -> InsetProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(1));
                    default -> InsetProperty.of(edges.get(0), edges.get(1), edges.get(2), edges.get(3));
                };
            case "-top" -> InsetProperty.top(edges.get(0));
            case "-right" -> InsetProperty.right(edges.get(0));
            case "-bottom" -> InsetProperty.bottom(edges.get(0));
            case "-left" -> InsetProperty.left(edges.get(0));
            case "-horizontal" -> InsetProperty.horizontal(edges.get(0));
            default -> InsetProperty.vertical(edges.get(0));
        };
    }

    private interface EdgeValue<T> {
        @Nullable
        T convert(ComponentValue v);
    }

    private static <T> @Nullable List<T> edgeValues(List<ComponentValue> values, EdgeValue<T> fn) {
        if (values.isEmpty() || values.size() > 4) {
            return null;
        }
        List<T> out = new ArrayList<>(values.size());
        for (ComponentValue v : values) {
            T t = fn.convert(v);
            if (t == null) {
                return null;
            }
            out.add(t);
        }
        return out;
    }

    // endregion

    // region box model
    private static void registerBoxModel() {
        reg("display", (ctx, v) -> {
            TaffyDisplay d = enumValue(single(v), TaffyDisplay.class, display);
            return d != null ? new DisplayProperty(d) : null;
        });
        reg("position", (ctx, v) -> {
            TaffyPosition p = enumValue(single(v), TaffyPosition.class, position);
            return p != null ? new PositionTypeProperty(p) : null;
        });
        reg("box-sizing", (ctx, v) -> {
            BoxSizing b = enumValue(single(v), BoxSizing.class, boxSizing);
            return b != null ? new BoxSizingProperty(b) : null;
        });
        reg("overflow", (ctx, v) -> {
            List<ComponentValue> flat = v;
            Overflow x = enumValue(flat.get(0), Overflow.class, overflow);
            if (x == null) return null;
            Overflow y = flat.size() > 1 ? enumValue(flat.get(1), Overflow.class, overflow) : x;
            return y == null ? null : new OverflowProperty(x, y);
        });
        reg("overflow-x", (ctx, v) -> {
            Overflow o = enumValue(single(v), Overflow.class, overflow);
            return o != null ? new OverflowProperty(o, null) : null;
        });
        reg("overflow-y", (ctx, v) -> {
            Overflow o = enumValue(single(v), Overflow.class, overflow);
            return o != null ? new OverflowProperty(null, o) : null;
        });
        reg("width", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? new SizeProperty(d, TaffyDimension.AUTO) : null;
        });
        reg("height", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? new SizeProperty(TaffyDimension.AUTO, d) : null;
        });
        reg("size", (ctx, v) -> {
            TaffyDimension w = dim(v.get(0));
            if (w == null) return null;
            TaffyDimension h = v.size() > 1 ? dim(v.get(1)) : w;
            return h == null ? null : new SizeProperty(w, h);
        });
        reg("min-width", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? SizeConstraintProperty.minWidth(d) : null;
        });
        reg("min-height", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? SizeConstraintProperty.minHeight(d) : null;
        });
        reg("max-width", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? SizeConstraintProperty.maxWidth(d) : null;
        });
        reg("max-height", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? SizeConstraintProperty.maxHeight(d) : null;
        });
        reg("min-size", (ctx, v) -> {
            TaffyDimension w = dim(v.get(0));
            TaffyDimension h = v.size() > 1 ? dim(v.get(1)) : w;
            return w == null || h == null ? null : SizeConstraintProperty.minSize(w, h);
        });
        reg("max-size", (ctx, v) -> {
            TaffyDimension w = dim(v.get(0));
            TaffyDimension h = v.size() > 1 ? dim(v.get(1)) : w;
            return w == null || h == null ? null : SizeConstraintProperty.maxSize(w, h);
        });
        reg("aspect-ratio", (ctx, v) -> {
            if (v.size() == 1 && v.get(0) instanceof ComponentValue.NumericValue n) {
                return new AspectRatioProperty((float) n.value());
            }
            // "w / h" form
            if (v.size() == 3
                    && v.get(0) instanceof ComponentValue.NumericValue w
                    && v.get(2) instanceof ComponentValue.NumericValue h
                    && v.get(1) instanceof ComponentValue.Delim d
                    && d.value() == '/') {
                return h.value() != 0 ? new AspectRatioProperty((float) (w.value() / h.value())) : null;
            }
            return null;
        });
        reg("gap", (ctx, v) -> {
            LengthPercentage row = lp(v.get(0));
            if (row == null) return null;
            LengthPercentage col = v.size() > 1 ? lp(v.get(1)) : row;
            return col == null ? null : new GapProperty(row, col);
        });
        reg("row-gap", (ctx, v) -> {
            LengthPercentage g = lp(single(v));
            return g != null ? new GapProperty(g, null) : null;
        });
        reg("column-gap", (ctx, v) -> {
            LengthPercentage g = lp(single(v));
            return g != null ? new GapProperty(null, g) : null;
        });
        reg("scrollbar-width", (ctx, v) -> {
            Float w = ThemeValues.lengthPx(single(v));
            return w != null ? new ScrollbarWidthProperty(w) : null;
        });
        reg("item-is-replaced", (ctx, v) -> bool(v) != null ? new ItemIsReplacedProperty(bool(v)) : null);
        reg("item-is-table", (ctx, v) -> bool(v) != null ? new ItemIsTableProperty(bool(v)) : null);
    }

    private static @Nullable Boolean bool(List<ComponentValue> v) {
        if (single(v) instanceof ComponentValue.Ident id) {
            return switch (id.value().toLowerCase(Locale.ROOT)) {
                case "true", "yes", "on" -> true;
                case "false", "no", "off" -> false;
                default -> null;
            };
        }
        return null;
    }

    // endregion

    // region flex
    private static void registerFlex() {
        reg("flex", (ctx, v) -> {
            // flex: <grow> [<shrink> [<basis>]] | none | auto | initial
            if (v.size() == 1 && v.get(0) instanceof ComponentValue.Ident id) {
                return switch (id.value().toLowerCase(Locale.ROOT)) {
                    case "none", "initial" -> new FlexProperty(0f);
                    case "auto" -> new FlexProperty(1f); // flex:auto = 1 1 auto
                    default -> null;
                };
            }
            if (v.get(0) instanceof ComponentValue.NumericValue n && n.kind() == ComponentValue.NumericKind.number) {
                return new FlexProperty((float) n.value());
            }
            return null;
        });
        reg("flex-grow", (ctx, v) -> number(v) != null ? new FlexGrowProperty(number(v)) : null);
        reg("flex-shrink", (ctx, v) -> number(v) != null ? new FlexShrinkProperty(number(v)) : null);
        reg("flex-basis", (ctx, v) -> {
            TaffyDimension d = dim(single(v));
            return d != null ? new FlexBasisProperty(d) : null;
        });
        reg("flex-direction", (ctx, v) -> {
            FlexDirection d = enumValue(single(v), FlexDirection.class, flexDirection);
            return d != null ? new FlexDirectionProperty(d) : null;
        });
        reg("flex-wrap", (ctx, v) -> {
            FlexWrap w = enumValue(single(v), FlexWrap.class, flexWrap);
            return w != null ? new FlexWrapProperty(w) : null;
        });
        reg("flex-flow", (ctx, v) -> {
            // shorthand — split into direction + wrap
            FlexDirection d = null;
            FlexWrap w = null;
            for (ComponentValue c : v) {
                if (d == null) d = enumValue(c, FlexDirection.class, flexDirection);
                if (w == null) w = enumValue(c, FlexWrap.class, flexWrap);
            }
            if (d == null && w == null) return null;
            List<StyleProperty> props = new ArrayList<>();
            // emit as a composite via a wrapper that applies both
            FlexDirection fd = d;
            FlexWrap fw = w;
            return new StyleProperty() {
                @Override
                public dev.vfyjxf.cloudlib.api.ui.style.StyleType<?> type() {
                    return FlexDirectionProperty.type;
                }

                @Override
                public void apply(dev.vfyjxf.cloudlib.api.ui.style.StyleContext context) {
                    if (fd != null) new FlexDirectionProperty(fd).apply(context);
                    if (fw != null) new FlexWrapProperty(fw).apply(context);
                }
            };
        });
        reg("align-items", (ctx, v) -> {
            AlignItems a = enumValue(single(v), AlignItems.class, alignItems);
            return a != null ? new AlignItemsProperty(a) : null;
        });
        reg("align-self", (ctx, v) -> {
            AlignItems a = enumValue(single(v), AlignItems.class, alignItems);
            return a != null ? new AlignSelfProperty(a) : null;
        });
        reg("align-content", (ctx, v) -> {
            AlignContent a = enumValue(single(v), AlignContent.class, alignContent);
            return a != null ? new AlignContentProperty(a) : null;
        });
        reg("justify-content", (ctx, v) -> {
            JustifyContent j = enumValue(single(v), JustifyContent.class, justifyContent);
            return j != null ? new JustifyContentProperty(j) : null;
        });
        reg("justify-items", (ctx, v) -> {
            AlignItems a = enumValue(single(v), AlignItems.class, alignItems);
            return a != null ? new JustifyItemsProperty(a) : null;
        });
        reg("justify-self", (ctx, v) -> {
            AlignItems a = enumValue(single(v), AlignItems.class, alignItems);
            return a != null ? new JustifySelfProperty(a) : null;
        });
        reg("text-align", (ctx, v) -> {
            TextAlign t = enumValue(single(v), TextAlign.class, textAlign);
            return t != null ? new TextAlignProperty(t) : null;
        });
        reg("direction", (ctx, v) -> {
            TaffyDirection d = enumValue(single(v), TaffyDirection.class, direction);
            return d != null ? new DirectionProperty(d) : null;
        });
    }

    private static @Nullable Float number(List<ComponentValue> v) {
        if (single(v) instanceof ComponentValue.NumericValue n && n.kind() == ComponentValue.NumericKind.number) {
            return (float) n.value();
        }
        return null;
    }

    // endregion

    // region grid
    private static void registerGrid() {
        // grid template/track values are a large grammar of their own — the theme
        // layer accepts them but the detailed track parser lands with grid support
        reg("grid-auto-flow", (ctx, v) -> {
            if (v.get(0) instanceof ComponentValue.Ident id) {
                GridAutoFlow flow =
                        switch (id.value().toLowerCase(Locale.ROOT)) {
                            case "row" -> GridAutoFlow.ROW;
                            case "column" -> GridAutoFlow.COLUMN;
                            case "dense", "row-dense" -> GridAutoFlow.ROW_DENSE;
                            case "column-dense" -> GridAutoFlow.COLUMN_DENSE;
                            default -> null;
                        };
                return flow != null
                        ? new dev.vfyjxf.cloudlib.api.ui.style.property.layout.GridAutoFlowProperty(flow)
                        : null;
            }
            return null;
        });
    }

    // endregion

    // region visual
    private static void registerVisual() {
        reg("background", (ctx, v) -> {
            VisualTexture tex = texture(single(v));
            if (tex != null) return new BackgroundProperty(tex);
            Integer c = color(single(v));
            return c != null ? new BackgroundProperty(new ColorTexture(c)) : null;
        });
        reg("background-color", (ctx, v) -> {
            Integer c = color(single(v));
            return c != null ? new BackgroundProperty(new ColorTexture(c)) : null;
        });
        reg("icon", (ctx, v) -> {
            VisualTexture tex = texture(single(v));
            return tex != null ? new IconProperty(tex) : null;
        });
        reg("color", (ctx, v) -> {
            Integer c = color(single(v));
            return c != null ? new TextColorProperty(c) : null;
        });
        reg("text-color", (ctx, v) -> {
            Integer c = color(single(v));
            return c != null ? new TextColorProperty(c) : null;
        });
        reg("textcolor", (ctx, v) -> {
            Integer c = color(single(v));
            return c != null ? new TextColorProperty(c) : null;
        });
        reg("z-index", (ctx, v) -> {
            Integer n = integer(v);
            return n != null ? new ZIndexProperty(n) : null;
        });
        reg("zindex", (ctx, v) -> {
            Integer n = integer(v);
            return n != null ? new ZIndexProperty(n) : null;
        });
        reg("shadow", (ctx, v) -> shadow(v));
        reg("box-shadow", (ctx, v) -> shadow(v));
        reg("border-color", (ctx, v) -> {
            Integer c = color(single(v));
            return c != null ? borderVisual(null, c) : null;
        });
        reg("border-width", (ctx, v) -> {
            Float w = ThemeValues.lengthPx(single(v));
            return w != null ? borderVisual(w, null) : null;
        });
        reg("opacity", (ctx, v) -> {
            Float f = number(v);
            return f != null ? alphaVisual(f) : null;
        });
    }

    private static @Nullable Integer integer(List<ComponentValue> v) {
        if (single(v) instanceof ComponentValue.NumericValue n && n.integer()) {
            return (int) n.value();
        }
        return null;
    }

    private static @Nullable StyleProperty shadow(List<ComponentValue> v) {
        // box-shadow: [inset]? <off-x> <off-y> [blur] [color]
        List<ComponentValue> flat = v;
        if (flat.isEmpty()) return null;
        if (flat.size() == 1
                && flat.get(0) instanceof ComponentValue.Ident id
                && id.value().equalsIgnoreCase("none")) {
            return ShadowProperty.none();
        }
        List<Float> lengths = new ArrayList<>();
        Integer color = null;
        for (ComponentValue c : flat) {
            Float l = ThemeValues.lengthPx(c);
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
        return new ShadowProperty(ox, oy, blur, color != null ? color : 0x80000000);
    }

    // endregion

    // region textures
    /**
     * Texture function values — the theme extension vocabulary:
     * {@code nine-slice(loc,border[,w,h])}, {@code sprite(loc,w,h)}, {@code tiled(loc,w,h)},
     * {@code color(argb)}, {@code linear-gradient(c1,c2[,vertical])},
     * {@code border-texture(color,thickness)}.
     */
    public static @Nullable VisualTexture texture(ComponentValue v) {
        if (!(v instanceof ComponentValue.Function fn)) {
            return null;
        }
        List<ComponentValue> args = fn.args().stream()
                .filter(c -> c != ComponentValue.Whitespace.instance
                        && !(c instanceof ComponentValue.Delim d && d.value() == ','))
                .toList();
        return switch (fn.name().toLowerCase(Locale.ROOT)) {
            case "nine-slice" -> nineSlice(args);
            case "sprite" -> sprite(args);
            case "tiled" -> tiled(args);
            case "color" -> {
                Integer c = args.isEmpty() ? null : color(args.get(0));
                yield c != null ? new ColorTexture(c) : null;
            }
            case "linear-gradient" -> gradient(args);
            case "border-texture" -> borderTexture(args);
            default -> null;
        };
    }

    private static @Nullable ResourceLocation location(ComponentValue v) {
        if (v instanceof ComponentValue.StringValue s) {
            return ResourceLocation.tryParse(s.value());
        }
        if (v instanceof ComponentValue.UrlValue u) {
            return ResourceLocation.tryParse(u.value());
        }
        if (v instanceof ComponentValue.Ident id) {
            return ResourceLocation.tryParse(id.value());
        }
        return null;
    }

    private static @Nullable Float num(ComponentValue v) {
        if (v instanceof ComponentValue.NumericValue n) {
            return (float) n.value();
        }
        return null;
    }

    private static @Nullable VisualTexture nineSlice(List<ComponentValue> args) {
        if (args.isEmpty()) return null;
        ResourceLocation loc = location(args.get(0));
        if (loc == null) return null;
        // nine-slice(loc, border) | nine-slice(loc, border, w, h)
        Float border = args.size() > 1 ? num(args.get(1)) : null;
        if (border == null) return null;
        if (args.size() >= 4) {
            Float w = num(args.get(2));
            Float h = num(args.get(3));
            if (w == null || h == null) return null;
            return NineSliceTexture.of(loc, w.intValue(), h.intValue(), border.intValue());
        }
        // default: 18x18 cell with given border (matches the bundled assets)
        return NineSliceTexture.of(loc, 18, 18, border.intValue());
    }

    private static @Nullable VisualTexture sprite(List<ComponentValue> args) {
        if (args.isEmpty()) return null;
        ResourceLocation loc = location(args.get(0));
        if (loc == null) return null;
        if (args.size() == 1) {
            return SpriteTexture.fromGuiSprite(loc); // intrinsic size from the atlas
        }
        Float w = num(args.get(1));
        Float h = args.size() > 2 ? num(args.get(2)) : null;
        if (w == null || h == null) return null;
        return SpriteTexture.fromGuiSprite(loc, w.intValue(), h.intValue());
    }

    private static @Nullable VisualTexture tiled(List<ComponentValue> args) {
        if (args.size() < 3) return null;
        ResourceLocation loc = location(args.get(0));
        Float w = num(args.get(1));
        Float h = num(args.get(2));
        if (loc == null || w == null || h == null) return null;
        return TiledTexture.sprite(loc, w.intValue(), h.intValue());
    }

    private static @Nullable VisualTexture gradient(List<ComponentValue> args) {
        // linear-gradient(c1, c2 [, vertical|horizontal])
        if (args.size() < 2) return null;
        Integer c1 = color(args.get(0));
        Integer c2 = color(args.get(1));
        if (c1 == null || c2 == null) return null;
        boolean vertical = args.size() > 2
                && args.get(2) instanceof ComponentValue.Ident id
                && id.value().equalsIgnoreCase("vertical");
        return vertical ? GradientTexture.vertical(c1, c2) : GradientTexture.horizontal(c1, c2);
    }

    private static @Nullable VisualTexture borderTexture(List<ComponentValue> args) {
        if (args.size() < 2) return null;
        Integer c = color(args.get(0));
        Float thickness = num(args.get(1));
        if (c == null || thickness == null) return null;
        return BorderTexture.of(c, thickness.intValue());
    }

    // endregion

    // region visual border/opacity
    /** Visual border: width+color merged onto {@code VisualContext.border}. */
    private static StyleProperty borderVisual(@Nullable Float width, @Nullable Integer color) {
        return new VisualProperty() {
            @Override
            public dev.vfyjxf.cloudlib.api.ui.style.StyleType<?> type() {
                return BorderVisualType.type;
            }

            @Override
            public void applyToWidget(dev.vfyjxf.cloudlib.api.ui.style.VisualContext context) {
                float w = width != null ? width : context.borderWidth();
                int c = color != null ? color : context.borderColor();
                context.border(w, c);
            }
        };
    }

    private static StyleProperty alphaVisual(float alpha) {
        return new VisualProperty() {
            @Override
            public dev.vfyjxf.cloudlib.api.ui.style.StyleType<?> type() {
                return BorderVisualType.type;
            }

            @Override
            public void applyToWidget(dev.vfyjxf.cloudlib.api.ui.style.VisualContext context) {
                context.setProperty("opacity", alpha);
            }
        };
    }

    /** Synthetic type ids for theme-only visual setters. */
    private static final class BorderVisualType {
        static final dev.vfyjxf.cloudlib.api.ui.style.StyleType<Object> type =
                dev.vfyjxf.cloudlib.api.ui.style.StyleType.visual("theme-visual", () -> null);
    }
    // endregion
}
