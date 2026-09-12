package dev.vfyjxf.cloudlib.api.ui.theme;

import dev.vfyjxf.cloudlib.internal.css.ComponentValue;
import dev.vfyjxf.cloudlib.internal.css.Declaration;
import dev.vfyjxf.cloudlib.internal.css.StyleRule;
import dev.vfyjxf.cloudlib.internal.css.Stylesheet;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed theme: an id plus the flattened, {@code @import}-resolved rule list.
 * <p>
 * Rules keep source order across the whole import chain — later declarations of
 * equal specificity win per the cascade.
 * <p>
 * For matching speed, rules are bucketed by the rightmost compound's tag /
 * classes / id — a node only tests rules that could possibly match it, plus the
 * always-matchable bucket. The index builds lazily on first {@link #rulesFor}.
 */
public final class Theme {

    private final ResourceLocation id;
    private final Stylesheet sheet;

    public Theme(ResourceLocation id, Stylesheet sheet) {
        this.id = id;
        this.sheet = sheet;
    }

    public ResourceLocation id() {
        return id;
    }

    public Stylesheet sheet() {
        return sheet;
    }

    // region rule access

    /** All style rules in source order (imported sheets inlined at the import point). */
    public List<StyleRule> styleRules() {
        return sheet.rules().stream()
                .filter(r -> r instanceof StyleRule)
                .map(r -> (StyleRule) r)
                .toList();
    }

    /** {@code :root}-scoped custom properties ({@code --name → raw values}), precomputed. */
    public Map<String, List<ComponentValue>> rootVars() {
        if (rootVars == null) {
            Map<String, List<ComponentValue>> vars = new LinkedHashMap<>();
            for (StyleRule rule : styleRules()) {
                boolean isRoot = rule.selectors().stream().anyMatch(s -> s.last().pseudos().stream()
                        .anyMatch(p -> p.name().equals("root")));
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
    public List<StyleRule> rulesFor(Themeable node) {
        if (byTag == null) {
            buildIndex();
        }
        List<IndexedRule> cand = new ArrayList<>(always.size());
        cand.addAll(always);
        List<IndexedRule> tagged = byTag.get(node.themeTag().toLowerCase(java.util.Locale.ROOT));
        if (tagged != null) {
            cand.addAll(tagged);
        }
        for (String c : node.themeClasses()) {
            List<IndexedRule> clsRules = byClass.get(c);
            if (clsRules != null) {
                cand.addAll(clsRules);
            }
        }
        if (node.themeId() != null) {
            List<IndexedRule> idRules = byId.get(node.themeId());
            if (idRules != null) {
                cand.addAll(idRules);
            }
        }
        // merge buckets back into global source order — int sort, no record hashing
        cand.sort(java.util.Comparator.comparingInt(IndexedRule::order));
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
                    tag.computeIfAbsent(last.tag().toLowerCase(java.util.Locale.ROOT), k -> new ArrayList<>())
                            .add(ir);
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
