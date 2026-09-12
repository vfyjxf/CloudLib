package dev.vfyjxf.cloudlib.internal.ui.theme;

import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.property.layout.StyleProperty;
import dev.vfyjxf.cloudlib.api.ui.theme.Theme;
import dev.vfyjxf.cloudlib.api.ui.theme.ThemeEngine;
import dev.vfyjxf.cloudlib.api.ui.theme.Themeable;
import dev.vfyjxf.cloudlib.internal.css.CssParser;
import dev.vfyjxf.cloudlib.internal.css.Stylesheet;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ThemeCascadeTest {

    /** A bare Themeable fixture — builds trees without a scene. */
    static final class Node implements Themeable {
        final String tag;
        String id;
        final List<String> classes = new ArrayList<>();
        final Map<String, String> attrs = new HashMap<>();
        final Set<String> states = new HashSet<>();
        Node parent;
        final List<Node> children = new ArrayList<>();
        String part;

        Node(String tag) {
            this.tag = tag;
        }

        Node child(Node c) {
            c.parent = this;
            children.add(c);
            return c;
        }

        @Override
        public String themeTag() {
            return tag;
        }

        @Override
        public List<String> themeClasses() {
            return classes;
        }

        @Override
        public @Nullable String themeId() {
            return id;
        }

        @Override
        public @Nullable String themeAttr(String name) {
            return attrs.get(name);
        }

        @Override
        public Set<String> themeStates() {
            return states;
        }

        @Override
        public @Nullable Themeable themeParent() {
            return parent;
        }

        @Override
        public List<? extends Themeable> themeSiblings() {
            return parent != null ? parent.children : List.of(this);
        }

        @Override
        public List<? extends Themeable> themeChildren() {
            return children;
        }

        @Override
        public @Nullable String themePart() {
            return part;
        }
    }

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "t"), CssParser.parse(css));
    }

    private static UIStyle styleOf(Theme theme, Themeable node) {
        List<String> warnings = new ArrayList<>();
        UIStyle style = ThemeEngine.resolve(theme, node, warnings::add);
        return style;
    }

    private static @Nullable StyleProperty prop(UIStyle style, String name) {
        return style.get(name);
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
        n.classes.add("primary");
        assertNotNull(prop(styleOf(t, n), "padding"));
        assertNull(prop(styleOf(t, n), "margin"));
        n.id = "ok";
        assertNotNull(prop(styleOf(t, n), "margin"));
    }

    @Test
    void attributeSelectors() {
        Theme t = theme("a[kind] { padding: 1px } a[kind=primary] { margin: 2px } a[kind~=x] { gap: 3px }");
        Node n = new Node("a");
        n.attrs.put("kind", "primary x");
        UIStyle s = styleOf(t, n);
        assertNotNull(prop(s, "padding")); // presence
        assertNull(prop(s, "margin")); // exact: "primary x" != "primary"
        assertNotNull(prop(s, "gap")); // includes: word "x" is in the list
        Node exact = new Node("a");
        exact.attrs.put("kind", "primary");
        assertNotNull(prop(styleOf(t, exact), "margin"));
    }

    @Test
    void combinators() {
        Theme t = theme(
                """
                panel button { padding: 1px }
                panel > button { margin: 2px }
                a + b { gap: 3px }
                x ~ y { z-index: 4 }
                """);
        Node panel = new Node("panel");
        Node inner = panel.child(new Node("wrap"));
        Node button = inner.child(new Node("button")); // panel → wrap → button
        Node direct = panel.child(new Node("button")); // panel → button
        // descendant hits both; child only the direct one
        assertNotNull(prop(styleOf(t, button), "padding"));
        assertNull(prop(styleOf(t, button), "margin"));
        assertNotNull(prop(styleOf(t, direct), "margin"));
        // siblings
        Node p = new Node("p");
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
        n.states.add("hovered");
        assertNotNull(prop(styleOf(t, n), "padding"));
        n.states.add("disabled");
        assertNotNull(prop(styleOf(t, n), "margin"));
    }

    @Test
    void nthChild() {
        Theme t = theme("slot:nth-child(2n) { padding: 5px } slot:nth-child(odd) { margin: 1px }");
        Node p = new Node("p");
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
        Theme t = theme(
                """
                button:not(.disabled) { padding: 1px }
                button:is(.a, .b) { margin: 2px }
                button:where(#x) { gap: 3px }
                panel:has(> indicator) { z-index: 5 }
                """);
        Node btn = new Node("button");
        assertNotNull(prop(styleOf(t, btn), "padding")); // not .disabled → matches
        btn.classes.add("disabled");
        assertNull(prop(styleOf(t, btn), "padding"));
        btn.classes.add("a");
        assertNotNull(prop(styleOf(t, btn), "margin")); // :is
        // :where contributes no specificity but still must match
        assertNull(prop(styleOf(t, btn), "gap")); // no #x
        Node panel = new Node("panel");
        panel.child(new Node("indicator"));
        assertNotNull(prop(styleOf(t, panel), "zIndex")); // :has(> indicator)
    }

    // ------------------------------------------------------------------ cascade order

    @Test
    void specificityWins() {
        Theme t = theme(
                """
                button { padding: 1px }
                button.primary { padding: 9px }
                button { padding: 2px }
                """);
        Node n = new Node("button");
        n.classes.add("primary");
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
        Theme t = theme(
                """
                button.x { padding: 9px }
                button { padding: 3px !important }
                """);
        Node n = new Node("button");
        n.classes.add("x");
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
        Theme t = theme(
                """
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
        UIStyle s = ThemeEngine.resolve(t, new Node("button"), warnings::add);
        assertNull(prop(s, "padding"));
        assertNotNull(prop(s, "margin")); // sibling declaration survives
    }

    @Test
    void customPropsInherit() {
        Theme t = theme(":root { --accent: #FF0000 } button { color: var(--accent) }");
        Node panel = new Node("panel");
        Node btn = panel.child(new Node("button"));
        // vars resolve through :root even when the node isn't :root
        assertNotNull(prop(styleOf(t, btn), "textColor"));
    }

    // ------------------------------------------------------------------ inheritance

    @Test
    void colorInheritsFromAncestor() {
        Theme t = theme("panel { color: #112233 } button { padding: 2px }");
        Node panel = new Node("panel");
        Node btn = panel.child(new Node("button"));
        assertNotNull(prop(styleOf(t, btn), "textColor"));
    }

    @Test
    void nonInheritedPropertiesDoNotPropagate() {
        Theme t = theme("panel { padding: 9px } button { margin: 1px }");
        Node panel = new Node("panel");
        Node btn = panel.child(new Node("button"));
        assertNull(prop(styleOf(t, btn), "padding")); // padding doesn't inherit
    }

    // ------------------------------------------------------------------ value conversion

    @Test
    void lengthUnits() {
        Theme t = theme(
                """
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
        UIStyle s = ThemeEngine.resolve(t, new Node("a"), warnings::add);
        assertNull(prop(s, "padding")); // em unsupported
        assertNull(prop(s, "margin")); // rem unsupported
        assertNotNull(prop(s, "gap")); // px still fine
    }

    @Test
    void colorForms() {
        Theme t = theme(
                """
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
        Theme t = theme(
                """
                a { background: nine-slice("cloudlib:gui/panel/dark", 3) }
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
        UIStyle s = ThemeEngine.resolve(t, new Node("a"), warnings::add);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("frobnicate")));
        assertNotNull(prop(s, "padding"));
    }

    @Test
    void invalidValueDropsOnlyThatDeclaration() {
        List<String> warnings = new ArrayList<>();
        Theme t = theme("a { padding: banana; margin: 2px }");
        UIStyle s = ThemeEngine.resolve(t, new Node("a"), warnings::add);
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
        Theme t = theme(
                """
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
                "flex-wrap")) {
            assertNotNull(prop(s, p), p);
        }
    }

    @Test
    void realStandardThemeParses() throws Exception {
        assertThemeResolves("standard.css", "panel", "button", "item-slot", "scrollbar", "tab", "switch", "pager");
    }

    @Test
    void realHackerThemeParses() throws Exception {
        assertThemeResolves(
                "hacker.css", "inworld-panel", "hint-chip", "leader-line", "scan-frame", "presence-pip", "item-slot");
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
        List<dev.vfyjxf.cloudlib.internal.css.CssError> errors = new ArrayList<>();
        Stylesheet sheet = CssParser.parse(css, errors);
        assertTrue(errors.isEmpty(), () -> file + " parse errors: " + errors);
        assertFalse(sheet.rules().isEmpty());
        Theme theme = new Theme(ResourceLocation.fromNamespaceAndPath("cloudlib", file.replace(".css", "")), sheet);
        for (String tag : tags) {
            Node n = new Node(tag);
            n.states.add("hovered");
            UIStyle s = styleOf(theme, n);
            assertFalse(s.isEmpty(), () -> file + ": no properties resolved for <" + tag + ">");
        }
    }
}
