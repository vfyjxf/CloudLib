/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

/**
 * A stylesheet member: either a {@link StyleRule} (qualified rule) or an {@link AtRule},
 * mirroring katana's {@code KatanaStyleRule}/{@code KatanaImportRule}/… union.
 */
public sealed interface Rule permits StyleRule, AtRule {}
