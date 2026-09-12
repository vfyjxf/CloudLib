/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

/**
 * A recoverable parse error. Parsers never throw on malformed input; they record an error here
 * (1-based line and column of the offending token) and continue at the next recovery point, mirroring
 * {@code KatanaError}.
 */
public record CssError(int line, int column, String message) {}
