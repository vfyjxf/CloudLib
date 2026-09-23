/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

import org.jspecify.annotations.Nullable;

/**
 * A member of a selector list inside a functional pseudo. {@code combinator} is only non-null for
 * relative selector lists ({@code :has(> a)}); it is {@code null} elsewhere and in
 * {@code :not(a > b)} — the combinator lives inside {@code selector} there.
 */
public record RelativeSelector(@Nullable Combinator combinator, ComplexSelector selector) {}
