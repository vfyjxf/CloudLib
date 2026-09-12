package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.theme.Themeable;
import dev.vfyjxf.cloudlib.internal.css.AttributeSelector;
import dev.vfyjxf.cloudlib.internal.css.Combinator;
import dev.vfyjxf.cloudlib.internal.css.ComplexSelector;
import dev.vfyjxf.cloudlib.internal.css.CompoundSelector;
import dev.vfyjxf.cloudlib.internal.css.PseudoArgs;
import dev.vfyjxf.cloudlib.internal.css.PseudoClass;
import dev.vfyjxf.cloudlib.internal.css.RelativeSelector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Matches {@link ComplexSelector}s against the {@link Themeable} tree.
 * <p>
 * Supported: type/universal, namespace prefixes (accepted and ignored — the widget
 * tree is flat-namespaced), {@code #id}, {@code .class}, all attribute operators,
 * descendant/child/sibling/column combinators, and pseudo-classes:
 * {@code hovered active inactive interactive non-interactive focused disabled enabled
 * checked selected pressed empty first-child last-child only-child nth-child
 * nth-last-child nth-of-type nth-last-of-type odd/even forms, root, not, is, where,
 * has} plus widget-defined custom states. Pseudo-elements resolve via
 * {@link Themeable#themePart()} ({@code ::part(name)}) or widget-defined part tags.
 */
public final class SelectorMatcher {

    private SelectorMatcher() {}

    public static boolean matches(ComplexSelector selector, Themeable node) {
        List<CompoundSelector> compounds = selector.compounds();
        List<Combinator> combinators = selector.combinators();
        return matchAt(selector, compounds.size() - 1, node);
    }

    private static boolean matchAt(ComplexSelector sel, int index, Themeable node) {
        if (!matchCompound(sel.compounds().get(index), node)) {
            return false;
        }
        if (index == 0) {
            return true;
        }
        Combinator comb = sel.combinators().get(index - 1);
        return switch (comb) {
            case descendant -> {
                Themeable p = node.themeParent();
                while (p != null) {
                    if (matchAt(sel, index - 1, p)) {
                        yield true;
                    }
                    p = p.themeParent();
                }
                yield false;
            }
            case child -> {
                Themeable p = node.themeParent();
                yield p != null && matchAt(sel, index - 1, p);
            }
            case nextSibling -> {
                Themeable prev = previousSibling(node);
                yield prev != null && matchAt(sel, index - 1, prev);
            }
            case subsequentSibling -> {
                Themeable sib = previousSibling(node);
                while (sib != null) {
                    if (matchAt(sel, index - 1, sib)) {
                        yield true;
                    }
                    sib = previousSibling(sib);
                }
                yield false;
            }
            case column -> {
                // || is not meaningful in the widget tree; treat as descendant
                Themeable p = node.themeParent();
                while (p != null) {
                    if (matchAt(sel, index - 1, p)) {
                        yield true;
                    }
                    p = p.themeParent();
                }
                yield false;
            }
        };
    }

    private static @Nullable Themeable previousSibling(Themeable node) {
        List<? extends Themeable> siblings = node.themeSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i) == node) {
                return i > 0 ? siblings.get(i - 1) : null;
            }
        }
        return null;
    }

    private static int siblingIndex(Themeable node) {
        List<? extends Themeable> siblings = node.themeSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i) == node) {
                return i;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ compound

    private static boolean matchCompound(CompoundSelector sel, Themeable node) {
        // type / universal — namespace prefixes are accepted but ignored
        if (sel.tag() != null && !sel.tag().equalsIgnoreCase(node.themeTag())) {
            return false;
        }
        if (sel.id() != null && !sel.id().equals(node.themeId())) {
            return false;
        }
        for (String cls : sel.classes()) {
            if (!node.themeClasses().contains(cls)) {
                return false;
            }
        }
        for (AttributeSelector attr : sel.attributes()) {
            if (!matchAttribute(attr, node)) {
                return false;
            }
        }
        for (PseudoClass pseudo : sel.pseudos()) {
            if (!matchPseudo(pseudo, node)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchAttribute(AttributeSelector attr, Themeable node) {
        String actual = node.themeAttr(attr.name());
        if (actual == null) {
            return false;
        }
        if (attr.operator() == null) {
            return true;
        }
        String expected = attr.value() != null ? attr.value() : "";
        boolean ci = attr.flag() == AttributeSelector.MatchFlag.insensitive;
        String a = ci ? actual.toLowerCase(Locale.ROOT) : actual;
        String e = ci ? expected.toLowerCase(Locale.ROOT) : expected;
        return switch (attr.operator()) {
            case exact -> a.equals(e);
            case includes -> List.of(a.split(" ")).contains(e);
            case dashMatch -> a.equals(e) || a.startsWith(e + "-");
            case prefixMatch -> a.startsWith(e);
            case suffixMatch -> a.endsWith(e);
            case substringMatch -> a.contains(e);
        };
    }

    // ------------------------------------------------------------------ pseudos

    private static boolean matchPseudo(PseudoClass pseudo, Themeable node) {
        String name = pseudo.name();
        if (pseudo.element()) {
            // pseudo-elements address named parts of the node itself
            return switch (name) {
                case "part" ->
                    pseudo.args() instanceof PseudoArgs.Idents ids
                            && node.themePart() != null
                            && ids.values().contains(node.themePart());
                case "before", "after", "first-line", "first-letter" -> false;
                default ->
                    pseudo.args() instanceof PseudoArgs.SelectorList sels
                            && sels.selectors().stream().anyMatch(r -> matchesRelative(r, node));
            };
        }
        return switch (name) {
            case "root" -> node.themeParent() == null;
            case "empty" -> node.themeStates().contains("empty");
            case "first-child" -> siblingIndex(node) == 0;
            case "last-child" -> siblingIndex(node) == node.themeSiblings().size() - 1;
            case "only-child" -> node.themeSiblings().size() == 1;
            case "nth-child", "nth-last-child" ->
                pseudo.args() instanceof PseudoArgs.AnPlusB ab && matchesNth(ab, node, name.equals("nth-last-child"));
            case "nth-of-type", "nth-last-of-type" ->
                pseudo.args() instanceof PseudoArgs.AnPlusB ab
                        && matchesNthOfType(ab, node, name.equals("nth-last-of-type"));
            case "nth-col", "nth-last-col" -> false; // no column concept in widget trees
            case "not" ->
                pseudo.args() instanceof PseudoArgs.SelectorList sels
                        && sels.selectors().stream().noneMatch(r -> matchesRelative(r, node));
            case "is", "where" ->
                pseudo.args() instanceof PseudoArgs.SelectorList sels
                        && sels.selectors().stream().anyMatch(r -> matchesRelative(r, node));
            case "has" ->
                pseudo.args() instanceof PseudoArgs.SelectorList sels
                        && sels.selectors().stream().anyMatch(r -> matchesHas(r, node));
            default -> node.themeStates().contains(name);
        };
    }

    /** A selector-list arg ({@code :not/:is/:where}) — combinators ignored (compound semantics). */
    private static boolean matchesRelative(RelativeSelector rel, Themeable node) {
        if (rel.combinator() != null) {
            return matchesHas(rel, node);
        }
        return matches(rel.selector(), node);
    }

    /** {@code :has()} — the relative selector must match in the space its combinator names. */
    private static boolean matchesHas(RelativeSelector rel, Themeable node) {
        Combinator comb = rel.combinator();
        if (comb == null || comb == Combinator.descendant || comb == Combinator.column) {
            for (Themeable child : childrenOf(node)) {
                if (matches(rel.selector(), child) || hasDeep(rel, child)) {
                    return true;
                }
            }
            return false;
        }
        return switch (comb) {
            case child -> childrenOf(node).stream().anyMatch(c -> matches(rel.selector(), c));
            case nextSibling -> {
                Themeable next = nextSiblingOf(node);
                yield next != null && matches(rel.selector(), next);
            }
            case subsequentSibling -> {
                List<? extends Themeable> sibs = node.themeSiblings();
                int from = siblingIndex(node) + 1;
                for (int i = from; i < sibs.size(); i++) {
                    if (matches(rel.selector(), sibs.get(i))) {
                        yield true;
                    }
                }
                yield false;
            }
            case descendant, column -> false; // handled above
        };
    }

    private static boolean hasDeep(RelativeSelector rel, Themeable node) {
        for (Themeable child : childrenOf(node)) {
            if (matches(rel.selector(), child) || hasDeep(rel, child)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Themeable nextSiblingOf(Themeable node) {
        List<? extends Themeable> siblings = node.themeSiblings();
        int i = siblingIndex(node);
        return i + 1 < siblings.size() ? siblings.get(i + 1) : null;
    }

    private static List<? extends Themeable> childrenOf(Themeable node) {
        return node.themeChildren();
    }

    private static boolean matchesNth(PseudoArgs.AnPlusB ab, Themeable node, boolean fromEnd) {
        List<? extends Themeable> sibs = node.themeSiblings();
        int index = fromEnd ? sibs.size() - siblingIndex(node) : siblingIndex(node) + 1;
        return anPlusB(ab.a(), ab.b(), index);
    }

    private static boolean matchesNthOfType(PseudoArgs.AnPlusB ab, Themeable node, boolean fromEnd) {
        List<? extends Themeable> sibs = node.themeSiblings();
        int count = 0;
        int index = 0;
        String tag = node.themeTag();
        for (int i = 0; i < sibs.size(); i++) {
            if (sibs.get(i).themeTag().equalsIgnoreCase(tag)) {
                count++;
                if (sibs.get(i) == node) {
                    index = count;
                }
            }
        }
        int pos = fromEnd ? count - index + 1 : index;
        return index > 0 && anPlusB(ab.a(), ab.b(), pos);
    }

    /** The An+B predicate — true when {@code n} satisfies {@code a*n + b = index} for some n >= 0. */
    static boolean anPlusB(int a, int b, int index) {
        if (a == 0) {
            return index == b;
        }
        int diff = index - b;
        return diff % a == 0 && diff / a >= 0;
    }
}
