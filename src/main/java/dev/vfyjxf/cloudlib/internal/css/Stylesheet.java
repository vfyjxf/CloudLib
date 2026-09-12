/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import java.util.List;

/** The parsed stylesheet — katana's {@code KatanaOutput} minus the C-mode details. */
public record Stylesheet(List<Rule> rules) {

    public Stylesheet {
        rules = List.copyOf(rules);
    }
}
