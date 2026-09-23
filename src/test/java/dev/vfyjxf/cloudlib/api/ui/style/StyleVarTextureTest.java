package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParseContext;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.RoundedRectTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TintedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link StyleVar#texture(String)} — the surface slot: a texture function with its
 * modifier chain, a bare {@code <color>} promoted to a solid texture, and the writer
 * that turns a texture back into css text.
 */
class StyleVarTextureTest {

    private static final StyleVar<VisualTexture> surface = StyleVar.texture("--surface");

    /** Leaf node — the smallest thing the cascade can resolve a theme for. */
    static class Node extends Widget {
        @Override
        public String styleTag() {
            return "panel";
        }
    }

    private static @Nullable VisualTexture parse(String cssValue) {
        return surface.parse(CssParser.parseValueList(cssValue), StyleParseContext.plain());
    }

    /** The value a real theme declaration resolves to, through the cascade. */
    private static @Nullable VisualTexture resolve(String css) {
        Theme theme = new Theme(ResourceLocation.fromNamespaceAndPath("test", "t"), CssParser.parse(css));
        return theme.resolve(new Node(), warning -> {}).var(surface);
    }

    // ------------------------------------------------------------------ the bare-color promotion

    @Test
    void bareHexIsPromotedToASolidTexture() {
        assertEquals(new ColorTexture(0xC0000000), parse("#000000C0"));
        assertEquals(new ColorTexture(0xFFFF0000), parse("red"));
        assertEquals(new ColorTexture(0xFFFF0000), parse("color(#FF0000)"));
    }

    @Test
    void emptyAndNoneAreTheEmptySentinel() {
        assertSame(VisualTexture.empty, parse("empty"));
        assertSame(VisualTexture.empty, parse("none"));
    }

    @Test
    void textureFunctionsParseAsThemselves() {
        assertTrue(parse("sdf(#102030, 4)") instanceof RoundedRectTexture);
        assertTrue(parse("nine-slice(\"cloudlib:gui/panel\", 3)") instanceof NineSliceTexture);
        assertTrue(parse("sprite(\"cloudlib:gui/x\", 16, 16) color(#FFFFFF80)") instanceof TintedTexture);
    }

    @Test
    void garbageValuesAreRejected() {
        assertNull(parse("banana"));
        assertNull(parse("3px"));
        assertNull(parse("sdf(4)"));
        assertNull(parse("wobble(#FF0000)"));
    }

    // ------------------------------------------------------------------ through the cascade

    @Test
    void varReferencesResolveBeforeTheLensParses() {
        assertEquals(new ColorTexture(0xFFFF00FF), resolve("panel { --tone: #FF00FF; --surface: var(--tone); }"));
        assertTrue(resolve("panel { --r: 4; --surface: sdf(#102030, var(--r)); }") instanceof RoundedRectTexture);
    }

    @Test
    void fallbackStandsInForAnUndeclaredOrInvalidProperty() {
        StyleVar<VisualTexture> fallback = surface.orElse(new ColorTexture(0xFF123456));
        Theme theme = new Theme(
            ResourceLocation.fromNamespaceAndPath("test", "t"),
            CssParser.parse("panel { --surface: banana; }")
        );
        assertEquals(new ColorTexture(0xFF123456), theme.resolve(new Node(), warning -> {}).var(fallback));
    }

    // ------------------------------------------------------------------ the writer

    @Test
    void aTextureWritesBackAsCssTextThatReparsesToIt() {
        for (String declaration : new String[]{"#000000C0", "red", "color(#12345678)", "sdf(#102030, 4)",
                "sdf(#102030, 1 2 3 4, 2, #AABBCC)", "nine-slice(\"cloudlib:gui/panel\", 3 4 5 6, 18, 18)",
                "sprite(\"cloudlib:gui/x\", 16, 16)", "sprite(\"cloudlib:gui/sheet\", 4, 8, 16, 16)",
                "tiled(\"cloudlib:gui/t\", 8, 8)", "linear-gradient(#FF0000, #0000FF, vertical)",
                "linear-gradient(#FF0000, #0000FF)", "border-texture(#FF000080, 2)",
                "color(#FFFFFFFF) scale(2) translate(3, 4) color(#FF0000)", "empty"}) {
            VisualTexture parsed = parse(declaration);
            assertNotNull(parsed, declaration + " parses");
            assertEquals(parsed, parse(surface.write(parsed)), declaration + " round-trips through its own text");
        }
    }

    @Test
    void aComposedTextureWritesItsLayersInOrder() {
        VisualTexture group = parse("group(color(#FF0000), sdf(#00FF00, 2))");
        assertEquals("group(color(#FF0000), sdf(#00FF00, 2, 0, #000000))", surface.write(group));
    }
}
