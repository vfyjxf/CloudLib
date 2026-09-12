/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import java.util.List;

/**
 * A single CSS component value — the atom of declaration values, at-rule preludes and block
 * contents per CSS Syntax Module Level 3. This is the Java counterpart of katana's
 * {@code KatanaValue} (with {@code KatanaValueUnit}), flattened into a sealed type.
 *
 * <p>Whitespace is preserved as {@link Whitespace} values so that raw values (e.g. custom
 * properties) round-trip faithfully.
 */
public sealed interface ComponentValue {

    /** An {@code <ident-token>}: a CSS identifier, escapes already resolved. */
    record Ident(String value) implements ComponentValue {}

    /** An {@code <at-keyword-token>} appearing in a value position, e.g. {@code @foo}. */
    record AtKeyword(String value) implements ComponentValue {}

    /**
     * A {@code <function-token>} and its argument block: {@code name(} … {@code )}.
     * {@code args} are the raw component values between the parentheses (commas appear as
     * {@link Delim} values), mirroring {@code KatanaValueFunction}.
     */
    record Function(String name, List<ComponentValue> args) implements ComponentValue {

        public Function {
            args = List.copyOf(args);
        }
    }

    /**
     * A {@code <number-token>}, {@code <percentage-token>} or {@code <dimension-token>}.
     *
     * @param value the numeric value (sign and exponent already applied)
     * @param unit {@code ""} for a plain number, {@code "%"} for a percentage, the unit ident for a
     *     dimension
     * @param kind which numeric token kind this was
     * @param integer whether the token had integer type (no fraction or exponent)
     * @param raw the exact source text of the numeric part (not including the unit)
     */
    record NumericValue(double value, String unit, NumericKind kind, boolean integer, String raw)
            implements ComponentValue {

        /** True for a {@code <dimension-token>}. */
        public boolean isDimension() {
            return kind == NumericKind.dimension;
        }
    }

    enum NumericKind {
        number,
        percentage,
        dimension
    }

    /** A {@code <string-token>} (or a recovered {@code <bad-string-token>}): content, unquoted. */
    record StringValue(String value) implements ComponentValue {}

    /**
     * A {@code <hash-token>}: {@code #value}. {@code id} is the hash type flag — true when the
     * value would start an identifier ({@code #abc}), false for unrestricted hashes
     * ({@code #123}).
     */
    record HashValue(String value, boolean id) implements ComponentValue {}

    /** A {@code <url-token>} (or a recovered {@code <bad-url-token>}): {@code url(value)}. */
    record UrlValue(String value) implements ComponentValue {}

    /** A {@code <unicode-range-token>}: {@code U+0025-00FF}, {@code U+4??} and friends. */
    record UnicodeRange(int start, int end) implements ComponentValue {}

    /** A {@code <delim-token>}: any single code point not otherwise tokenized. */
    record Delim(char value) implements ComponentValue {}

    /**
     * A simple block — {@code {…}}, {@code […]} or {@code (…)} — with its component values.
     * Blocks are always balanced; unclosed blocks end at EOF per spec.
     */
    record Block(BlockKind kind, List<ComponentValue> values) implements ComponentValue {

        public Block {
            values = List.copyOf(values);
        }
    }

    enum BlockKind {
        curly,
        square,
        paren
    }

    /** A {@code <whitespace-token>}: any run of spaces, tabs and newlines, folded into one. */
    enum Whitespace implements ComponentValue {
        instance
    }

    /** A {@code <CDO-token>} ({@code <!--}) in a value position. */
    enum Cdo implements ComponentValue {
        instance
    }

    /** A {@code <CDC-token>} ({@code -->}) in a value position. */
    enum Cdc implements ComponentValue {
        instance
    }
}
