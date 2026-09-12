package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import dev.vfyjxf.cloudlib.api.ui.theme.Themeable;
import dev.vfyjxf.cloudlib.internal.css.ComponentValue;
import dev.vfyjxf.cloudlib.internal.css.Declaration;
import dev.vfyjxf.cloudlib.internal.css.Specificity;
import dev.vfyjxf.cloudlib.internal.css.StyleRule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The cascade: for a {@link Themeable} node, collects every matching declaration,
 * orders it by the CSS cascade (origin → importance → specificity → source order),
 * substitutes {@code var(--x, fallback)}, and produces the resolved property map.
 * <p>
 * Inheritance follows the web rule restricted to CloudLib's supported set:
 * {@code color}, {@code text-align}, {@code direction} and all custom properties
 * inherit unless explicitly overridden.
 * <p>
 * Performance: {@link ResolveContext} memoizes each node's resolution — ancestor
 * chains resolve once per pass, so refreshing a whole tree costs
 * O(nodes × matching rules) instead of re-walking ancestors per widget.
 */
public final class Cascade {

    /** Properties that inherit through the widget tree when unspecified. */
    private static final List<String> inherited = List.of("color", "text-align", "direction");

    private Cascade() {}

    // region cascade

    private record Candidate(Specificity specificity, int order, boolean important, Declaration declaration) {}

    /**
     * Resolves the winning declarations for {@code node} under {@code theme}
     * using a fresh context. For whole-tree work prefer
     * {@code new ResolveContext(theme)} and resolve every node through it.
     */
    public static Map<String, List<ComponentValue>> resolve(Theme theme, Themeable node) {
        return new ResolveContext(theme).resolve(node);
    }

    /**
     * A reusable resolution pass over one theme: memoizes matched rules per node
     * and resolved maps per ancestor so sibling/descendant lookups are shared.
     */
    public static final class ResolveContext {

        private final Theme theme;
        private final SelectorMatcher.MatchContext match = new SelectorMatcher.MatchContext();
        private final Map<Themeable, Map<String, List<ComponentValue>>> resolved = new IdentityHashMap<>();

        public ResolveContext(Theme theme) {
            this.theme = theme;
        }

        /** The resolved property map for {@code node} (custom props included). */
        public Map<String, List<ComponentValue>> resolve(Themeable node) {
            Map<String, List<ComponentValue>> cached = resolved.get(node);
            if (cached != null) {
                return cached;
            }
            Map<String, List<ComponentValue>> out = compute(node);
            resolved.put(node, out);
            return out;
        }

        private Map<String, List<ComponentValue>> compute(Themeable node) {
            // collect matching declarations → keep winners by cascade order
            Map<String, Candidate> winners = new HashMap<>();
            List<StyleRule> candidates = theme.rulesFor(node);
            int order = 0;
            for (StyleRule rule : candidates) {
                Specificity best = null;
                for (var sel : rule.selectors()) {
                    if (SelectorMatcher.matches(match, sel, node)) {
                        Specificity s = sel.specificity();
                        if (best == null || s.compareTo(best) > 0) {
                            best = s;
                        }
                    }
                }
                if (best == null) {
                    continue;
                }
                for (Declaration decl : rule.declarations()) {
                    Candidate cand = new Candidate(best, order++, decl.important(), decl);
                    Candidate prev = winners.get(decl.property());
                    if (prev == null
                            || (cand.important && !prev.important)
                            || (cand.important == prev.important
                                    && (cand.specificity.compareTo(prev.specificity) > 0
                                            || (cand.specificity.equals(prev.specificity)
                                                    && cand.order > prev.order)))) {
                        winners.put(decl.property(), cand);
                    }
                }
            }
            Map<String, List<ComponentValue>> local = new HashMap<>();
            for (Map.Entry<String, Candidate> e : winners.entrySet()) {
                local.put(e.getKey(), e.getValue().declaration().value());
            }
            // inheritance: pull from the parent's resolved map (already memoized)
            Themeable parent = node.themeParent();
            if (parent != null) {
                Map<String, List<ComponentValue>> parentResolved = resolve(parent);
                for (Map.Entry<String, List<ComponentValue>> e : parentResolved.entrySet()) {
                    boolean inheritable =
                            inherited.contains(e.getKey()) || e.getKey().startsWith("--");
                    if (inheritable && !local.containsKey(e.getKey())) {
                        local.put(e.getKey(), e.getValue());
                    }
                }
            }
            // var() substitution — runs against this node's own resolved map
            Map<String, List<ComponentValue>> out = new HashMap<>();
            for (Map.Entry<String, List<ComponentValue>> e : local.entrySet()) {
                out.put(e.getKey(), substitute(e.getValue(), local, theme));
            }
            return out;
        }
    }

    // endregion

    // region var() substitution

    /**
     * Substitutes {@code var(--name, fallback)} inside a component-value list.
     * Unresolvable vars drop the declaration (empty result) unless a fallback exists.
     */
    private static List<ComponentValue> substitute(
            List<ComponentValue> values, Map<String, List<ComponentValue>> resolved, Theme theme) {
        List<ComponentValue> out = substituteList(values, resolved, theme, new HashSet<>());
        return out == null ? List.of() : out;
    }

    /**
     * Recursive substitution — var() can nest inside calc()/min()/max()/color()
     * and any other function arguments, per CSS Custom Properties spec.
     *
     * @param inFlight vars currently being expanded — guards {@code --a: var(--b)}
     *                 cycles (per spec, cyclic vars resolve to the empty value)
     * @return the substituted list, or null when an unresolved var() poisons the value
     */
    private static @Nullable List<ComponentValue> substituteList(
            List<ComponentValue> values,
            Map<String, List<ComponentValue>> resolved,
            Theme theme,
            Set<String> inFlight) {
        List<ComponentValue> out = new ArrayList<>(values.size());
        for (ComponentValue v : values) {
            if (v instanceof ComponentValue.Function fn) {
                if (fn.name().equalsIgnoreCase("var")) {
                    List<ComponentValue> expanded = expandVar(fn, resolved, theme, inFlight);
                    if (expanded == null) {
                        return null; // unresolved var → invalid at computed-value time
                    }
                    out.addAll(expanded);
                    continue;
                }
                List<ComponentValue> inner = substituteList(fn.args(), resolved, theme, inFlight);
                if (inner == null) {
                    return null;
                }
                out.add(new ComponentValue.Function(fn.name(), inner));
                continue;
            }
            if (v instanceof ComponentValue.Block block) {
                List<ComponentValue> inner = substituteList(block.values(), resolved, theme, inFlight);
                if (inner == null) {
                    return null;
                }
                out.add(new ComponentValue.Block(block.kind(), inner));
                continue;
            }
            out.add(v);
        }
        return out;
    }

    private static @Nullable List<ComponentValue> expandVar(
            ComponentValue.Function var,
            Map<String, List<ComponentValue>> resolved,
            Theme theme,
            Set<String> inFlight) {
        List<ComponentValue> args = var.args();
        if (args.isEmpty()
                || !(args.get(0) instanceof ComponentValue.Ident name)
                || !name.value().startsWith("--")) {
            return null;
        }
        String varName = name.value();
        if (!inFlight.add(varName)) {
            return null; // cyclic reference — per spec resolves to the empty value
        }
        try {
            List<ComponentValue> found = resolved.get(varName);
            if (found == null) {
                found = theme.rootVars().get(varName);
            }
            if (found != null) {
                // the var's own value may reference further vars — expand recursively
                List<ComponentValue> expanded = substituteList(found, resolved, theme, inFlight);
                if (expanded == null) {
                    return null;
                }
                return expanded.stream()
                        .filter(c -> c != ComponentValue.Whitespace.instance)
                        .toList();
            }
            // fallback = everything after the first comma
            int comma = -1;
            for (int i = 0; i < args.size(); i++) {
                if (args.get(i) instanceof ComponentValue.Delim d && d.value() == ',') {
                    comma = i;
                    break;
                }
            }
            if (comma < 0) {
                return null;
            }
            List<ComponentValue> fb = args.subList(comma + 1, args.size());
            return substituteList(fb, resolved, theme, inFlight) != null
                    ? fb.stream()
                            .filter(c -> c != ComponentValue.Whitespace.instance)
                            .toList()
                    : null;
        } finally {
            inFlight.remove(varName);
        }
    }

    // endregion
}
