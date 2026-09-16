package dev.vfyjxf.cloudlib.api.css;

import java.util.List;
import java.util.Locale;

/**
 * A component-value list — the raw value of a css declaration, and the value
 * type of css custom properties ({@code --*}).
 * <p>
 * Custom properties keep their value as the preserved token stream (whitespace
 * included) so they round-trip like css; {@link var()} references inside are
 * substituted during style resolution, not at parse time.
 * <p>
 * {@link #of(String)} tokenizes css source; {@link #toString()} serializes back
 * to css text — the two are the read/write pair for both theme files and the
 * java-side variable api.
 */
public final class Tokens {

    public static final Tokens empty = new Tokens(List.of());

    private final List<ComponentValue> values;
    private String text;

    private Tokens(List<ComponentValue> values) {
        this.values = values;
    }

    /** Tokenizes css source into a component-value list. */
    public static Tokens of(String cssSource) {
        return new Tokens(CssParser.parseValueList(cssSource));
    }

    /** Wraps an already-parsed component-value list. */
    public static Tokens of(List<ComponentValue> values) {
        if (values.isEmpty()) {
            return empty;
        }
        return new Tokens(List.copyOf(values));
    }

    public List<ComponentValue> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** The css text form — serialization is cached. */
    public String text() {
        if (text == null) {
            text = serialize(values);
        }
        return text;
    }

    @Override
    public String toString() {
        return text();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tokens other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    // region serialization — CSS Syntax L3 §serialization, simplified

    public static String serialize(List<ComponentValue> values) {
        StringBuilder out = new StringBuilder();
        for (ComponentValue v : values) {
            append(out, v);
        }
        return out.toString();
    }

    private static void append(StringBuilder out, ComponentValue v) {
        switch (v) {
            case ComponentValue.Ident i -> out.append(i.value());
            case ComponentValue.AtKeyword a -> out.append('@').append(a.value());
            case ComponentValue.Function f -> {
                out.append(f.name()).append('(');
                for (ComponentValue arg : f.args()) {
                    append(out, arg);
                }
                out.append(')');
            }
            case ComponentValue.NumericValue n -> {
                String raw = n.raw();
                out.append(raw != null ? raw : number(n.value(), n.integer())).append(n.unit());
            }
            case ComponentValue.StringValue s -> {
                out.append('"');
                for (int i = 0; i < s.value().length(); i++) {
                    char c = s.value().charAt(i);
                    switch (c) {
                        case '"', '\\' -> out.append('\\').append(c);
                        case '\n' -> out.append("\\a ");
                        default -> out.append(c);
                    }
                }
                out.append('"');
            }
            case ComponentValue.HashValue h -> out.append('#').append(h.value());
            case ComponentValue.UrlValue u ->
                out.append("url(").append(u.value()).append(')');
            case ComponentValue.UnicodeRange r -> {
                out.append("U+").append(Integer.toHexString(r.start()).toUpperCase(Locale.ROOT));
                if (r.end() != r.start()) {
                    out.append('-').append(Integer.toHexString(r.end()).toUpperCase(Locale.ROOT));
                }
            }
            case ComponentValue.Delim d -> out.append(d.value());
            case ComponentValue.Block b -> {
                out.append(
                        switch (b.kind()) {
                            case curly -> '{';
                            case square -> '[';
                            case paren -> '(';
                        });
                for (ComponentValue inner : b.values()) {
                    append(out, inner);
                }
                out.append(
                        switch (b.kind()) {
                            case curly -> '}';
                            case square -> ']';
                            case paren -> ')';
                        });
            }
            case ComponentValue.Whitespace w -> out.append(' ');
            case ComponentValue.Cdo c -> out.append("<!--");
            case ComponentValue.Cdc c -> out.append("-->");
        }
    }

    private static String number(double value, boolean integer) {
        return integer ? Long.toString((long) value) : Double.toString(value);
    }

    // endregion
}
