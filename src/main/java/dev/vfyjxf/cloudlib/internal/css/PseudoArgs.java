/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import java.util.List;

/**
 * The argument of a functional pseudo-class/pseudo-element, parsed per the grammar the spec
 * assigns to that pseudo.
 */
public sealed interface PseudoArgs {

    /** No arguments: {@code :hover}, {@code ::before}. */
    enum None implements PseudoArgs {
        instance
    }

    /**
     * An {@code An+B} argument for the {@code :nth-*} family.
     *
     * @param a the step
     * @param b the offset
     * @param of the optional {@code of <selector-list>} clause of {@code :nth-child()}/
     *     {@code :nth-last-child()} (Selectors Level 4); empty when absent
     */
    record AnPlusB(int a, int b, List<ComplexSelector> of) implements PseudoArgs {

        public AnPlusB {
            of = List.copyOf(of);
        }

        public AnPlusB(int a, int b) {
            this(a, b, List.of());
        }
    }

    /**
     * A selector-list argument: {@code :not()}, {@code :is()}, {@code :where()}, {@code :has()},
     * {@code :host()}, {@code :host-context()}, {@code ::slotted()} and friends. Members carry an
     * optional leading combinator so relative selectors ({@code :has(> a)}) round-trip.
     */
    record SelectorList(List<RelativeSelector> selectors) implements PseudoArgs {

        public SelectorList {
            selectors = List.copyOf(selectors);
        }
    }

    /**
     * An identifier/word argument list: {@code :lang(en, fr)}, {@code :dir(rtl)},
     * {@code ::part(name)}.
     */
    record Idents(List<String> values) implements PseudoArgs {

        public Idents {
            values = List.copyOf(values);
        }
    }

    /** Raw component values for any functional pseudo without a more specific grammar. */
    record Raw(List<ComponentValue> values) implements PseudoArgs {

        public Raw {
            values = List.copyOf(values);
        }
    }
}
