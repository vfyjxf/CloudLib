package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParser;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ident → enum constant tables for css parsing.
 * <p>
 * Alias maps carry the kebab-case css vocabulary; the
 * {@code Enum.valueOf(kebab→UPPER_SNAKE)} fallback in {@link #enumValue} covers
 * anything not aliased.
 */
public final class CssEnums {

    private CssEnums() {}

    // region helpers

    /** First non-whitespace token of a declaration value. */
    public static @Nullable ComponentValue single(List<ComponentValue> values) {
        for (ComponentValue v : values) {
            if (v != ComponentValue.Whitespace.instance) {
                return v;
            }
        }
        return null;
    }

    /** Declaration values without whitespace tokens. */
    public static List<ComponentValue> flat(List<ComponentValue> values) {
        return values.stream()
                .filter(c -> c != ComponentValue.Whitespace.instance)
                .toList();
    }

    public static @Nullable <E extends Enum<E>> E enumValue(
            @Nullable ComponentValue v, Class<E> type, Map<String, E> aliases) {
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

    /** Single-token enum parser for a {@link StyleKey}. */
    public static <E extends Enum<E>> StyleParser<E> parser(Class<E> type, Map<String, E> aliases) {
        return (values, ctx) -> {
            E hit = enumValue(single(values), type, aliases);
            if (hit == null) {
                ctx.warn("invalid enum value for " + type.getSimpleName());
            }
            return hit;
        };
    }

    // endregion

    // region tables

    public static final Map<String, TaffyDisplay> display = Map.of(
            "block", TaffyDisplay.BLOCK,
            "flex", TaffyDisplay.FLEX,
            "grid", TaffyDisplay.GRID,
            "none", TaffyDisplay.NONE);

    public static final Map<String, TaffyPosition> position =
            Map.of("relative", TaffyPosition.RELATIVE, "absolute", TaffyPosition.ABSOLUTE);

    public static final Map<String, Overflow> overflow = Map.of(
            "visible", Overflow.VISIBLE,
            "clip", Overflow.CLIP,
            "hidden", Overflow.HIDDEN,
            "scroll", Overflow.SCROLL);

    public static final Map<String, BoxSizing> boxSizing =
            Map.of("border-box", BoxSizing.BORDER_BOX, "content-box", BoxSizing.CONTENT_BOX);

    public static final Map<String, FlexDirection> flexDirection = Map.of(
            "row", FlexDirection.ROW,
            "row-reverse", FlexDirection.ROW_REVERSE,
            "column", FlexDirection.COLUMN,
            "column-reverse", FlexDirection.COLUMN_REVERSE);

    public static final Map<String, FlexWrap> flexWrap = Map.of(
            "nowrap", FlexWrap.NO_WRAP,
            "no-wrap", FlexWrap.NO_WRAP,
            "wrap", FlexWrap.WRAP,
            "wrap-reverse", FlexWrap.WRAP_REVERSE);

    public static final Map<String, AlignItems> alignItems = Map.ofEntries(
            Map.entry("flex-start", AlignItems.FLEX_START),
            Map.entry("start", AlignItems.FLEX_START),
            Map.entry("flex-end", AlignItems.FLEX_END),
            Map.entry("end", AlignItems.FLEX_END),
            Map.entry("center", AlignItems.CENTER),
            Map.entry("baseline", AlignItems.BASELINE),
            Map.entry("stretch", AlignItems.STRETCH));

    public static final Map<String, AlignContent> alignContent = Map.ofEntries(
            Map.entry("flex-start", AlignContent.FLEX_START),
            Map.entry("start", AlignContent.FLEX_START),
            Map.entry("flex-end", AlignContent.FLEX_END),
            Map.entry("end", AlignContent.FLEX_END),
            Map.entry("center", AlignContent.CENTER),
            Map.entry("stretch", AlignContent.STRETCH),
            Map.entry("space-between", AlignContent.SPACE_BETWEEN),
            Map.entry("space-around", AlignContent.SPACE_AROUND),
            Map.entry("space-evenly", AlignContent.SPACE_EVENLY));

    public static final Map<String, JustifyContent> justifyContent = Map.ofEntries(
            Map.entry("flex-start", JustifyContent.FLEX_START),
            Map.entry("start", JustifyContent.FLEX_START),
            Map.entry("flex-end", JustifyContent.FLEX_END),
            Map.entry("end", JustifyContent.FLEX_END),
            Map.entry("center", JustifyContent.CENTER),
            Map.entry("stretch", JustifyContent.STRETCH),
            Map.entry("space-between", JustifyContent.SPACE_BETWEEN),
            Map.entry("space-around", JustifyContent.SPACE_AROUND),
            Map.entry("space-evenly", JustifyContent.SPACE_EVENLY));

    public static final Map<String, TextAlign> textAlign = Map.of(
            "auto", TextAlign.AUTO,
            "left", TextAlign.LEFT,
            "right", TextAlign.RIGHT,
            "center", TextAlign.CENTER,
            "justify", TextAlign.JUSTIFY,
            "justify-all", TextAlign.JUSTIFY_ALL,
            "start", TextAlign.START,
            "end", TextAlign.END);

    public static final Map<String, TaffyDirection> direction = Map.of(
            "inherit", TaffyDirection.INHERIT,
            "ltr", TaffyDirection.LTR,
            "rtl", TaffyDirection.RTL,
            "default", TaffyDirection.DEFAULT);

    // endregion

    /** taffy quirk — {@code TaffyStyle.justifyContent} is {@link AlignContent}-typed. */
    public static AlignContent toAlignContent(JustifyContent justify) {
        return switch (justify) {
            case FLEX_START -> AlignContent.FLEX_START;
            case FLEX_END -> AlignContent.FLEX_END;
            case CENTER -> AlignContent.CENTER;
            case SPACE_BETWEEN -> AlignContent.SPACE_BETWEEN;
            case SPACE_AROUND -> AlignContent.SPACE_AROUND;
            case SPACE_EVENLY -> AlignContent.SPACE_EVENLY;
            case START -> AlignContent.START;
            case END -> AlignContent.END;
            case STRETCH -> AlignContent.STRETCH;
        };
    }

    // region scalar helpers

    public static @Nullable Float number(List<ComponentValue> values) {
        ComponentValue v = single(values);
        if (v instanceof ComponentValue.NumericValue n && n.kind() == ComponentValue.NumericKind.number) {
            return (float) n.value();
        }
        return null;
    }

    public static @Nullable Integer integer(List<ComponentValue> values) {
        ComponentValue v = single(values);
        if (v instanceof ComponentValue.NumericValue n && n.integer()) {
            return (int) n.value();
        }
        return null;
    }

    public static @Nullable Boolean bool(List<ComponentValue> values) {
        if (single(values) instanceof ComponentValue.Ident id) {
            return switch (id.value().toLowerCase(Locale.ROOT)) {
                case "true", "yes", "on" -> true;
                case "false", "no", "off" -> false;
                default -> null;
            };
        }
        return null;
    }

    /** Single-token scalar parser for a {@link StyleKey}. */
    public static StyleParser<Float> floatParser(String name) {
        return (values, ctx) -> {
            Float f = number(values);
            if (f == null) {
                ctx.warn("invalid number for " + name);
            }
            return f;
        };
    }

    public static StyleParser<Integer> intParser(String name) {
        return (values, ctx) -> {
            Integer n = integer(values);
            if (n == null) {
                ctx.warn("invalid integer for " + name);
            }
            return n;
        };
    }

    public static StyleParser<Boolean> boolParser(String name) {
        return (values, ctx) -> {
            Boolean b = bool(values);
            if (b == null) {
                ctx.warn("invalid boolean for " + name);
            }
            return b;
        };
    }

    // endregion
}
