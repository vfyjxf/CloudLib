package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two wrap modes at the quad level — {@link NineSliceTexture.Wrap#stretch} fills
 * each of the nine regions with one quad, {@link NineSliceTexture.Wrap#repeat} tiles
 * the edges and the center with source-sized quads. Both are checked on the primitives
 * the texture emits into a batch, geometry and UV alike.
 */
class NineSliceTextureTest {

    private static final float epsilon = 1e-4f;

    /** A 16x16 sprite with a 3px border — the middle region is 10x10. */
    private static final ResourceLocation sheet = ResourceLocation
            .fromNamespaceAndPath("cloudlib", "textures/gui/oreui/mc_styles.png");

    private static NineSliceTexture sprite(NineSliceTexture.Wrap wrap) {
        return NineSliceTexture.of(sheet, 16, 16, 3).withWrap(wrap);
    }

    /** Emits into {@code 100x50} at the origin and returns the quads, in emit order. */
    private static List<Quad> quadsOf(NineSliceTexture texture) {
        Recorder recorder = new Recorder();
        texture.emit(recorder, 0, 0, 100, 50, 0xFFFFFFFF);
        return recorder.quads;
    }

    // ------------------------------------------------------------------ stretch

    @Test
    void stretchDrawsNineQuads() {
        assertEquals(9, quadsOf(sprite(NineSliceTexture.Wrap.stretch)).size());
    }

    @Test
    void stretchKeepsTheCornersAtTheirSourceSize() {
        List<Quad> quads = quadsOf(sprite(NineSliceTexture.Wrap.stretch));

        // TL, TR, BL, BR — fixed 3x3 corners, pushed first
        assertQuad(quads.get(0), 0, 0, 3, 3, 0f, 0f, 0.1875f, 0.1875f);
        assertQuad(quads.get(1), 97, 0, 3, 3, 0.8125f, 0f, 1f, 0.1875f);
        assertQuad(quads.get(2), 0, 47, 3, 3, 0f, 0.8125f, 0.1875f, 1f);
        assertQuad(quads.get(3), 97, 47, 3, 3, 0.8125f, 0.8125f, 1f, 1f);
    }

    @Test
    void stretchSpansEachEdgeWithOneQuad() {
        List<Quad> quads = quadsOf(sprite(NineSliceTexture.Wrap.stretch));

        // top, bottom, left, right — one quad each, the source edge UV stretched across
        assertQuad(quads.get(4), 3, 0, 94, 3, 0.1875f, 0f, 0.8125f, 0.1875f);
        assertQuad(quads.get(5), 3, 47, 94, 3, 0.1875f, 0.8125f, 0.8125f, 1f);
        assertQuad(quads.get(6), 0, 3, 3, 44, 0f, 0.1875f, 0.1875f, 0.8125f);
        assertQuad(quads.get(7), 97, 3, 3, 44, 0.8125f, 0.1875f, 1f, 0.8125f);
    }

    @Test
    void stretchStretchesTheCenterQuad() {
        List<Quad> quads = quadsOf(sprite(NineSliceTexture.Wrap.stretch));

        assertQuad(quads.get(8), 3, 3, 94, 44, 0.1875f, 0.1875f, 0.8125f, 0.8125f);
    }

    @Test
    void stretchTilesTheTargetExactlyOnce() {
        float area = 0;
        for (Quad quad : quadsOf(sprite(NineSliceTexture.Wrap.stretch))) {
            area += quad.w() * quad.h();
        }
        assertEquals(100 * 50, area, epsilon);
    }

    @Test
    void stretchStillSpansWhenTheTargetIsNarrowerThanTheSource() {
        Recorder recorder = new Recorder();
        sprite(NineSliceTexture.Wrap.stretch).emit(recorder, 0, 0, 10, 10, 0xFFFFFFFF);

        assertEquals(9, recorder.quads.size());
        // the middle spans shrink to 4px; the edges follow the same source UV
        assertQuad(recorder.quads.get(4), 3, 0, 4, 3, 0.1875f, 0f, 0.8125f, 0.1875f);
        assertQuad(recorder.quads.get(8), 3, 3, 4, 4, 0.1875f, 0.1875f, 0.8125f, 0.8125f);
    }

    // ------------------------------------------------------------------ repeat

    @Test
    void repeatTilesTheSpansWithSourceSizedQuads() {
        List<Quad> quads = quadsOf(sprite(NineSliceTexture.Wrap.repeat));

        // 4 corners + 10 top + 10 bottom + 5 left + 5 right + 10x5 center
        assertEquals(84, quads.size());
        // the corners are the same quads stretch draws
        assertQuad(quads.get(0), 0, 0, 3, 3, 0f, 0f, 0.1875f, 0.1875f);
        assertQuad(quads.get(3), 97, 47, 3, 3, 0.8125f, 0.8125f, 1f, 1f);
    }

    @Test
    void repeatRunsTheTopEdgeAsTenTilesWithAClippedLastOne() {
        List<Quad> quads = quadsOf(sprite(NineSliceTexture.Wrap.repeat));

        // the top edge's tiles follow the four corners
        assertQuad(quads.get(4), 3, 0, 10, 3, 0.1875f, 0f, 0.8125f, 0.1875f);
        assertQuad(quads.get(5), 13, 0, 10, 3, 0.1875f, 0f, 0.8125f, 0.1875f);
        // the tenth spans the 4px left before the right corner, so its UV is clipped
        assertQuad(quads.get(13), 93, 0, 4, 3, 0.1875f, 0f, 0.4375f, 0.1875f);
    }

    @Test
    void repeatNeverOverflowsTheSourceTile() {
        for (Quad quad : quadsOf(sprite(NineSliceTexture.Wrap.repeat))) {
            // every quad is at most one source region, in pixels and in UV
            assertTrue(quad.w() <= 10, "width " + quad.w());
            assertTrue(quad.h() <= 10, "height " + quad.h());
            assertTrue(quad.u1() - quad.u0() <= 0.625f + epsilon, "u " + (quad.u1() - quad.u0()));
            assertTrue(quad.v1() - quad.v0() <= 0.625f + epsilon, "v " + (quad.v1() - quad.v0()));
        }
    }

    @Test
    void repeatCoversTheWholeTarget() {
        float area = 0;
        for (Quad quad : quadsOf(sprite(NineSliceTexture.Wrap.repeat))) {
            area += quad.w() * quad.h();
        }
        assertEquals(100 * 50, area, epsilon);
    }

    // ------------------------------------------------------------------ mode and defaults

    @Test
    void theWrapLessFactoriesKeepTiling() {
        assertEquals(NineSliceTexture.Wrap.repeat, NineSliceTexture.of(sheet, 16, 16, 3).wrap());
        assertEquals(NineSliceTexture.Wrap.repeat, NineSliceTexture.of(sheet, 16, 16, 3, 3, 3, 3).wrap());
        assertEquals(NineSliceTexture.Wrap.repeat, NineSliceTexture.region(sheet, 0, 0, 16, 16, 3, 3, 3, 3).wrap());
        assertEquals(NineSliceTexture.Wrap.repeat, NineSliceTexture.sprite(sheet, 16, 16, 3).wrap());
    }

    @Test
    void withWrapKeepsTheRegionAndTheBorders() {
        NineSliceTexture repeated = sprite(NineSliceTexture.Wrap.repeat);
        NineSliceTexture stretched = repeated.withWrap(NineSliceTexture.Wrap.stretch);

        assertEquals(repeated.withWrap(NineSliceTexture.Wrap.stretch), stretched);
        assertSame(repeated, repeated.withWrap(NineSliceTexture.Wrap.repeat));
        assertEquals(repeated.location(), stretched.location());
        assertEquals(repeated.u(), stretched.u());
        assertEquals(repeated.v(), stretched.v());
        assertEquals(repeated.width(), stretched.width());
        assertEquals(repeated.height(), stretched.height());
        assertEquals(repeated.left(), stretched.left());
        assertEquals(repeated.right(), stretched.right());
        assertEquals(repeated.top(), stretched.top());
        assertEquals(repeated.bottom(), stretched.bottom());
        assertNotEquals(repeated, stretched);
    }

    @Test
    void theAtlasConversionsCarryTheWrapMode() {
        NineSliceTexture stretched = sprite(NineSliceTexture.Wrap.stretch);

        assertEquals(NineSliceTexture.Wrap.stretch, stretched.asAtlasSprite().wrap());
        assertEquals(NineSliceTexture.Wrap.stretch, stretched.asAtlasSprite().asStandardTexture().wrap());
    }

    @Test
    void thePortedSpritesStretchAndTheScrollersRepeat() {
        NineSliceTexture button = cast(NineSliceTexture.class, BuiltInTextures.get("ore:BTN_DEFAULT"));
        NineSliceTexture scroller = cast(NineSliceTexture.class, BuiltInTextures.get("mc:SCROLLER_V"));

        assertEquals(NineSliceTexture.Wrap.stretch, button.wrap());
        assertEquals(NineSliceTexture.Wrap.repeat, scroller.wrap());
        // a stretched sprite is 9 quads whatever its span, a repeating one grows with it
        assertEquals(9, quadsOf(button).size());
        Recorder tall = new Recorder();
        button.emit(tall, 0, 0, 16, 200, 0xFFFFFFFF);
        assertEquals(9, tall.quads.size());

        Recorder tiled = new Recorder();
        scroller.emit(tiled, 0, 0, 16, 200, 0xFFFFFFFF);
        assertTrue(tiled.quads.size() > 9, "scroller tiles: " + tiled.quads.size());
    }

    // ------------------------------------------------------------------ helpers

    /** One emitted quad — its rect on screen and its rect in the sheet. */
    private record Quad(float x, float y, float w, float h, float u0, float v0, float u1, float v1) {}

    private static void assertQuad(
        Quad quad,
        float x,
        float y,
        float w,
        float h,
        float u0,
        float v0,
        float u1,
        float v1
    ) {
        assertEquals(x, quad.x(), epsilon);
        assertEquals(y, quad.y(), epsilon);
        assertEquals(w, quad.w(), epsilon);
        assertEquals(h, quad.h(), epsilon);
        assertEquals(u0, quad.u0(), epsilon);
        assertEquals(v0, quad.v0(), epsilon);
        assertEquals(u1, quad.u1(), epsilon);
        assertEquals(v1, quad.v1(), epsilon);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Class<T> type, @Nullable Object value) {
        assertNotNull(value, "expected " + type.getSimpleName() + ", got null");
        assertTrue(
            type.isInstance(value),
            "expected " + type.getSimpleName() + " but got " + value.getClass().getSimpleName()
        );
        return (T) value;
    }

    private static final class Recorder implements BatchableTexture.VertexEmitter {
        private final List<Quad> quads = new ArrayList<>();

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
            quads.add(new Quad(x, y, width, height, u0, v0, u1, v1));
        }

        @Override
        public void colored(float x, float y, float width, float height, int color) {
            fail("a nine-slice emits textured quads only");
        }
    }
}
