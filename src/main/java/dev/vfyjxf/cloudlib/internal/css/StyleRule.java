/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import java.util.List;

/**
 * A qualified rule {@code selector-list { declaration-list }}, katana's {@code KatanaStyleRule}.
 */
public record StyleRule(List<ComplexSelector> selectors, List<Declaration> declarations) implements Rule {

    public StyleRule {
        selectors = List.copyOf(selectors);
        declarations = List.copyOf(declarations);
    }
}
