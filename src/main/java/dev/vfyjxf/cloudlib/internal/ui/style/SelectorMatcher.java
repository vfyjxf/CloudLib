package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.AttributeSelector;
import dev.vfyjxf.cloudlib.api.css.Combinator;
import dev.vfyjxf.cloudlib.api.css.ComplexSelector;
import dev.vfyjxf.cloudlib.api.css.CompoundSelector;
import dev.vfyjxf.cloudlib.api.css.PseudoArgs;
import dev.vfyjxf.cloudlib.api.css.PseudoClass;
import dev.vfyjxf.cloudlib.api.css.RelativeSelector;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Matches {@link ComplexSelector}s against the {@link Widget} tree.
 * <p>
 * Supported: type/universal, namespace prefixes (accepted and ignored — the widget
 * tree is flat-namespaced), {@code #id}, {@code .class}, all attribute operators,
 * descendant/child/sibling/column combinators, and pseudo-classes:
 * {@code hovered active inactive interactive non-interactive focused disabled enabled
 * checked selected pressed empty first-child last-child only-child nth-child
 * nth-last-child nth-of-type nth-last-of-type odd/even forms, root, not, is, where,
 * has} plus widget-defined custom states. Pseudo-elements resolve via
 * {@link Widget#stylePart()} ({@code ::part(name)}) or widget-defined part tags.
 * <p>
 * Matching against a node repeatedly calls back into {@code parent().children()},
 * {@code styleStates()} and {@code composite children()} — those allocate. A
 * {@link MatchContext} memoizes them per node so a full cascade pass costs one
 * snapshot per node instead of one per rule.
 */
public final class SelectorMatcher {

    private SelectorMatcher() {}

    // region match context

    /**
     * Per-resolution memo: siblings/children/states of every node touched while
     * matching this pass. Share one context across a whole cascade resolution.
     */
    public static final class MatchContext {

        private final Map<Widget, List<Widget>> siblings = new IdentityHashMap<>();
        private final Map<Widget, List<Widget>> children = new IdentityHashMap<>();
        private final Map<Widget, Set<String>> states = new IdentityHashMap<>();
        private final Map<Widget, Integer> siblingIndex = new IdentityHashMap<>();

        /** Siblings in document order (self included) — the real widget tree. */
        List<Widget> siblingsOf(Widget node) {
            return siblings.computeIfAbsent(node, n -> {
                Widget parent = n.parent();
                if (parent == null) {
                    return List.of(n);
                }
                return List.copyOf(((CompositeWidget<?>) parent).children());
            });
        }

        /** Direct children in document order — composites only. */
        List<Widget> childrenOf(Widget node) {
            return children.computeIfAbsent(
                node,
                n -> n instanceof CompositeWidget<?> composite ? List.copyOf(composite.children()) : List.of()
            );
        }

        Set<String> statesOf(Widget node) {
            return states.computeIfAbsent(node, Widget::styleStates);
        }

        /** {@code node's} index among its siblings — memoized, linear scan once. */
        int indexOf(Widget node) {
            return siblingIndex.computeIfAbsent(node, n -> {
                List<Widget> sibs = siblingsOf(n);
                for (int i = 0; i < sibs.size(); i++) {
                    if (sibs.get(i) == n) {
                        return i;
                    }
                }
                return 0;
            });
        }
    }

    // endregion

    // region matching

    public static boolean matches(ComplexSelector selector, Widget node) {
        return matches(new MatchContext(), selector, node);
    }

    public static boolean matches(MatchContext ctx, ComplexSelector selector, Widget node) {
        return matchAt(ctx, selector, selector.compounds().size() - 1, node);
    }

    private static boolean matchAt(MatchContext ctx, ComplexSelector sel, int index, Widget node) {
        if (!matchCompound(ctx, sel.compounds().get(index), node)) {
            return false;
        }
        if (index == 0) {
            return true;
        }
        Combinator comb = sel.combinators().get(index - 1);
        return switch (comb) {
            case descendant -> {
                Widget p = node.parent();
                while (p != null) {
                    if (matchAt(ctx, sel, index - 1, p)) {
                        yield true;
                    }
                    p = p.parent();
                }
                yield false;
            }
            case child -> {
                Widget p = node.parent();
                yield p != null && matchAt(ctx, sel, index - 1, p);
            }
            case nextSibling -> {
                Widget prev = previousSibling(ctx, node);
                yield prev != null && matchAt(ctx, sel, index - 1, prev);
            }
            case subsequentSibling -> {
                Widget sib = previousSibling(ctx, node);
                while (sib != null) {
                    if (matchAt(ctx, sel, index - 1, sib)) {
                        yield true;
                    }
                    sib = previousSibling(ctx, sib);
                }
                yield false;
            }
            case column -> {
                // || is not meaningful in the widget tree; treat as descendant
                Widget p = node.parent();
                while (p != null) {
                    if (matchAt(ctx, sel, index - 1, p)) {
                        yield true;
                    }
                    p = p.parent();
                }
                yield false;
            }
        };
    }

    private static @Nullable Widget previousSibling(MatchContext ctx, Widget node) {
        List<Widget> siblings = ctx.siblingsOf(node);
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i) == node) {
                return i > 0 ? siblings.get(i - 1) : null;
            }
        }
        return null;
    }

    private static @Nullable Widget nextSibling(MatchContext ctx, Widget node) {
        List<Widget> siblings = ctx.siblingsOf(node);
        int i = ctx.indexOf(node);
        return i + 1 < siblings.size() ? siblings.get(i + 1) : null;
    }

    // endregion

    // region compound

    private static boolean matchCompound(MatchContext ctx, CompoundSelector sel, Widget node) {
        // type / universal — namespace prefixes are accepted but ignored
        if (sel.tag() != null && !sel.tag().equalsIgnoreCase(node.styleTag())) {
            return false;
        }
        if (sel.id() != null && !sel.id().equals(node.styleId())) {
            return false;
        }
        for (String cls : sel.classes()) {
            if (!node.styleClasses().contains(cls)) {
                return false;
            }
        }
        for (AttributeSelector attr : sel.attributes()) {
            if (!matchAttribute(attr, node)) {
                return false;
            }
        }
        for (PseudoClass pseudo : sel.pseudos()) {
            if (!matchPseudo(ctx, pseudo, node)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchAttribute(AttributeSelector attr, Widget node) {
        String actual = node.styleAttrs().get(attr.name());
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

    // endregion

    // region pseudos

    private static boolean matchPseudo(MatchContext ctx, PseudoClass pseudo, Widget node) {
        String name = pseudo.name();
        if (pseudo.element()) {
            // pseudo-elements address named parts of the node itself
            return switch (name) {
                case "part" -> pseudo.args() instanceof PseudoArgs.Idents ids
                        && node.stylePart() != null
                        && ids.values().contains(node.stylePart());
                case "before", "after", "first-line", "first-letter" -> false;
                default -> pseudo.args() instanceof PseudoArgs.SelectorList sels
                        && sels.selectors().stream().anyMatch(r -> matchesRelative(ctx, r, node));
            };
        }
        return switch (name) {
            case "root" -> node.parent() == null;
            case "empty" -> ctx.statesOf(node).contains("empty");
            case "first-child" -> ctx.indexOf(node) == 0;
            case "last-child" -> ctx.indexOf(node) == ctx.siblingsOf(node).size() - 1;
            case "only-child" -> ctx.siblingsOf(node).size() == 1;
            case "nth-child", "nth-last-child" -> pseudo.args() instanceof PseudoArgs.AnPlusB ab
                    && matchesNth(ctx, ab, node, name.equals("nth-last-child"));
            case "nth-of-type", "nth-last-of-type" -> pseudo.args() instanceof PseudoArgs.AnPlusB ab
                    && matchesNthOfType(ctx, ab, node, name.equals("nth-last-of-type"));
            case "nth-col", "nth-last-col" -> false; // no column concept in widget trees
            case "not" -> pseudo.args() instanceof PseudoArgs.SelectorList sels
                    && sels.selectors().stream().noneMatch(r -> matchesRelative(ctx, r, node));
            case "is", "where" -> pseudo.args() instanceof PseudoArgs.SelectorList sels
                    && sels.selectors().stream().anyMatch(r -> matchesRelative(ctx, r, node));
            case "has" -> pseudo.args() instanceof PseudoArgs.SelectorList sels
                    && sels.selectors().stream().anyMatch(r -> matchesHas(ctx, r, node));
            default -> ctx.statesOf(node).contains(name);
        };
    }

    /** A selector-list arg ({@code :not/:is/:where}) — combinators ignored (compound semantics). */
    private static boolean matchesRelative(MatchContext ctx, RelativeSelector rel, Widget node) {
        if (rel.combinator() != null) {
            return matchesHas(ctx, rel, node);
        }
        return matches(ctx, rel.selector(), node);
    }

    /** {@code :has()} — the relative selector must match in the space its combinator names. */
    private static boolean matchesHas(MatchContext ctx, RelativeSelector rel, Widget node) {
        Combinator comb = rel.combinator();
        if (comb == null || comb == Combinator.descendant || comb == Combinator.column) {
            for (Widget child : ctx.childrenOf(node)) {
                if (matches(ctx, rel.selector(), child) || hasDeep(ctx, rel, child)) {
                    return true;
                }
            }
            return false;
        }
        return switch (comb) {
            case child -> ctx.childrenOf(node).stream().anyMatch(c -> matches(ctx, rel.selector(), c));
            case nextSibling -> {
                Widget next = nextSibling(ctx, node);
                yield next != null && matches(ctx, rel.selector(), next);
            }
            case subsequentSibling -> {
                List<Widget> sibs = ctx.siblingsOf(node);
                int from = ctx.indexOf(node) + 1;
                for (int i = from; i < sibs.size(); i++) {
                    if (matches(ctx, rel.selector(), sibs.get(i))) {
                        yield true;
                    }
                }
                yield false;
            }
            case descendant, column -> false; // handled above
        };
    }

    private static boolean hasDeep(MatchContext ctx, RelativeSelector rel, Widget node) {
        for (Widget child : ctx.childrenOf(node)) {
            if (matches(ctx, rel.selector(), child) || hasDeep(ctx, rel, child)) {
                return true;
            }
        }
        return false;
    }

    // endregion

    // region positional

    private static boolean matchesNth(MatchContext ctx, PseudoArgs.AnPlusB ab, Widget node, boolean fromEnd) {
        List<Widget> sibs = ctx.siblingsOf(node);
        int index = fromEnd ? sibs.size() - ctx.indexOf(node) : ctx.indexOf(node) + 1;
        return anPlusB(ab.a(), ab.b(), index);
    }

    private static boolean matchesNthOfType(MatchContext ctx, PseudoArgs.AnPlusB ab, Widget node, boolean fromEnd) {
        List<Widget> sibs = ctx.siblingsOf(node);
        int count = 0;
        int index = 0;
        String tag = node.styleTag();
        for (int i = 0; i < sibs.size(); i++) {
            if (sibs.get(i).styleTag().equalsIgnoreCase(tag)) {
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

    // endregion
}
