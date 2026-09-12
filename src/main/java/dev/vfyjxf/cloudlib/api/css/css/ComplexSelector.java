/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

import java.util.List;

/**
 * A complex selector: {@code compounds.size() == combinators.size() + 1}.
 * {@code compounds[i] <combinators[i]> compounds[i+1]} — the compound list runs left to right in
 * source order, i.e. {@code a > b ~ c} is {@code [a,b,c]} + {@code [child, subsequentSibling]},
 * matching katana's flattened {@code tagHistory} representation.
 */
public record ComplexSelector(List<CompoundSelector> compounds, List<Combinator> combinators) {

    public ComplexSelector {
        compounds = List.copyOf(compounds);
        combinators = List.copyOf(combinators);
        if (compounds.isEmpty() || compounds.size() != combinators.size() + 1) {
            throw new IllegalArgumentException("compounds must be non-empty and combinators must have one fewer entry");
        }
    }

    /** The rightmost compound — the part that matches the candidate element. */
    public CompoundSelector last() {
        return compounds.get(compounds.size() - 1);
    }

    /** Total specificity of the whole selector. */
    public Specificity specificity() {
        Specificity s = Specificity.zero;
        for (CompoundSelector c : compounds) {
            s = s.plus(c.specificity());
        }
        return s;
    }
}
