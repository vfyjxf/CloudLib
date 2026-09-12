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
     * Rules that could match {@code node}: its tag bucket + every class bucket +
     * its id bucket + the always bucket, deduplicated in source order.
     */
    public List<StyleRule> rulesFor(Themeable node) {
        if (byTag == null) {
            buildIndex();
        }
        List<StyleRule> out = new ArrayList<>(always);
        List<StyleRule> tagged = byTag.get(node.themeTag().toLowerCase(java.util.Locale.ROOT));
        if (tagged != null) {
            out.addAll(tagged);
        }
        for (String cls : node.themeClasses()) {
            List<StyleRule> clsRules = byClass.get(cls);
            if (clsRules != null) {
                out.addAll(clsRules);
            }
        }
        if (node.themeId() != null) {
            List<StyleRule> idRules = byId.get(node.themeId());
            if (idRules != null) {
                out.addAll(idRules);
            }
        }
        // a rule may land in several buckets — dedupe while keeping source order
        out.sort(java.util.Comparator.comparingInt(order::get));
        List<StyleRule> dedup = new ArrayList<>(out.size());
        StyleRule last = null;
        for (StyleRule r : out) {
            if (r != last) {
                dedup.add(r);
            }
            last = r;
        }
        return dedup;
    }

    // endregion

    // region rule index

    private volatile @Nullable Map<String, List<StyleRule>> byTag;
    private volatile @Nullable Map<String, List<StyleRule>> byClass;
    private volatile @Nullable Map<String, List<StyleRule>> byId;
    private volatile @Nullable List<StyleRule> always;
    private volatile @Nullable Map<StyleRule, Integer> order;
    private volatile @Nullable Map<String, List<ComponentValue>> rootVars;

    /**
     * Buckets rules by the rightmost compound's most selective simple selector:
     * {@code #id} &gt; {@code .class} &gt; tag &gt; always. A rule lands in
     * {@code always} when ANY of its selectors has no keyable rightmost compound.
     */
    private synchronized void buildIndex() {
        if (byTag != null) {
            return;
        }
        Map<String, List<StyleRule>> tag = new LinkedHashMap<>();
        Map<String, List<StyleRule>> cls = new LinkedHashMap<>();
        Map<String, List<StyleRule>> ids = new LinkedHashMap<>();
        List<StyleRule> all = new ArrayList<>();
        Map<StyleRule, Integer> ord = new LinkedHashMap<>();
        int i = 0;
        for (StyleRule rule : styleRules()) {
            ord.put(rule, i++);
            for (var sel : rule.selectors()) {
                var last = sel.last();
                if (last.id() != null) {
                    ids.computeIfAbsent(last.id(), k -> new ArrayList<>()).add(rule);
                } else if (!last.classes().isEmpty()) {
                    for (String c : last.classes()) {
                        cls.computeIfAbsent(c, k -> new ArrayList<>()).add(rule);
                    }
                } else if (last.tag() != null) {
                    tag.computeIfAbsent(last.tag().toLowerCase(java.util.Locale.ROOT), k -> new ArrayList<>())
                            .add(rule);
                } else {
                    all.add(rule);
                }
            }
        }
        byTag = tag;
        byClass = cls;
        byId = ids;
        always = all;
        order = ord;
    }

    // endregion
}
