/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

import org.jspecify.annotations.Nullable;

/**
 * An attribute selector: {@code [name]}, {@code [ns|name op "value" i]}.
 *
 * <p>Semantics of {@code namespace}: {@code null} when no prefix was written, {@code ""} for an
 * empty prefix ({@code [|name]} — "no namespace"), {@code "*"} for the any-namespace prefix, and the
 * literal prefix otherwise.
 */
public record AttributeSelector(
    @Nullable String namespace,
    String name,
    @Nullable Operator operator,
    @Nullable String value,
    @Nullable MatchFlag flag
) {

    /** The match operator, mirroring {@code KatanaSelectorMatchAttribute*}. */
    public enum Operator {
        /** {@code =} */
        exact,
        /** {@code ~=} */
        includes,
        /** {@code |=} */
        dashMatch,
        /** {@code ^=} */
        prefixMatch,
        /** {@code $=} */
        suffixMatch,
        /** {@code *=} */
        substringMatch
    }

    /** The case-sensitivity modifier after the value, mirroring {@code KatanaAttributeMatchType}. */
    public enum MatchFlag {
        /** {@code i} — ASCII case-insensitive. */
        insensitive,
        /** {@code s} — case-sensitive. */
        sensitive
    }
}
