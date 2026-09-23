package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Declaration;
import dev.vfyjxf.cloudlib.api.css.Rule;
import dev.vfyjxf.cloudlib.api.css.StyleRule;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.internal.ui.style.Cascade;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A theme: a parsed stylesheet that resolves into a {@link UIStyle} per widget.
 * <p>
 * A theme <em>is</em> a style — every widget resolves its own {@code UIStyle}
 * from the same rule list, then applies it through the usual style machinery
 * (below code styles, above widget defaults).
 * <p>
 * For matching speed, rules are bucketed by the rightmost compound's tag /
 * classes / id — a node only tests rules that could possibly match it, plus the
 * always-matchable bucket. The index builds lazily on first {@link #rulesFor}.
 */
public final class Theme {

    /**
     * Display metadata from the theme's {@code theme.json} — both fields
     * optional; {@code null} when the theme was built programmatically or the
     * descriptor omitted them.
     */
    public record Meta(@Nullable String name, @Nullable String description) {}

    private final ResourceLocation id;
    private final Stylesheet sheet;
    private final @Nullable Meta meta;

    public Theme(ResourceLocation id, Stylesheet sheet) {
        this(id, sheet, null);
    }

    public Theme(ResourceLocation id, Stylesheet sheet, @Nullable Meta meta) {
        this.id = id;
        this.sheet = sheet;
        this.meta = meta;
    }

    public ResourceLocation id() {
        return id;
    }

    public Stylesheet sheet() {
        return sheet;
    }

    public @Nullable Meta meta() {
        return meta;
    }

    // region composition

    /**
     * Layers several themes into one: their rule lists concatenate in stack
     * order, so a later layer wins equal-specificity ties — the real cascade
     * does the rest. A single layer returns itself (keeps its own id).
     */
    public static Theme compose(List<Theme> layers) {
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("compose() needs at least one layer");
        }
        if (layers.size() == 1) {
            return layers.get(0);
        }
        List<Rule> rules = new ArrayList<>();
        StringBuilder path = new StringBuilder("stack/");
        for (Theme layer : layers) {
            rules.addAll(layer.sheet.rules());
            path.append(layer.id.getPath().replace('/', '_')).append('_');
        }
        path.setLength(path.length() - 1);
        // the top layer's meta describes the stack
        return new Theme(
            ResourceLocation.fromNamespaceAndPath("cloudlib", path.toString()),
            new Stylesheet(rules),
            layers.get(layers.size() - 1).meta()
        );
    }

    // endregion

    // region resolve

    /**
     * Resolves this theme against {@code node} into a {@code UIStyle}.
     *
     * @param warn sink for dropped/invalid declarations (may be null)
     */
    public UIStyle resolve(Widget node, @Nullable Consumer<String> warn) {
        return ThemeEngine.resolve(new Cascade.ResolveContext(this), this, node, warn);
    }

    /** Convenience overload without a warning sink. */
    public UIStyle resolve(Widget node) {
        return resolve(node, null);
    }

    /** Resolves through a shared cascade context — used by scene dirty flushes. */
    public UIStyle resolveShared(Widget node, Cascade.ResolveContext ctx) {
        return ThemeEngine.resolve(ctx, this, node, null);
    }

    /**
     * Re-resolves this theme for {@code root} and its whole subtree, sharing one
     * cascade context so ancestors resolve once per pass. Each widget applies the
     * resolved style into its theme segment — see {@link Widget#applyThemeStyle}.
     */
    public void applyTree(Widget root, @Nullable Consumer<String> warn) {
        applyTree(new Cascade.ResolveContext(this), root, warn);
    }

    public void applyTree(Widget root) {
        applyTree(root, null);
    }

    private void applyTree(Cascade.ResolveContext ctx, Widget widget, @Nullable Consumer<String> warn) {
        widget.applyThemeStyle(ThemeEngine.resolve(ctx, this, widget, warn));
        if (widget instanceof CompositeWidget<?> composite) {
            composite.children().forEach(c -> applyTree(ctx, c, warn));
        }
    }

    // endregion

    // region rule access

    /** All style rules in source order (imported sheets inlined at the import point). */
    public List<StyleRule> styleRules() {
        return sheet.rules().stream().filter(r -> r instanceof StyleRule).map(r -> (StyleRule) r).toList();
    }

    /** {@code :root}-scoped custom properties ({@code --name → raw values}), precomputed. */
    public Map<String, List<ComponentValue>> rootVars() {
        if (rootVars == null) {
            Map<String, List<ComponentValue>> vars = new LinkedHashMap<>();
            for (StyleRule rule : styleRules()) {
                boolean isRoot = rule.selectors().stream()
                        .anyMatch(s -> s.last().pseudos().stream().anyMatch(p -> p.name().equals("root")));
                if (!isRoot) {
                    continue;
                }
                for (Declaration decl : rule.declarations()) {
                    if (decl.property().startsWith("--")) {
                        vars.put(decl.property(), decl.value());
                    }
                }
            }
            rootVars = vars;
        }
        return rootVars;
    }

    /**
     * Rules that could match {@code node}: a single pass over the indexed rule
     * list — a rule is a candidate when the node supplies ANY of the keys its
     * selectors require (tag/class/id), or when it contains a universal selector.
     * Source order is preserved by construction — no sorting, no hashing of the
     * rule records.
     */
    public List<StyleRule> rulesFor(Widget node) {
        if (byTag == null) {
            buildIndex();
        }
        List<IndexedRule> cand = new ArrayList<>(always.size());
        cand.addAll(always);
        List<IndexedRule> tagged = byTag.get(node.styleTag().toLowerCase(Locale.ROOT));
        if (tagged != null) {
            cand.addAll(tagged);
        }
        for (String c : node.styleClasses()) {
            List<IndexedRule> clsRules = byClass.get(c);
            if (clsRules != null) {
                cand.addAll(clsRules);
            }
        }
        if (node.styleId() != null) {
            List<IndexedRule> idRules = byId.get(node.styleId());
            if (idRules != null) {
                cand.addAll(idRules);
            }
        }
        // merge buckets back into global source order — int sort, no record hashing
        cand.sort(Comparator.comparingInt(IndexedRule::order));
        List<StyleRule> out = new ArrayList<>(cand.size());
        StyleRule last = null;
        for (IndexedRule ir : cand) {
            if (ir.rule() != last) {
                out.add(ir.rule());
            }
            last = ir.rule();
        }
        return out;
    }

    // endregion

    // region rule index

    /** A rule plus its source position — ordering never hashes the rule record. */
    private record IndexedRule(StyleRule rule, int order) {}

    private volatile @Nullable Map<String, List<IndexedRule>> byTag;
    private volatile @Nullable Map<String, List<IndexedRule>> byClass;
    private volatile @Nullable Map<String, List<IndexedRule>> byId;
    private volatile @Nullable List<IndexedRule> always;
    private volatile @Nullable Map<String, List<ComponentValue>> rootVars;

    /**
     * Buckets rules by the rightmost compound's most selective key:
     * {@code #id} &gt; {@code .class} &gt; tag &gt; always. Each bucket keeps
     * global source order via the embedded {@code order} index.
     */
    private synchronized void buildIndex() {
        if (byTag != null) {
            return;
        }
        Map<String, List<IndexedRule>> tag = new LinkedHashMap<>();
        Map<String, List<IndexedRule>> cls = new LinkedHashMap<>();
        Map<String, List<IndexedRule>> ids = new LinkedHashMap<>();
        List<IndexedRule> all = new ArrayList<>();
        int i = 0;
        for (StyleRule rule : styleRules()) {
            IndexedRule ir = new IndexedRule(rule, i++);
            for (var sel : rule.selectors()) {
                var last = sel.last();
                if (last.id() != null) {
                    ids.computeIfAbsent(last.id(), k -> new ArrayList<>()).add(ir);
                } else if (!last.classes().isEmpty()) {
                    for (String c : last.classes()) {
                        cls.computeIfAbsent(c, k -> new ArrayList<>()).add(ir);
                    }
                } else if (last.tag() != null) {
                    tag.computeIfAbsent(last.tag().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(ir);
                } else {
                    all.add(ir);
                }
            }
        }
        byTag = tag;
        byClass = cls;
        byId = ids;
        always = all;
    }

    // endregion
}
