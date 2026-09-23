package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleCollector;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValues;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for the {@link StyleKey}/{@link StyleValue} style system —
 * registration, shorthand expansion, the three-segment apply order, and the
 * java-dsl ⇄ css equivalence.
 */
class StyleSystemTest {

    /** Leaf node with a fixed selector tag. */
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

    private static Theme theme(String css) {
        return new Theme(ResourceLocation.fromNamespaceAndPath("test", "t"), CssParser.parse(css));
    }

    // ------------------------------------------------------------------ registry

    @Test
    void keysResolveByCssNameAndAliases() {
        assertSame(Styles.paddingLeft, Styles.byId("padding-left"));
        assertSame(Styles.color, Styles.byId("text-color")); // alias
        assertSame(Styles.color, Styles.byId("textcolor")); // alias
        assertSame(Styles.zIndex, Styles.byId("zindex")); // alias
        assertSame(Styles.boxShadow, Styles.byId("shadow")); // alias
        assertNull(Styles.byId("padding")); // shorthand is not a key
        assertNull(Styles.byId("frobnicate"));
    }

    @Test
    void everyBuiltinKeyIsIndexed() {
        // the constant table and the index must agree
        var all = Styles.all();
        assertTrue(all.contains(Styles.paddingLeft));
        assertTrue(all.size() > 60, "expected 60+ builtin keys, got " + all.size());
        for (StyleKey<?> key : all) {
            assertSame(key, Styles.byId(key.id()), key.id());
        }
    }

    // ------------------------------------------------------------------ dsl

    @Test
    void boxFactoryExpandsToFourLonghands() {
        StyleValues group = padding(4);
        List<StyleValue<?>> values = new ArrayList<>();
        group.collectInto(new StyleCollector() {
            @Override
            public void accept(StyleValue<?> value) {
                values.add(value);
            }

            @Override
            public void var(String name, Tokens value) {}
        });
        assertEquals(4, values.size());
        assertTrue(values.stream().anyMatch(v -> v.key() == Styles.paddingLeft));
        assertTrue(values.stream().anyMatch(v -> v.key() == Styles.paddingRight));
        assertTrue(values.stream().anyMatch(v -> v.key() == Styles.paddingTop));
        assertTrue(values.stream().anyMatch(v -> v.key() == Styles.paddingBottom));
    }

    @Test
    void javaDslWritesIntoTheStyleContext() {
        Node node = new Node("a");
        node.useStyle(padding(4), displayFlex());
        assertEquals(4f, node.style().layoutStyle().padding.top.getValue());
        assertEquals(4f, node.style().layoutStyle().padding.left.getValue());
        assertEquals(TaffyDisplay.FLEX, node.style().layoutStyle().display);

        node.set(Styles.paddingLeft, LengthPercentage.length(9f));
        assertEquals(9f, node.style().layoutStyle().padding.left.getValue());
        assertEquals(4f, node.style().layoutStyle().padding.right.getValue()); // others untouched
    }

    @Test
    void uiStyleLastWriteWins() {
        UIStyle s = UIStyle.of(padding(4), paddingLeft(9));
        StyleValue<?> v = s.get(Styles.paddingLeft);
        assertNotNull(v);
        assertEquals(9f, ((LengthPercentage) v.value()).getValue());
    }

    // ------------------------------------------------------------------ segments

    @Test
    void segmentsApplyInOrder() {
        Node node = new Node("a");
        node.defaultStyle(UIStyle.of(padding(2), displayFlex()));
        // theme writes over defaults, code over both
        node.applyThemeStyle(UIStyle.of(padding(6)));
        node.useStyle(paddingLeft(11));
        assertEquals(6f, node.style().layoutStyle().padding.top.getValue()); // theme wins over default
        assertEquals(11f, node.style().layoutStyle().padding.left.getValue()); // code wins over theme
    }

    @Test
    void themeRefreshKeepsCodeStyles() {
        Node node = new Node("a");
        node.useStyle(margin(7));
        node.applyThemeStyle(UIStyle.of(padding(3)));
        assertEquals(3f, node.style().layoutStyle().padding.top.getValue());
        assertEquals(7f, node.style().layoutStyle().margin.top.getValue()); // code replayed on top
    }

    // ------------------------------------------------------------------ css ⇄ dsl equivalence

    @Test
    void cssShorthandExpandsToLonghands() {
        Theme t = theme("a { padding: 4px }");
        UIStyle s = t.resolve(new Node("a"));
        for (StyleKey<?> key : List
                .of(Styles.paddingTop, Styles.paddingRight, Styles.paddingBottom, Styles.paddingLeft)) {
            StyleValue<?> v = s.get(key);
            assertNotNull(v, key.id());
            assertEquals(4f, ((LengthPercentage) v.value()).getValue());
        }
    }

    @Test
    void cssLonghandAfterShorthandWinsItsEdge() {
        Theme t = theme("a { padding: 2px; padding-left: 9px }");
        UIStyle s = t.resolve(new Node("a"));
        assertEquals(2f, ((LengthPercentage) s.get(Styles.paddingTop).value()).getValue());
        assertEquals(9f, ((LengthPercentage) s.get(Styles.paddingLeft).value()).getValue());
    }

    @Test
    void cssShorthandAfterLonghandWinsAllEdges() {
        Theme t = theme("a { padding-left: 9px; padding: 2px }");
        UIStyle s = t.resolve(new Node("a"));
        assertEquals(2f, ((LengthPercentage) s.get(Styles.paddingLeft).value()).getValue());
        assertEquals(2f, ((LengthPercentage) s.get(Styles.paddingTop).value()).getValue());
    }

    @Test
    void javaAndCssProduceEquivalentStyles() {
        Theme t = theme("a { padding: 4px; display: flex; gap: 6px }");
        UIStyle css = t.resolve(new Node("a"));
        UIStyle java = UIStyle.of(padding(4), displayFlex(), gap(6));
        // same key set
        for (StyleValue<?> v : css.values()) {
            assertTrue(java.has(v.key()), "css produced " + v.key().id() + " missing from java");
        }
        for (StyleValue<?> v : java.values()) {
            assertTrue(css.has(v.key()), "java produced " + v.key().id() + " missing from css");
        }
    }

    // ------------------------------------------------------------------ closed vocabulary

    @Test
    void styleKeyIsNotPubliclyConstructible() {
        // the builtin vocabulary is closed — StyleKey offers no accessible ctor
        for (var ctor : StyleKey.class.getDeclaredConstructors()) {
            assertFalse(
                Modifier.isPublic(ctor.getModifiers()) || Modifier.isProtected(ctor.getModifiers()),
                "StyleKey ctor must not be accessible: " + ctor
            );
        }
    }
}
