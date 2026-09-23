package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.CssError;
import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Stylesheet;
import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThemeCascadeTest {

    /** A leaf test node — a real {@link Widget} with a fixed selector tag. */
    static class Node extends Widget {
        private final String tag;

        Node(String tag) {
            this.tag = tag;
        }

        @Override
        public String styleTag() {
            return tag;
        }
    }

    /** A composite fixture — exposes the protected {@code addWidget}. */
    static class Panel extends CompositeWidget<Widget> {
        private final String tag;

        Panel(String tag) {
            this.tag = tag;
        }

        <W extends Widget> W child(W w) {
            addWidget(w);
            return w;
        }

        @Override
        public String styleTag() {
            return tag;
        }
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "t"), CssParser.parse(css));
    }

    private static UIStyle styleOf(Theme theme, Widget node) {
        List<String> warnings = new ArrayList<>();
        return theme.resolve(node, warnings::add);
    }

    /**
     * Resolves a (possibly shorthand / aliased) css name to its winning
     * {@link StyleValue} — box shorthands land on their {@code -top} longhand.
     */
    private static @Nullable StyleValue<?> prop(UIStyle style, String name) {
        String mapped = switch (name) {
            case "padding", "margin", "inset", "border-width" -> name + "-top";
            case "size" -> "width";
            case "min-size" -> "min-width";
            case "max-size" -> "max-width";
            case "gap" -> "row-gap";
            case "overflow" -> "overflow-x";
            case "flex-flow" -> "flex-direction";
            case "border" -> "border-top-width";
            case "zIndex" -> "z-index";
            case "textColor" -> "color";
            default -> name;
        };
        var key = Styles.byId(mapped);
        return key == null ? null : style.get(key);
    }

    // ------------------------------------------------------------------ matching

    @Test
    void tagMatches() {
        Theme t = theme("button { padding: 4px }");
        Node button = new Node("button");
        Node label = new Node("label");
        assertNotNull(prop(styleOf(t, button), "padding"));
        assertTrue(styleOf(t, label).isEmpty());
    }

    @Test
    void classAndIdMatch() {
        Theme t = theme(".primary { padding: 4px } #ok { margin: 2px }");
        Node n = new Node("button");
        n.addStyleClass("primary");
        assertNotNull(prop(styleOf(t, n), "padding"));
        assertNull(prop(styleOf(t, n), "margin"));
        n.styleId("ok");
        assertNotNull(prop(styleOf(t, n), "margin"));
    }

    @Test
    void attributeSelectors() {
        Theme t = theme("a[kind] { padding: 1px } a[kind=primary] { margin: 2px } a[kind~=x] { gap: 3px }");
        Node n = new Node("a");
        n.styleAttr("kind", "primary x");
        UIStyle s = styleOf(t, n);
        assertNotNull(prop(s, "padding")); // presence
        assertNull(prop(s, "margin")); // exact: "primary x" != "primary"
        assertNotNull(prop(s, "gap")); // includes: word "x" is in the list
        Node exact = new Node("a");
        exact.styleAttr("kind", "primary");
        assertNotNull(prop(styleOf(t, exact), "margin"));
    }

    @Test
    void combinators() {
        Theme t = theme("""
                panel button { padding: 1px }
                panel > button { margin: 2px }
                a + b { gap: 3px }
                x ~ y { z-index: 4 }
                """);
        Panel panel = new Panel("panel");
        Panel inner = panel.child(new Panel("wrap"));
        Node button = inner.child(new Node("button")); // panel → wrap → button
        Node direct = panel.child(new Node("button")); // panel → button
        // descendant hits both; child only the direct one
        assertNotNull(prop(styleOf(t, button), "padding"));
        assertNull(prop(styleOf(t, button), "margin"));
        assertNotNull(prop(styleOf(t, direct), "margin"));
        // siblings
        Panel p = new Panel("p");
        Node a = p.child(new Node("a"));
        Node b = p.child(new Node("b"));
        assertNotNull(prop(styleOf(t, b), "gap"));
        Node x = p.child(new Node("x"));
        Node y = p.child(new Node("y"));
        assertNotNull(prop(styleOf(t, y), "zIndex"));
    }

    @Test
    void pseudoStates() {
        Theme t = theme("button:hovered { padding: 8px } button:disabled { margin: 1px }");
        Node n = new Node("button");
        assertTrue(styleOf(t, n).isEmpty());
        n.addStyleState("hovered");
        assertNotNull(prop(styleOf(t, n), "padding"));
        n.addStyleState("disabled");
        assertNotNull(prop(styleOf(t, n), "margin"));
    }

    @Test
    void nthChild() {
        Theme t = theme("slot:nth-child(2n) { padding: 5px } slot:nth-child(odd) { margin: 1px }");
        Panel p = new Panel("p");
        Node s1 = p.child(new Node("slot"));
        Node s2 = p.child(new Node("slot"));
        Node s3 = p.child(new Node("slot"));
        assertNull(prop(styleOf(t, s1), "padding"));
        assertNotNull(prop(styleOf(t, s1), "margin"));
        assertNotNull(prop(styleOf(t, s2), "padding"));
        assertNull(prop(styleOf(t, s2), "margin"));
        assertNull(prop(styleOf(t, s3), "padding"));
        assertNotNull(prop(styleOf(t, s3), "margin"));
    }

    @Test
    void notIsWhereHas() {
        Theme t = theme("""
                button:not(.disabled) { padding: 1px }
                button:is(.a, .b) { margin: 2px }
                button:where(#x) { gap: 3px }
                panel:has(> indicator) { z-index: 5 }
                """);
        Node btn = new Node("button");
        assertNotNull(prop(styleOf(t, btn), "padding")); // not .disabled → matches
        btn.addStyleClass("disabled");
        assertNull(prop(styleOf(t, btn), "padding"));
        btn.addStyleClass("a");
        assertNotNull(prop(styleOf(t, btn), "margin")); // :is
        // :where contributes no specificity but still must match
        assertNull(prop(styleOf(t, btn), "gap")); // no #x
        Panel panel = new Panel("panel");
        panel.child(new Node("indicator"));
        assertNotNull(prop(styleOf(t, panel), "zIndex")); // :has(> indicator)
    }

    // ------------------------------------------------------------------ cascade order

    @Test
    void specificityWins() {
        Theme t = theme("""
                button { padding: 1px }
                button.primary { padding: 9px }
                button { padding: 2px }
                """);
        Node n = new Node("button");
        n.addStyleClass("primary");
        UIStyle s = styleOf(t, n);
        var p = prop(s, "padding");
        assertNotNull(p);
        // specificity (0,1,1) beats (0,0,1) regardless of source order
        assertTrue(p.toString().contains("9") || p.toString().contains("9.0"));
    }

    @Test
    void sourceOrderBreaksTies() {
        Theme t = theme("button { padding: 1px } button { padding: 7px }");
        var p = prop(styleOf(t, new Node("button")), "padding");
        assertNotNull(p);
        assertTrue(p.toString().contains("7"));
    }

    @Test
    void importantBeatsSpecificity() {
        Theme t = theme("""
                button.x { padding: 9px }
                button { padding: 3px !important }
                """);
        Node n = new Node("button");
        n.addStyleClass("x");
        var p = prop(styleOf(t, n), "padding");
        assertNotNull(p);
        assertTrue(p.toString().contains("3"));
    }

    @Test
    void laterThemeLayerOverrides() {
        // stack semantics: two themes, later in stack wins at equal specificity
        Theme base = theme("button { padding: 1px }");
        Theme over = theme("button { padding: 8px }");
        Node n = new Node("button");
        // emulate the stack by cascading both sheets in order
        Stylesheet merged = new Stylesheet(new ArrayList<>() {
            {
                addAll(base.sheet().rules());
                addAll(over.sheet().rules());
            }
        });
        Theme stacked = new Theme(ResourceLocation.fromNamespaceAndPath("test", "stack"), merged);
        var p = prop(styleOf(stacked, n), "padding");
        assertTrue(p.toString().contains("8"));
    }

    // ------------------------------------------------------------------ var() and custom properties

    @Test
    void varSubstitution() {
        Theme t = theme("""
                :root { --pad: 6px; --accent: #35D6D0 }
                button { padding: var(--pad); color: var(--accent) }
                """);
        UIStyle s = styleOf(t, new Node("button"));
        assertNotNull(prop(s, "padding"));
        assertNotNull(prop(s, "textColor"));
    }

    @Test
    void varFallbackUsedWhenMissing() {
        Theme t = theme("button { padding: var(--missing, 4px) }");
        assertNotNull(prop(styleOf(t, new Node("button")), "padding"));
    }

    @Test
    void unresolvedVarDropsDeclaration() {
        List<String> warnings = new ArrayList<>();
        Theme t = theme("button { padding: var(--missing) } button { margin: 2px }");
        UIStyle s = t.resolve(new Node("button"), warnings::add);
        assertNull(prop(s, "padding"));
        assertNotNull(prop(s, "margin")); // sibling declaration survives
    }

    @Test
    void customPropsInherit() {
        Theme t = theme(":root { --accent: #FF0000 } button { color: var(--accent) }");
        Panel panel = new Panel("panel");
        Node btn = panel.child(new Node("button"));
        // vars resolve through :root even when the node isn't :root
        assertNotNull(prop(styleOf(t, btn), "textColor"));
    }

    @Test
    void customPropsLandInStyleVars() {
        Theme t = theme("button { --pad: 6px; --accent: #35D6D0 }");
        UIStyle s = styleOf(t, new Node("button"));
        assertNotNull(s.varRaw("--pad"));
        assertEquals("6px", s.varRaw("--pad").text().trim());
        assertEquals("#35D6D0", s.varRaw("--accent").text().trim());
    }

    @Test
    void customPropsInheritIntoVars() {
        Theme t = theme(":root { --accent: #FF0000 }");
        Panel panel = new Panel("panel");
        Node btn = panel.child(new Node("button"));
        // --* inherits unconditionally: the child's vars carry the root binding
        UIStyle s = styleOf(t, btn);
        assertNotNull(s.varRaw("--accent"));
        assertEquals("#FF0000", s.varRaw("--accent").text().trim());
    }

    @Test
    void inlineVarWinsOverThemeVar() {
        Theme t = theme("button { --pad: 1px; padding: var(--pad) }");
        Node n = new Node("button");
        n.setVar("--pad", "9px");
        UIStyle s = styleOf(t, n);
        var p = prop(s, "padding");
        assertNotNull(p);
        assertTrue(p.toString().contains("9")); // the inline binding feeds var()
        assertEquals("9px", s.varRaw("--pad").text().trim());
    }

    @Test
    void varChainedReferences() {
        Theme t = theme("button { --a: 4px; --b: var(--a); padding: var(--b) }");
        UIStyle s = styleOf(t, new Node("button"));
        var p = prop(s, "padding");
        assertNotNull(p);
        assertTrue(p.toString().contains("4"));
        assertEquals("4px", s.varRaw("--b").text().trim()); // --b resolves to --a's tokens
    }

    @Test
    void varCycleDropsDeclaration() {
        Theme t = theme("button { --a: var(--b); --b: var(--a); padding: var(--a); margin: 2px }");
        UIStyle s = styleOf(t, new Node("button"));
        assertNull(prop(s, "padding")); // cyclic --a poisons the consumer
        assertNull(s.varRaw("--a")); // cyclic vars emit nothing
        assertNull(s.varRaw("--b"));
        assertNotNull(prop(s, "margin")); // unrelated declarations survive
    }

    @Test
    void styleVarTypedRead() {
        Theme t = theme("button { --accent: #35D6D0; --scale: 1.5 }");
        UIStyle s = styleOf(t, new Node("button"));
        var accent = StyleVar.color("--accent");
        var scale = StyleVar.number("--scale");
        assertEquals(0xFF35D6D0, s.var(accent));
        assertEquals(1.5f, s.var(scale));
        var missing = StyleVar.number("--missing").orElse(3f);
        assertEquals(3f, s.var(missing)); // fallback on unset
    }

    // ------------------------------------------------------------------ inheritance

    @Test
    void colorInheritsFromAncestor() {
        Theme t = theme("panel { color: #112233 } button { padding: 2px }");
        Panel panel = new Panel("panel");
        Node btn = panel.child(new Node("button"));
        assertNotNull(prop(styleOf(t, btn), "textColor"));
    }

    @Test
    void nonInheritedPropertiesDoNotPropagate() {
        Theme t = theme("panel { padding: 9px } button { margin: 1px }");
        Panel panel = new Panel("panel");
        Node btn = panel.child(new Node("button"));
        assertNull(prop(styleOf(t, btn), "padding")); // padding doesn't inherit
    }

    // ------------------------------------------------------------------ value conversion

    @Test
    void lengthUnits() {
        Theme t = theme("""
                a { padding: 4px }
                b { padding: 50% }
                c { width: calc(50% - 4px) }
                d { width: min-content }
                e { width: stretch }
                """);
        assertNotNull(prop(styleOf(t, new Node("a")), "padding"));
        assertNotNull(prop(styleOf(t, new Node("b")), "padding"));
        assertNotNull(prop(styleOf(t, new Node("c")), "size"));
        assertNotNull(prop(styleOf(t, new Node("d")), "size"));
        assertNotNull(prop(styleOf(t, new Node("e")), "size"));
    }

    @Test
    void unsupportedUnitsDrop() {
        List<String> warnings = new ArrayList<>();
        Theme t = theme("a { padding: 2em; margin: 1rem; gap: 4px }");
        UIStyle s = t.resolve(new Node("a"), warnings::add);
        assertNull(prop(s, "padding")); // em unsupported
        assertNull(prop(s, "margin")); // rem unsupported
        assertNotNull(prop(s, "gap")); // px still fine
    }

    @Test
    void colorForms() {
        Theme t = theme("""
                a { color: #FF0000 }
                b { color: #35D6D0FF }
                c { color: rgb(255, 0, 0) }
                d { color: red }
                e { color: #F00F }
                """);
        for (String tag : List.of("a", "b", "c", "d", "e")) {
            assertNotNull(prop(styleOf(t, new Node(tag)), "textColor"), tag);
        }
    }

    @Test
    void textureFunctions() {
        Theme t = theme("""
                a { background: nine-slice("cloudlib:gui/background/dark", 3) }
                b { background: color(#102030) }
                c { background: linear-gradient(#000, #FFF, vertical) }
                d { background: tiled("cloudlib:gui/x", 16px, 16px) }
                e { background: sprite("cloudlib:gui/y", 16px, 16px) }
                """);
        for (String tag : List.of("a", "b", "c", "d", "e")) {
            assertNotNull(prop(styleOf(t, new Node(tag)), "background"), tag);
        }
    }

    @Test
    void unknownPropertyWarnsAndSkips() {
        List<String> warnings = new ArrayList<>();
        Theme t = theme("a { frobnicate: 3px; padding: 4px }");
        UIStyle s = t.resolve(new Node("a"), warnings::add);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("frobnicate")));
        assertNotNull(prop(s, "padding"));
    }

    @Test
    void invalidValueDropsOnlyThatDeclaration() {
        List<String> warnings = new ArrayList<>();
        Theme t = theme("a { padding: banana; margin: 2px }");
        UIStyle s = t.resolve(new Node("a"), warnings::add);
        assertNull(prop(s, "padding"));
        assertNotNull(prop(s, "margin"));
    }

    @Test
    void edgeShorthands() {
        Theme t = theme("a { padding: 1px 2px; margin: 1px 2px 3px; inset: 0 4px 2px 8px }");
        UIStyle s = styleOf(t, new Node("a"));
        assertNotNull(prop(s, "padding"));
        assertNotNull(prop(s, "margin"));
        assertNotNull(prop(s, "inset"));
    }

    @Test
    void enumProperties() {
        Theme t = theme("""
                a { display: flex; flex-direction: row; align-items: center;
                    position: absolute; overflow: hidden scroll; box-sizing: content-box;
                    text-align: justify-all; direction: rtl; flex-wrap: wrap-reverse }
                """);
        UIStyle s = styleOf(t, new Node("a"));
        for (String p : List.of(
            "display",
            "flex-direction",
            "align-items",
            "position",
            "overflow",
            "box-sizing",
            "text-align",
            "direction",
            "flex-wrap"
        )) {
            assertNotNull(prop(s, p), p);
        }
    }

    @Test
    void realStandardThemeParses() throws Exception {
        assertThemeResolves("standard/base.css", "panel", "button", "toggle", "divider", "label", "check");
    }

    @Test
    void baseCssParsesAndStylesWidgets() throws Exception {
        String path = "/assets/cloudlib/ui/themes/standard/base.css";
        var in = ThemeCascadeTest.class.getResourceAsStream(path);
        assertNotNull(in, path + " missing from classpath");
        List<CssError> errors = new ArrayList<>();
        Stylesheet sheet = CssParser.parse(new String(in.readAllBytes()), errors);
        assertTrue(errors.isEmpty(), () -> "base.css parse errors: " + errors);
        Theme theme = new Theme(ResourceLocation.fromNamespaceAndPath("cloudlib", "standard"), sheet);

        Node button = new Node("button");
        UIStyle s = styleOf(theme, button);
        assertFalse(s.isEmpty(), "base.css resolves nothing for button");
        assertNotNull(prop(s, "background"), "button background did not resolve");

        Node hover = new Node("button");
        hover.addStyleState("hover");
        assertNotNull(prop(styleOf(theme, hover), "background"), "button:hover background did not resolve");
    }

    /**
     * Dogfood: load the bundled css, then resolve every declared tag and assert
     * each produces at least one style property — catches bad value shapes and
     * unknown functions across the whole file.
     */
    private static void assertThemeResolves(String file, String... tags) throws Exception {
        String path = "/assets/cloudlib/ui/themes/" + file;
        var in = ThemeCascadeTest.class.getResourceAsStream(path);
        assertNotNull(in, path + " missing from classpath");
        String css = new String(in.readAllBytes());
        List<CssError> errors = new ArrayList<>();
        Stylesheet sheet = CssParser.parse(css, errors);
        assertTrue(errors.isEmpty(), () -> file + " parse errors: " + errors);
        assertFalse(sheet.rules().isEmpty());
        Theme theme = new Theme(ResourceLocation.fromNamespaceAndPath("cloudlib", file.replace(".css", "")), sheet);
        for (String tag : tags) {
            Node n = new Node(tag);
            n.addStyleState("hovered");
            UIStyle s = styleOf(theme, n);
            assertFalse(s.isEmpty(), () -> file + ": no properties resolved for <" + tag + ">");
        }
    }
}
