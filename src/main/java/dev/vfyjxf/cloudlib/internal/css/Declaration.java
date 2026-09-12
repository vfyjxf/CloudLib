/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import java.util.List;

/**
 * A {@code property: value} pair — katana's {@code KatanaDeclaration}.
 *
 * @param property the property name, lowercased (custom-property names keep their case —
 *     {@code --foo} is case-sensitive per spec)
 * @param value the raw component values (whitespace preserved, {@code !important} stripped)
 * @param important whether {@code !important} was present
 */
public record Declaration(String property, List<ComponentValue> value, boolean important) {

    public Declaration {
        value = List.copyOf(value);
    }
}
