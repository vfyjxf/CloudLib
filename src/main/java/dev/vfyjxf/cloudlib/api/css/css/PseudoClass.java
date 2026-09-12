/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

/**
 * A pseudo-class or pseudo-element component of a compound selector.
 *
 * @param name the pseudo name, lowercased (without colons)
 * @param element true for a pseudo-element: anything written with {@code ::}, plus the four
 *     legacy single-colon pseudo-elements {@code before}, {@code after}, {@code first-line} and
 *     {@code first-letter} (katana's "compat" set)
 * @param args the parsed argument, per the pseudo's grammar
 */
public record PseudoClass(String name, boolean element, PseudoArgs args) {}
