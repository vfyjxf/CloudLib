package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.CssParser;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.Styles;
import dev.vfyjxf.cloudlib.api.ui.style.Theme;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.texture.BatchableTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.BuiltInTextures;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.CompositeTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.RoundedRectTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.SpriteTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TintedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TransformedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The texture function vocabulary and its modifier chain — every rule the theme
 * value grammar exposes, checked on the parsed value and (for the modifiers) on
 * the primitives the texture pushes into a batch.
 */
class CssTexturesTest {

    /** JUnit 5.7 lacks assertInstanceOf — same contract: fail on type mismatch, else cast. */
    @SuppressWarnings("unchecked")
    private static <T> T cast(Class<T> type, Object value) {
        assertNotNull(value, "expected " + type.getSimpleName() + ", got null");
        assertTrue(
            type.isInstance(value),
            "expected " + type.getSimpleName() + " but got " + value.getClass().getSimpleName()
        );
        return (T) value;
    }

    private static @Nullable VisualTexture parse(String value) {
        return CssTextures.parse(CssParser.parseValueList(value));
    }

    /** A leaf node — the smallest thing the cascade can resolve a theme for. */
    static class Node extends Widget {
        @Override
        public String styleTag() {
            return "panel";
        }
    }

    /** The texture a real theme declaration resolves to, through the style key. */
    private static @Nullable VisualTexture resolveBackground(String declaration) {
        Theme theme = new Theme(
            ResourceLocation.fromNamespaceAndPath("test", "t"),
            CssParser.parse("panel { background: " + declaration + " }")
        );
        UIStyle style = theme.resolve(new Node(), warning -> {});
        StyleValue<?> value = style.get(Styles.byId("background"));
        return value == null ? null : cast(VisualTexture.class, value.value());
    }

    // ------------------------------------------------------------------ sdf / rect

    @Test
    void sdfSingleRadius() {
        RoundedRectTexture rect = cast(RoundedRectTexture.class, parse("sdf(#FF00FF, 4)"));
        assertEquals(0xFFFF00FF, rect.fillColor());
        assertEquals(4, rect.radiusTopLeft());
        assertEquals(4, rect.radiusTopRight());
        assertEquals(4, rect.radiusBottomLeft());
        assertEquals(4, rect.radiusBottomRight());
        assertEquals(0, rect.borderThickness());
    }

    @Test
    void sdfFourRadiiKeepLdlibOrder() {
        // LDLib2's order is bottom-left, bottom-right, top-right, top-left
        RoundedRectTexture rect = cast(RoundedRectTexture.class, parse("sdf(#FF00FF, 1 2 3 4)"));
        assertEquals(1, rect.radiusBottomLeft());
        assertEquals(2, rect.radiusBottomRight());
        assertEquals(3, rect.radiusTopRight());
        assertEquals(4, rect.radiusTopLeft());
    }

    @Test
    void sdfStrokeAndBorderColor() {
        RoundedRectTexture rect = cast(RoundedRectTexture.class, parse("sdf(#102030, 2, 1, #AABBCC)"));
        assertEquals(0xFF102030, rect.fillColor());
        assertEquals(1, rect.borderThickness());
        assertEquals(0xFFAABBCC, rect.borderColor());
    }

    @Test
    void sdfBorderInkDefaultsToBlack() {
        RoundedRectTexture rect = cast(RoundedRectTexture.class, parse("sdf(#102030, 2, 3)"));
        assertEquals(3, rect.borderThickness());
        assertEquals(0xFF000000, rect.borderColor());
    }

    @Test
    void rectIsTheSdfAlias() {
        assertEquals(cast(RoundedRectTexture.class, parse("sdf(#102030, 2)")), parse("rect(#102030, 2)"));
    }

    @Test
    void sdfRejectsInvalidShapes() {
        assertNull(parse("sdf(4)")); // no color
        assertNull(parse("sdf(#fff, 1 2 3)")); // neither one nor four radii
        assertNull(parse("sdf(#fff, 2, 1, #000, extra)")); // too many arguments
    }

    // ------------------------------------------------------------------ group / empty

    @Test
    void groupLayersInOrder() {
        CompositeTexture group = cast(CompositeTexture.class, parse("group(color(#FF0000), color(#00FF00))"));
        assertEquals(2, group.layerCount());
        assertEquals(new ColorTexture(0xFFFF0000), group.layer(0));
        assertEquals(new ColorTexture(0xFF00FF00), group.layer(1));
    }

    @Test
    void groupLayersCarryTheirOwnModifiers() {
        CompositeTexture group = cast(
            CompositeTexture.class,
            parse("group(color(#FF0000) color(#808080), sprite(\"cloudlib:gui/s\", 4, 4))")
        );
        TintedTexture tinted = cast(TintedTexture.class, group.layer(0));
        assertEquals(0xFF808080, tinted.tint());
        assertEquals(
            new ImageTexture(
                ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/s.png"),
                0,
                0,
                4,
                4,
                4,
                4,
                false
            ),
            group.layer(1)
        );
    }

    @Test
    void groupDropsLayersItCannotDraw() {
        CompositeTexture group = cast(CompositeTexture.class, parse("group(empty, color(#123456))"));
        assertEquals(1, group.layerCount());
        assertEquals(new ColorTexture(0xFF123456), group.layer(0));
    }

    @Test
    void groupWithoutDrawableLayersIsInvalid() {
        assertNull(parse("group(empty)"));
        assertNull(parse("group()"));
    }

    @Test
    void emptyAndNoneAreTheEmptySentinel() {
        assertSame(VisualTexture.empty, parse("none"));
        assertSame(VisualTexture.empty, parse("empty"));
        assertSame(VisualTexture.empty, parse("none()"));
        assertSame(VisualTexture.empty, parse("empty()"));
    }

    // ------------------------------------------------------------------ sprite

    @Test
    void spriteFiveArgsReadsARegion() {
        // the sheet's size stays unresolved (0) until the texture draws
        assertEquals(
            new ImageTexture(
                ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/sheet.png"),
                4,
                8,
                16,
                16,
                0,
                0,
                false
            ),
            parse("sprite(\"cloudlib:gui/sheet\", 4, 8, 16, 16)")
        );
    }

    @Test
    void spriteThreeArgsStillBlitsTheFile() {
        assertEquals(
            new ImageTexture(
                ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/x.png"),
                0,
                0,
                16,
                16,
                16,
                16,
                false
            ),
            parse("sprite(\"cloudlib:gui/x\", 16, 16)")
        );
    }

    @Test
    void spriteOneArgIsAnAtlasSprite() {
        assertTrue(parse("sprite(\"cloudlib:gui/y\")") instanceof SpriteTexture);
    }

    // ------------------------------------------------------------------ built-in

    @Test
    void builtInResolvesAPortedSprite() {
        // built-in() carries LDLib2's CLAMP default, i.e. NineSliceTexture.Wrap.stretch
        assertEquals(
            new NineSliceTexture(
                ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/oreui/ore_styles.png"),
                0,
                0,
                5,
                7,
                0,
                0,
                2,
                2,
                2,
                4,
                false,
                NineSliceTexture.Wrap.stretch
            ),
            parse("built-in(ore:BTN_DEFAULT)")
        );
    }

    @Test
    void builtInAcceptsQuotedAndLdlibIds() {
        assertNotNull(parse("built-in(\"mc:RECT\")"));
        assertSame(parse("built-in(ore:BTN_DEFAULT)"), parse("built-in(ui-ore:btn_default)"));
    }

    @Test
    void theHarvestPairIsNameableFromCss() {
        assertSame(BuiltInTextures.Icon.check, parse("built-in(icon:check)"));
        assertSame(BuiltInTextures.Icon.cross, parse("built-in(icon:CROSS)"));
        // the pair rides the same modifier chain as any other sprite, so a theme tints it
        TintedTexture tinted = cast(TintedTexture.class, parse("built-in(icon:cross) color(#E06666)"));
        assertSame(BuiltInTextures.Icon.cross, tinted.texture());
        assertEquals(0xFFE06666, tinted.tint());
    }

    @Test
    void unknownBuiltInIsInvalid() {
        assertNull(parse("built-in(ore:NOPE)"));
    }

    // ------------------------------------------------------------------ modifier chain

    @Test
    void scaleAndTranslateWrapTheTexture() {
        TintedTexture tinted = cast(
            TintedTexture.class,
            parse("sprite(\"cloudlib:gui/x\", 16, 16) scale(2) translate(3, 4) color(#FF0000)")
        );
        assertEquals(0xFFFF0000, tinted.tint());
        TransformedTexture transform = cast(TransformedTexture.class, tinted.texture());
        assertEquals(2f, transform.scaleX());
        assertEquals(2f, transform.scaleY());
        assertEquals(3f, transform.offsetX());
        assertEquals(4f, transform.offsetY());
    }

    @Test
    void scaleTakesTwoAxes() {
        TransformedTexture transform = cast(TransformedTexture.class, parse("color(#fff) scale(2, 3)"));
        assertEquals(2f, transform.scaleX());
        assertEquals(3f, transform.scaleY());
    }

    @Test
    void colorOnlySkipsTheTransform() {
        TintedTexture tinted = cast(TintedTexture.class, parse("color(#fff) color(#FF0000)"));
        assertTrue(tinted.texture() instanceof ColorTexture);
    }

    @Test
    void unknownModifiersAreIgnored() {
        assertTrue(parse("color(#fff) rotate(45)") instanceof ColorTexture);
    }

    @Test
    void malformedModifiersDropTheDeclaration() {
        assertNull(parse("color(#fff) scale(banana)"));
        assertNull(parse("color(#fff) scale(1, 2, 3)"));
        assertNull(parse("color(#fff) translate(1)"));
        assertNull(parse("color(#fff) color(banana)"));
        assertNull(parse("color(#fff) #FF0000")); // a stray token where a modifier belongs
    }

    // ------------------------------------------------------------------ nine-slice borders

    @Test
    void nineSliceBorderSequence() {
        // nine-slice(loc, <top> <right> <bottom> <left>)
        assertEquals(
            NineSliceTexture.of(
                ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/panel.png"),
                18,
                18,
                6,
                4,
                3,
                5
            ),
            parse("nine-slice(\"cloudlib:gui/panel\", 3 4 5 6)")
        );
    }

    @Test
    void nineSliceUniformBorderAndCellSize() {
        assertEquals(
            NineSliceTexture.of(ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/panel.png"), 16, 16, 3),
            parse("nine-slice(\"cloudlib:gui/panel\", 3, 16, 16)")
        );
    }

    // ------------------------------------------------------------------ modifier primitives

    /** Captures the quad a texture pushes into the batch — position, size and tint. */
    private static final class Emitter implements BatchableTexture.VertexEmitter {
        private final List<float[]> quads = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();

        @Override
        public void textured(
            ResourceLocation texture,
            float x,
            float y,
            float width,
            float height,
            float u0,
            float v0,
            float u1,
            float v1,
            int color
        ) {
            quads.add(new float[]{x, y, width, height});
            colors.add(color);
        }

        @Override
        public void colored(float x, float y, float width, float height, int color) {
            quads.add(new float[]{x, y, width, height});
            colors.add(color);
        }
    }

    @Test
    void transformScalesAboutTheCenter() {
        Emitter emitter = new Emitter();
        TransformedTexture texture = TransformedTexture.scaled(new ColorTexture(0xFFFFFFFF), 2f);
        texture.emit(emitter, 10f, 10f, 20f, 10f, -1);
        // the rect grows around its center, so both sides gain half the slack
        assertArrayEquals(new float[]{0f, 5f, 40f, 20f}, emitter.quads.get(0));
        assertTrue(texture.supportsBatching());
    }

    @Test
    void transformTranslatesAfterScaling() {
        Emitter emitter = new Emitter();
        VisualTexture texture = parse("color(#FFFFFFFF) scale(2) translate(3, 4)");
        cast(BatchableTexture.class, texture).emit(emitter, 10f, 10f, 20f, 10f, -1);
        assertArrayEquals(new float[]{3f, 9f, 40f, 20f}, emitter.quads.get(0));
    }

    @Test
    void tintComposesWithTheDrawTint() {
        Emitter emitter = new Emitter();
        TintedTexture texture = new TintedTexture(new ColorTexture(0xFF804020), 0xFF808080);
        texture.emit(emitter, 0f, 0f, 4f, 4f, -1);
        assertEquals(0xFF402010, emitter.colors.get(0));
        // the identity tint leaves a color alone
        assertEquals(0xFF804020, VisualTexture.multiply(0xFF804020, -1));
    }

    @Test
    void nestedGroupBatchesThroughTheEmitter() {
        Emitter emitter = new Emitter();
        BatchableTexture group = cast(
            BatchableTexture.class,
            parse("group(color(#FF0000), color(#00FF00)) color(#808080)")
        );
        assertTrue(group.supportsBatching());
        group.emit(emitter, 0f, 0f, 2f, 2f, -1);
        assertEquals(2, emitter.quads.size());
        assertEquals(0xFF800000, emitter.colors.get(0));
        assertEquals(0xFF008000, emitter.colors.get(1));
    }

    // ------------------------------------------------------------------ through the style engine

    @Test
    void backgroundConsumesTheWholeModifierChain() {
        // #RRGGBBAA — the css hex order, alpha last
        VisualTexture background = resolveBackground("built-in(ore:BTN_DEFAULT) scale(2) color(#FFFFFF80)");
        TintedTexture tinted = cast(TintedTexture.class, background);
        assertEquals(0x80FFFFFF, tinted.tint());
        assertTrue(tinted.texture() instanceof TransformedTexture);
    }

    @Test
    void backgroundKeepsPlainColorsAndNone() {
        assertTrue(resolveBackground("red") instanceof ColorTexture);
        assertSame(VisualTexture.empty, resolveBackground("none"));
        assertTrue(resolveBackground("built-in(mc:RECT)") instanceof NineSliceTexture);
    }
}
