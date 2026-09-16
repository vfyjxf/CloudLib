package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComplexSelector;
import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Declaration;
import dev.vfyjxf.cloudlib.api.css.Specificity;
import dev.vfyjxf.cloudlib.api.css.StyleRule;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The cascade: for a {@link Widget} node, collects every matching declaration,
 * orders it by the CSS cascade (origin → importance → specificity → source order),
 * substitutes {@code var(--x, fallback)}, and produces the resolved property map.
 * <p>
 * Inheritance follows the web rule: a property inherits when its
 * {@link StyleKey} is flagged
 * {@code inherited} ({@code color}, {@code text-align}, {@code direction});
 * all custom properties ({@code --*}) inherit unconditionally.
 * <p>
 * Performance: {@link ResolveContext} memoizes each node's resolution — ancestor
 * chains resolve once per pass, so refreshing a whole tree costs
 * O(nodes × matching rules) instead of re-walking ancestors per widget.
 */
public final class Cascade {

    private Cascade() {}

    // region cascade

    /**
     * A winning declaration after cascade + var() substitution.
     *
     * @param declaration the source declaration (identity-stable across nodes —
     *                    downstream consumers may memoize on it)
     * @param value       the substituted component values
     * @param hadVar      whether the declaration contained a {@code var()}
     *                    reference — var-free declarations parse identically on
     *                    every node, so consumers can cache by declaration
     */
    public record ResolvedDecl(Declaration declaration, List<ComponentValue> value, boolean hadVar) {}

    private record Candidate(Specificity specificity, int order, boolean important, Declaration declaration) {}

    /**
     * Resolves the winning declarations for {@code node} under {@code theme}
     * using a fresh context. For whole-tree work prefer
     * {@code new ResolveContext(theme)} and resolve every node through it.
     */
    public static Map<String, ResolvedDecl> resolve(Theme theme, Widget node) {
        return new ResolveContext(theme).resolve(node);
    }

    /**
     * A reusable resolution pass over one theme: memoizes matched rules per node
     * and resolved maps per ancestor so sibling/descendant lookups are shared.
     */
    public static final class ResolveContext {

        private final Theme theme;
        private final SelectorMatcher.MatchContext match = new SelectorMatcher.MatchContext();
        private final Map<Widget, Map<String, ResolvedDecl>> resolved = new IdentityHashMap<>();
        private final Map<ComplexSelector, Specificity> specificity = new IdentityHashMap<>();
        /** {@code var()}-presence per shared declaration — computed once per pass. */
        private final Map<Declaration, Boolean> declHasVar = new IdentityHashMap<>();
        /**
         * Parsed {@code StyleValue}s per var-free declaration — shared across the
         * pass so a rule's value parses once for the whole tree, not per node.
         */
        private final Map<Declaration, List<StyleValue<?>>> valueCache = new IdentityHashMap<>();

        /** The per-pass declaration→parsed-values memo (engine use). */
        public Map<Declaration, List<StyleValue<?>>> valueCache() {
            return valueCache;
        }

        public ResolveContext(Theme theme) {
            this.theme = theme;
        }

        /** The resolved property map for {@code node} (custom props included). */
        public Map<String, ResolvedDecl> resolve(Widget node) {
            Map<String, ResolvedDecl> cached = resolved.get(node);
            if (cached != null) {
                return cached;
            }
            Map<String, ResolvedDecl> out = compute(node);
            resolved.put(node, out);
            return out;
        }

        private Map<String, ResolvedDecl> compute(Widget node) {
            // collect matching declarations → keep winners by cascade order
            Map<String, Candidate> winners = new HashMap<>();
            List<StyleRule> candidates = theme.rulesFor(node);
            int order = 0;
            for (StyleRule rule : candidates) {
                Specificity best = null;
                for (var sel : rule.selectors()) {
                    if (SelectorMatcher.matches(match, sel, node)) {
                        Specificity s = specificity.computeIfAbsent(sel, ComplexSelector::specificity);
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
            // emit winners in declaration order — shorthand expansion downstream
            // relies on it (padding:2px declared before padding-left:4px means
            // left wins; the reverse declaration order reverses the result)
            List<Candidate> ordered = new ArrayList<>(winners.values());
            ordered.sort(Comparator.comparingInt(Candidate::order));
            Map<String, ResolvedDecl> local = new LinkedHashMap<>(ordered.size());
            for (Candidate cand : ordered) {
                Declaration decl = cand.declaration();
                boolean hasVar = declHasVar.computeIfAbsent(decl, d -> containsVar(d.value()));
                local.put(decl.property(), new ResolvedDecl(decl, decl.value(), hasVar));
            }
            // inheritance: pull from the parent's resolved map (already memoized)
            Widget parent = node.parent();
            if (parent != null) {
                Map<String, ResolvedDecl> parentResolved = resolve(parent);
                for (Map.Entry<String, ResolvedDecl> e : parentResolved.entrySet()) {
                    var key = Styles.byId(e.getKey());
                    boolean inheritable =
                            (key != null && key.inherited()) || e.getKey().startsWith("--");
                    if (inheritable && !local.containsKey(e.getKey())) {
                        local.put(e.getKey(), e.getValue());
                    }
                }
            }
            // inline custom properties — java-side setVar bindings behave like
            // style-attribute declarations: they sit above every theme winner
            // in this node's resolved map and feed var() like any other --*.
            Map<String, Tokens> inline = node.codeVars();
            if (!inline.isEmpty()) {
                for (Map.Entry<String, Tokens> e : inline.entrySet()) {
                    List<ComponentValue> vals = e.getValue().values();
                    Declaration synth = new Declaration(e.getKey(), vals, false);
                    local.put(e.getKey(), new ResolvedDecl(synth, vals, containsVar(vals)));
                }
            }
            // var() substitution — runs against this node's own resolved map;
            // per-node varCache: expanding --pad under this node's bindings is
            // deterministic, so repeated uses expand once. var-free declarations
            // pass through untouched — their values are node-independent, so the
            // engine downstream can memoize parse results on declaration identity.
            Map<String, List<ComponentValue>> varCache = new HashMap<>();
            Function<String, List<ComponentValue>> env = name -> {
                ResolvedDecl d = local.get(name);
                if (d != null) {
                    return d.value();
                }
                return theme.rootVars().get(name);
            };
            Map<String, ResolvedDecl> out = new LinkedHashMap<>();
            for (Map.Entry<String, ResolvedDecl> e : local.entrySet()) {
                ResolvedDecl decl = e.getValue();
                if (!decl.hadVar()) {
                    out.put(e.getKey(), decl);
                    continue;
                }
                out.put(
                        e.getKey(),
                        new ResolvedDecl(decl.declaration(), substitute(decl.value(), env, varCache), true));
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
            List<ComponentValue> values,
            Function<String, @Nullable List<ComponentValue>> env,
            Map<String, List<ComponentValue>> varCache) {
        if (!containsVar(values)) {
            return values; // fast path — no var() anywhere, skip the copy
        }
        List<ComponentValue> out = substituteList(values, env, varCache, new HashSet<>());
        return out == null ? List.of() : out;
    }

    /**
     * Apply-time {@code var()} resolution for the {@code StyleContext} var
     * table — substitutes references against the already-resolved {@code env}.
     * Unlike cascade substitution, an unresolvable reference keeps the raw
     * tokens (a later binding may complete it).
     */
    public static Tokens substituteVars(Tokens value, Map<String, Tokens> env) {
        if (!containsVar(value.values())) {
            return value;
        }
        List<ComponentValue> out = substituteList(
                value.values(),
                name -> {
                    Tokens t = env.get(name);
                    return t == null ? null : t.values();
                },
                new HashMap<>(),
                new HashSet<>());
        return out == null ? value : Tokens.of(out);
    }

    private static boolean containsVar(List<ComponentValue> values) {
        for (ComponentValue v : values) {
            if (v instanceof ComponentValue.Function fn) {
                if (fn.name().equalsIgnoreCase("var") || containsVar(fn.args())) {
                    return true;
                }
            } else if (v instanceof ComponentValue.Block block && containsVar(block.values())) {
                return true;
            }
        }
        return false;
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
            Function<String, @Nullable List<ComponentValue>> env,
            Map<String, List<ComponentValue>> varCache,
            Set<String> inFlight) {
        List<ComponentValue> out = new ArrayList<>(values.size());
        for (ComponentValue v : values) {
            if (v instanceof ComponentValue.Function fn) {
                if (fn.name().equalsIgnoreCase("var")) {
                    List<ComponentValue> expanded = expandVar(fn, env, varCache, inFlight);
                    if (expanded == null) {
                        return null; // unresolved var → invalid at computed-value time
                    }
                    out.addAll(expanded);
                    continue;
                }
                List<ComponentValue> inner = substituteList(fn.args(), env, varCache, inFlight);
                if (inner == null) {
                    return null;
                }
                out.add(new ComponentValue.Function(fn.name(), inner));
                continue;
            }
            if (v instanceof ComponentValue.Block block) {
                List<ComponentValue> inner = substituteList(block.values(), env, varCache, inFlight);
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
            Function<String, @Nullable List<ComponentValue>> env,
            Map<String, List<ComponentValue>> varCache,
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
            List<ComponentValue> expanded = varCache.get(varName);
            if (expanded != null) {
                return expanded;
            }
            List<ComponentValue> found = env.apply(varName);
            if (found != null) {
                // the var's own value may reference further vars — expand recursively
                expanded = substituteList(found, env, varCache, inFlight);
                if (expanded == null) {
                    return null;
                }
                // cache only resolved bindings — fallbacks differ per call site
                List<ComponentValue> trimmed = stripWhitespace(expanded);
                varCache.put(varName, trimmed);
                return trimmed;
            } else {
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
                expanded = substituteList(args.subList(comma + 1, args.size()), env, varCache, inFlight);
                if (expanded == null) {
                    return null;
                }
                return stripWhitespace(expanded);
            }
        } finally {
            inFlight.remove(varName);
        }
    }

    private static List<ComponentValue> stripWhitespace(List<ComponentValue> values) {
        List<ComponentValue> out = new ArrayList<>(values.size());
        for (ComponentValue c : values) {
            if (c != ComponentValue.Whitespace.instance) {
                out.add(c);
            }
        }
        return out;
    }

    // endregion
}
