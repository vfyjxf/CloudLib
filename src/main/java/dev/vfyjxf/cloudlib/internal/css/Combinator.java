/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

/**
 * A selector combinator, per Selectors Level 3/4 — katana's {@code KatanaSelectorRelation}.
 */
public enum Combinator {
    /** {@code A B} — descendant. */
    descendant,
    /** {@code A > B} — child. */
    child,
    /** {@code A + B} — next sibling. */
    nextSibling,
    /** {@code A ~ B} — subsequent sibling. */
    subsequentSibling,
    /** {@code A || B} — column (Selectors Level 4). */
    column
}
