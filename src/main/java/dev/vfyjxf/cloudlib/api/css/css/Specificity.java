/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.api.css;

/**
 * Selector specificity {@code (a, b, c)} per Selectors Level 3/4: id count, class/attribute/
 * pseudo-class count, type/pseudo-element count. Compares lexicographically.
 */
public record Specificity(int a, int b, int c) implements Comparable<Specificity> {

    public static final Specificity zero = new Specificity(0, 0, 0);

    public Specificity plus(Specificity other) {
        return new Specificity(a + other.a, b + other.b, c + other.c);
    }

    /** The more specific of the two — used for {@code :not()}/{@code :is()}/{@code :has()} args. */
    public Specificity max(Specificity other) {
        return compareTo(other) >= 0 ? this : other;
    }

    @Override
    public int compareTo(Specificity o) {
        if (a != o.a) {
            return Integer.compare(a, o.a);
        }
        if (b != o.b) {
            return Integer.compare(b, o.b);
        }
        return Integer.compare(c, o.c);
    }
}
