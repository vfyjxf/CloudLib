/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A compound selector: an optional type/universal part plus id, classes, attribute selectors and
 * pseudos. Specificity is precomputed and exposed both in katana's packed form
 * ({@code specA}/{@code specB}/{@code specC} fields, like {@code KatanaSelector::specificity}) and
 * as a {@link Specificity} value.
 *
 * @param tag the local name ({@code div}) or {@code "*"} for the universal selector;
 *     {@code null} when the compound has no type part at all
 * @param namespace {@code null} when no prefix was written, {@code ""} for {@code |name},
 *     {@code "*"} for {@code *|name}, or the literal prefix
 * @param id the id selector ({@code #id}) or {@code null}
 * @param classes class names ({@code .cls}) in order
 * @param attributes attribute selectors in order
 * @param pseudos pseudo-classes/-elements in order
 * @param specA specificity column a (ids)
 * @param specB specificity column b (classes, attributes, pseudo-classes)
 * @param specC specificity column c (types, pseudo-elements)
 */
public record CompoundSelector(
        @Nullable String tag,
        @Nullable String namespace,
        @Nullable String id,
        List<String> classes,
        List<AttributeSelector> attributes,
        List<PseudoClass> pseudos,
        int specA,
        int specB,
        int specC) {

    public CompoundSelector {
        classes = List.copyOf(classes);
        attributes = List.copyOf(attributes);
        pseudos = List.copyOf(pseudos);
    }

    public static CompoundSelector of(
            @Nullable String tag,
            @Nullable String namespace,
            @Nullable String id,
            List<String> classes,
            List<AttributeSelector> attributes,
            List<PseudoClass> pseudos,
            Specificity spec) {
        return new CompoundSelector(tag, namespace, id, classes, attributes, pseudos, spec.a(), spec.b(), spec.c());
    }

    public Specificity specificity() {
        return new Specificity(specA, specB, specC);
    }

    /** True when this compound is only a type/universal part with no other component. */
    public boolean isBareType() {
        return tag != null && id == null && classes.isEmpty() && attributes.isEmpty() && pseudos.isEmpty();
    }
}
