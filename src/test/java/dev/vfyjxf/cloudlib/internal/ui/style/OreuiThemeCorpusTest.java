package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.Combinator;
import dev.vfyjxf.cloudlib.api.css.ComplexSelector;
import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.CompoundSelector;
import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Declaration;
import dev.vfyjxf.cloudlib.api.css.PseudoArgs;
import dev.vfyjxf.cloudlib.api.css.PseudoClass;
import dev.vfyjxf.cloudlib.api.css.Rule;
import dev.vfyjxf.cloudlib.api.css.StyleRule;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.style.key.BuiltinKeys;
import dev.vfyjxf.cloudlib.api.ui.texture.BuiltInTextures;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two shipped oreui sheets (ore and mc) as natively rewritten — a corpus
 * guard, not a parser test. It reads every sheet off the classpath and pins the
 * skeleton the rewrite shares across the set: one rule count, the control
 * vocabulary every
 * sheet styles (hosts, button and toggle states, the slider and progress-bar
 * parts), texture declarations that parse, {@code built-in(ns:NAME)} ids that
 * exist, {@code --*} custom properties as the only theme-side vocabulary, and no
 * leftover LDLib2 property name or Shadow-DOM {@code :host}.
 * <p>
 * A theme that loses part of its skeleton (a rule dropped, a part left unstyled,
 * a sprite id renamed, a function the vocabulary cannot read) fails here instead
 * of silently loading as a thinner theme.
 */
class OreuiThemeCorpusTest {

    /** The shipped sheets, each at {@code themes/<name>/<name>.css} — the entry its theme.json declares. */
    private static final List<String> themes = List.of("ore", "mc");

    /**
     * The shared skeleton in rules: the {@code :root} token block plus 38 chrome
     * rules — {@code panel}; {@code button} and its {@code :hover}/{@code :pressed}/
     * {@code :disabled}; {@code toggle} and its {@code :hover}/{@code :checked}/
     * {@code :checked:hover}/{@code :disabled}; {@code slider} and its
     * {@code ::part(track)}, {@code ::part(fill)}, {@code ::part(handle)} with the
     * handle's {@code :hover}/{@code :pressed}; {@code progress-bar} with
     * {@code ::part(track)}/{@code ::part(fill)}; {@code text-field};
     * {@code label, text}; {@code divider}; {@code .slot}; and the info-panel
     * block — {@code info-bar} with {@code ::part(track)}/{@code ::part(frame)}/
     * {@code ::part(fill)}/{@code ::part(mark)} and the four semantic segment
     * classes, {@code section-header} with {@code ::part(title)}/{@code ::part(rule)},
     * and {@code kv-row} with {@code ::part(label)}/{@code ::part(value)}. Every sheet
     * carries exactly this count, so a rule added to one theme alone shows up as a
     * diff.
     */
    private static final int skeletonRules = 39;

    /** {@code built-in(<ns:NAME>)} — the sprite id as written in the corpus. */
    private static final Pattern builtInReference = Pattern.compile("built-in\\(\\s*([^)\\s]+)\\s*\\)");

    /**
     * The LDLib2 property vocabulary the native rewrite dropped: the base/hover/
     * pressed state backgrounds and the mark/unmark pair. {@code hover-overlay} is
     * the one name the vocabulary kept — {@code .slot} styles it with
     * {@code var(--slot-overlay)} — so the ban lets that single selector through.
     */
    private static final Set<String> ldlibProperties = Set.of(
        "base-background",
        "hover-background",
        "pressed-background",
        "mark-background",
        "unmark-background",
        "hover-overlay"
    );

    /** The function names the vocabulary owns — a value starting with one is a texture. */
    private static final Set<String> textureFunctions = Set.of(
        "nine-slice",
        "sprite",
        "tiled",
        "color",
        "linear-gradient",
        "border-texture",
        "sdf",
        "rect",
        "group",
        "built-in",
        "empty",
        "none"
    );

    /**
     * The properties whose value is a texture. A value starting with one of
     * {@link #textureFunctions} is a texture declaration wherever it appears — the
     * theme's own {@code --*} tokens carry the surfaces — while these names make a
     * texture-shaped property whose value is <em>not</em> a texture function fail
     * instead of being read as a plain value.
     */
    private static final Set<String> textureProperties = Set
            .of("background", "icon", "arrow", "hover-overlay", "focus-overlay", "slot-overlay");

    /**
     * The controls every sheet must style, as the selector shape each one has to
     * carry. A sheet that drops a host, a state, a part or a semantic segment
     * class loses its entry here.
     */
    private static final Map<String, Predicate<StyleRule>> controlVocabulary = vocabulary();

    private static Map<String, Predicate<StyleRule>> vocabulary() {
        Map<String, Predicate<StyleRule>> out = new LinkedHashMap<>();
        out.put("panel", rule(bare("panel")));
        out.put("button", rule(bare("button")));
        out.put("button:hover", rule(state("button", "hover")));
        out.put("button:pressed", rule(state("button", "pressed")));
        out.put("button:disabled", rule(state("button", "disabled")));
        out.put("toggle", rule(bare("toggle")));
        out.put("toggle:checked", rule(state("toggle", "checked")));
        out.put("slider::part(track)", rule(part("slider", "track")));
        out.put("slider::part(fill)", rule(part("slider", "fill")));
        out.put("slider::part(handle)", rule(part("slider", "handle")));
        out.put("progress-bar::part(track)", rule(part("progress-bar", "track")));
        out.put("progress-bar::part(fill)", rule(part("progress-bar", "fill")));
        out.put("text-field", rule(bare("text-field")));
        out.put("label | text", rule(bare("label").or(bare("text"))));
        out.put("divider", rule(bare("divider")));
        out.put(".slot", rule(classOnly("slot")));
        // the info-panel block
        out.put("info-bar", rule(bare("info-bar")));
        out.put("info-bar::part(track)", rule(part("info-bar", "track")));
        out.put("info-bar::part(frame)", rule(part("info-bar", "frame")));
        out.put("info-bar::part(fill)", rule(part("info-bar", "fill")));
        out.put("info-bar::part(mark)", rule(part("info-bar", "mark")));
        for (String styleClass : List.of("ok", "warn", "danger", "info")) {
            out.put(
                "info-bar ." + styleClass + "::part(fill)",
                descendant(bare("info-bar"), classPart(styleClass, "fill"))
            );
        }
        out.put("section-header", rule(bare("section-header")));
        out.put("section-header::part(title)", rule(part("section-header", "title")));
        out.put("section-header::part(rule)", rule(part("section-header", "rule")));
        out.put("kv-row", rule(bare("kv-row")));
        out.put("kv-row::part(label)", rule(part("kv-row", "label")));
        out.put("kv-row::part(value)", rule(part("kv-row", "value")));
        return Collections.unmodifiableMap(out);
    }

    /** The sheet's source text, straight off the classpath. */
    private static String source(String theme) throws IOException {
        String path = "/assets/cloudlib/ui/themes/" + theme + "/" + theme + ".css";
        try (InputStream in = OreuiThemeCorpusTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing theme sheet " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Stylesheet sheet(String theme) throws IOException {
        return CssParser.parse(source(theme));
    }

    private static List<StyleRule> styleRules(Stylesheet stylesheet) {
        List<StyleRule> out = new ArrayList<>();
        for (Rule rule : stylesheet.rules()) {
            if (rule instanceof StyleRule styleRule) {
                out.add(styleRule);
            }
        }
        return out;
    }

    // region selector shapes

    /** Wraps a compound predicate so it can sit in the rule-shaped vocabulary. */
    private static Predicate<StyleRule> rule(Predicate<CompoundSelector> compound) {
        return rule -> matches(rule, compound);
    }

    /**
     * A selector of the shape {@code host <descendant> target} — the compound
     * form the segmented bar selects its semantic fills with,
     * {@code info-bar .danger::part(fill)}.
     */
    private static Predicate<StyleRule> descendant(
        Predicate<CompoundSelector> ancestor,
        Predicate<CompoundSelector> target
    ) {
        return rule -> {
            for (ComplexSelector selector : rule.selectors()) {
                if (selector.compounds().size() == 2
                        && selector.combinators().get(0) == Combinator.descendant
                        && ancestor.test(selector.compounds().get(0))
                        && target.test(selector.compounds().get(1))) {
                    return true;
                }
            }
            return false;
        };
    }

    /** True when one of the rule's selectors is a lone compound the predicate accepts. */
    private static boolean matches(StyleRule rule, Predicate<CompoundSelector> compound) {
        for (ComplexSelector selector : rule.selectors()) {
            if (selector.compounds().size() == 1 && compound.test(selector.compounds().get(0))) {
                return true;
            }
        }
        return false;
    }

    /** A bare widget tag — {@code panel}, {@code slider}, {@code text-field}. */
    private static Predicate<CompoundSelector> bare(String tag) {
        return compound -> tag.equals(compound.tag()) && compound.classes().isEmpty() && compound.pseudos().isEmpty();
    }

    /** A widget tag carrying exactly one state — {@code button:disabled}. */
    private static Predicate<CompoundSelector> state(String tag, String pseudo) {
        return compound -> tag.equals(compound.tag())
                && compound.classes().isEmpty()
                && compound.pseudos().size() == 1
                && hasPseudo(compound, pseudo);
    }

    /** A widget part — {@code slider::part(fill)}. */
    private static Predicate<CompoundSelector> part(String tag, String name) {
        return compound -> tag.equals(compound.tag())
                && compound.classes().isEmpty()
                && compound.pseudos().size() == 1
                && isPart(compound.pseudos().get(0), name);
    }

    /**
     * A class-selected part with no tag of its own — the {@code .danger::part(fill)}
     * half of {@code info-bar .danger::part(fill)}. The tag is dropped because a
     * part answers to its owner's tag, so a tag here would select the bar itself.
     */
    private static Predicate<CompoundSelector> classPart(String cls, String name) {
        return compound -> compound.tag() == null
                && compound.classes().equals(List.of(cls))
                && compound.pseudos().size() == 1
                && isPart(compound.pseudos().get(0), name);
    }

    private static boolean isPart(PseudoClass pseudo, String name) {
        return pseudo.element()
                && pseudo.name().equals("part")
                && pseudo.args() instanceof PseudoArgs.Idents ids
                && ids.values().contains(name);
    }

    /** The class a widget repeats itself with — {@code .slot}. */
    private static Predicate<CompoundSelector> classOnly(String cls) {
        return compound -> compound.tag() == null
                && compound.classes().equals(List.of(cls))
                && compound.pseudos().isEmpty();
    }

    private static boolean hasPseudo(CompoundSelector compound, String name) {
        for (PseudoClass pseudo : compound.pseudos()) {
            if (!pseudo.element() && pseudo.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    // endregion

    @Test
    void everyThemeKeepsTheSkeletonRuleCount() throws IOException {
        List<String> drift = new ArrayList<>();
        int rules = 0;
        for (String theme : themes) {
            int actual = styleRules(sheet(theme)).size();
            rules += actual;
            if (actual != skeletonRules) {
                drift.add(theme + ": expected " + skeletonRules + ", found " + actual);
            }
        }
        assertEquals(List.of(), drift, "sheets that no longer carry the shared skeleton");
        assertTrue(rules > 40, "the corpus has shrunk: " + rules + " rules");
    }

    @Test
    void everyThemeCoversTheControlVocabulary() throws IOException {
        List<String> missing = new ArrayList<>();
        int styled = 0;
        for (String theme : themes) {
            List<StyleRule> rules = styleRules(sheet(theme));
            for (Map.Entry<String, Predicate<StyleRule>> control : controlVocabulary.entrySet()) {
                if (rules.stream().anyMatch(control.getValue())) {
                    styled++;
                } else {
                    missing.add(theme + ": " + control.getKey());
                }
            }
        }
        assertEquals(List.of(), missing, "controls a sheet no longer styles");
        assertTrue(styled > 40, "the corpus has shrunk: " + styled + " styled controls");
    }

    @Test
    void noSheetKeepsTheLdlibPropertyVocabulary() throws IOException {
        List<String> leftovers = new ArrayList<>();
        int declarations = 0;
        for (String theme : themes) {
            for (StyleRule rule : styleRules(sheet(theme))) {
                boolean slot = matches(rule, classOnly("slot"));
                for (Declaration declaration : rule.declarations()) {
                    declarations++;
                    String property = declaration.property();
                    if (!ldlibProperties.contains(property)) {
                        continue;
                    }
                    if (property.equals("hover-overlay") && slot) {
                        continue; // the slot overlay is the one name the vocabulary kept
                    }
                    leftovers.add(theme + ": " + property);
                }
            }
        }
        assertEquals(List.of(), leftovers, "LDLib2 property names that survived the rewrite");
        assertTrue(declarations > 60, "the corpus has shrunk: " + declarations + " declarations");
    }

    @Test
    void noSelectorKeepsTheHostPseudo() throws IOException {
        List<String> leftovers = new ArrayList<>();
        int pseudos = 0;
        for (String theme : themes) {
            for (StyleRule rule : styleRules(sheet(theme))) {
                for (ComplexSelector selector : rule.selectors()) {
                    for (CompoundSelector compound : selector.compounds()) {
                        for (PseudoClass pseudo : compound.pseudos()) {
                            pseudos++;
                            if (pseudo.name().equalsIgnoreCase("host")) {
                                leftovers.add(theme + ": " + (pseudo.element() ? "::" : ":") + pseudo.name());
                            }
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), leftovers, "selectors that still target the removed Shadow-DOM host");
        assertTrue(pseudos > 24, "the corpus has shrunk: " + pseudos + " pseudo-classes");
    }

    @Test
    void everyTextureDeclarationParses() throws IOException {
        List<String> failures = new ArrayList<>();
        int declarations = 0;
        for (String theme : themes) {
            for (StyleRule rule : styleRules(sheet(theme))) {
                for (Declaration declaration : rule.declarations()) {
                    ComponentValue first = CssEnums.single(declaration.value());
                    if (!(first instanceof ComponentValue.Function function)) {
                        continue; // a bare colour or a length — not a texture declaration
                    }
                    String name = function.name().toLowerCase(Locale.ROOT);
                    if (name.equals("var")) {
                        continue; // a var() token stream, resolved at cascade time
                    }
                    boolean known = textureFunctions.contains(name);
                    if (!known && !textureProperties.contains(declaration.property())) {
                        continue; // a non-texture function — transform: translateY(1px)
                    }
                    declarations++;
                    VisualTexture texture = known ? CssTextures.parse(declaration.value()) : null;
                    if (texture == null) {
                        failures.add(
                            theme + ": " + declaration.property() + " = " + Tokens.serialize(declaration.value())
                        );
                    }
                }
            }
        }
        assertEquals(List.of(), failures, "texture declarations that no longer parse");
        assertTrue(declarations > 24, "the corpus has shrunk: " + declarations + " texture declarations");
    }

    @Test
    void everyReferencedBuiltInSpriteExists() throws IOException {
        List<String> unknown = new ArrayList<>();
        int references = 0;
        for (String theme : themes) {
            Matcher matcher = builtInReference.matcher(source(theme));
            while (matcher.find()) {
                references++;
                String id = matcher.group(1);
                if (BuiltInTextures.get(id) == null) {
                    unknown.add(theme + ": built-in(" + id + ")");
                }
            }
        }
        assertEquals(List.of(), unknown, "built-in ids missing from BuiltInTextures");
        assertTrue(references > 12, "the corpus has shrunk: " + references + " built-in references");
    }

    @Test
    void everyDeclaredPropertyResolvesInBuiltinKeys() throws IOException {
        List<String> unknown = new ArrayList<>();
        int declarations = 0;
        for (String theme : themes) {
            for (StyleRule rule : styleRules(sheet(theme))) {
                for (Declaration declaration : rule.declarations()) {
                    String property = declaration.property();
                    if (property.startsWith("--")) {
                        continue; // custom properties are style values read through StyleVar
                    }
                    declarations++;
                    if (BuiltinKeys.byId(property) == null && BuiltinKeys.shorthand(property) == null) {
                        unknown.add(theme + ": " + property);
                    }
                }
            }
        }
        assertEquals(List.of(), unknown, "properties outside the builtin vocabulary");
        assertTrue(declarations > 60, "the corpus has shrunk: " + declarations + " declarations");
    }
}
