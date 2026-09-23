package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ported LDLib2 sprite tables — the ids a theme may name through
 * {@code built-in(<ns:NAME>)} and the regions behind them.
 */
class BuiltInTexturesTest {

    private static final ResourceLocation ore = ResourceLocation
            .fromNamespaceAndPath("cloudlib", "textures/gui/oreui/ore_styles.png");
    private static final ResourceLocation mc = ResourceLocation
            .fromNamespaceAndPath("cloudlib", "textures/gui/oreui/mc_styles.png");
    private static final ResourceLocation gdp = ResourceLocation
            .fromNamespaceAndPath("cloudlib", "textures/gui/oreui/gdp_styles.png");

    private static NineSliceTexture sliced(String id) {
        return assertInstanceOf(NineSliceTexture.class, BuiltInTextures.get(id), id);
    }

    /** JUnit 5.7 lacks assertInstanceOf — same contract: fail on type mismatch, else cast. */
    @SuppressWarnings("unchecked")
    private static <T> T assertInstanceOf(Class<T> type, @Nullable Object value, String message) {
        assertNotNull(value, message);
        assertTrue(type.isInstance(value), message + ": expected " + type.getSimpleName());
        return (T) value;
    }

    @Test
    void sheetsPointAtThePortedAssets() {
        assertEquals(ore, BuiltInTextures.oreSheet);
        assertEquals(mc, BuiltInTextures.mcSheet);
        assertEquals(gdp, BuiltInTextures.gdpSheet);
    }

    @Test
    void everyUpstreamSpriteIsRegistered() {
        // OreSprites 48 + MCSprites 22 + Sprites 82 + the generated check/cross pair
        assertEquals(154, BuiltInTextures.ids().size());
        assertEquals(48, BuiltInTextures.ids().stream().filter(id -> id.startsWith("ore:")).count());
        assertEquals(22, BuiltInTextures.ids().stream().filter(id -> id.startsWith("mc:")).count());
        assertEquals(82, BuiltInTextures.ids().stream().filter(id -> id.startsWith("gdp:")).count());
        assertEquals(2, BuiltInTextures.ids().stream().filter(id -> id.startsWith("icon:")).count());
    }

    @Test
    void theHarvestPairIsRegisteredOverItsTwoFiles() throws IOException {
        ImageTexture check = assertInstanceOf(ImageTexture.class, BuiltInTextures.get("icon:CHECK"), "icon:CHECK");
        ImageTexture cross = assertInstanceOf(ImageTexture.class, BuiltInTextures.get("icon:CROSS"), "icon:CROSS");
        // "can" reuses the ore sheet sprite verbatim — one definition, not a copy
        assertSame(BuiltInTextures.get("ore:CHECK"), check);
        assertEquals(ore, check.location());
        // "cannot" lives on the generated standalone file, at the same 10x10 size
        assertEquals(ResourceLocation.fromNamespaceAndPath("cloudlib", "textures/gui/cross.png"), cross.location());
        assertEquals(10, cross.width());
        assertEquals(10, cross.height());
        assertEquals(BuiltInTextures.crossImage, cross.location());
    }

    @Test
    void theGeneratedCrossFileIsTheTenPixelSquareTheTableDeclares() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/assets/cloudlib/textures/gui/cross.png")) {
            assertNotNull(in, "cross.png must ship in the mod resources");
            byte[] header = in.readNBytes(24);
            assertEquals(24, header.length, "a png header is at least 24 bytes");
            byte[] signature = new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
            assertArrayEquals(signature, Arrays.copyOf(header, signature.length));
            assertEquals("IHDR", new String(header, 12, 4, StandardCharsets.US_ASCII));
            // an uncompressed PNG IHDR: width at offset 16, height at 20, big-endian
            assertEquals(10, bigEndianInt(header, 16), "cross.png width");
            assertEquals(10, bigEndianInt(header, 20), "cross.png height");
        }
    }

    private static int bigEndianInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    @Test
    void idsKeepTheUpstreamNames() {
        assertTrue(
            BuiltInTextures.ids().containsAll(
                List.of("ore:BTN_DEFAULT", "ore:BORDER_5", "mc:SCROLLER_V", "mc:RECT_1", "gdp:RECT_RD", "gdp:TAB_WHITE")
            )
        );
    }

    @Test
    void regionsAndBordersMatchUpstream() {
        NineSliceTexture button = sliced("ore:BTN_DEFAULT");
        assertEquals(ore, button.location());
        assertEquals(0, button.u());
        assertEquals(0, button.v());
        assertEquals(5, button.width());
        assertEquals(7, button.height());
        // LDLib2 setBorder(left, top, right, bottom)
        assertEquals(2, button.left());
        assertEquals(2, button.top());
        assertEquals(2, button.right());
        assertEquals(4, button.bottom());

        NineSliceTexture border5 = sliced("ore:BORDER_5");
        assertEquals(0, border5.u());
        assertEquals(199, border5.v());
        assertEquals(50, border5.width());
        assertEquals(52, border5.height());
        assertEquals(3, border5.left());
        assertEquals(5, border5.bottom());

        NineSliceTexture scroller = sliced("mc:SCROLLER_V");
        assertEquals(mc, scroller.location());
        assertEquals(0, scroller.u());
        assertEquals(48, scroller.v());
        assertEquals(2, scroller.left());

        NineSliceTexture progress = sliced("gdp:PROGRESS_BAR");
        assertEquals(241, progress.u());
        assertEquals(164, progress.v());
        assertEquals(10, progress.width());
        assertEquals(3, progress.height());

        // upstream's borderless sprites are plain regions, not nine-slices
        ImageTexture check = assertInstanceOf(ImageTexture.class, BuiltInTextures.get("ore:CHECK"), "ore:CHECK");
        assertEquals(50, check.u());
        assertEquals(35, check.v());
        assertEquals(10, check.width());
        assertEquals(10, check.height());
    }

    @Test
    void everySpriteIsADrawableRegion() {
        for (var entry : BuiltInTextures.all().entrySet()) {
            VisualTexture texture = entry.getValue();
            assertTrue(
                texture instanceof NineSliceTexture || texture instanceof ImageTexture,
                entry.getKey() + " is " + texture.getClass().getSimpleName()
            );
            assertNotSame(VisualTexture.empty, texture, entry.getKey());
            if (texture instanceof NineSliceTexture nine) {
                assertTrue(nine.width() > 0 && nine.height() > 0, entry.getKey());
                assertTrue(nine.left() + nine.right() <= nine.width(), entry.getKey() + " borders");
                assertTrue(nine.top() + nine.bottom() <= nine.height(), entry.getKey() + " borders");
            } else if (texture instanceof ImageTexture image) {
                assertTrue(image.width() > 0 && image.height() > 0, entry.getKey());
            }
        }
    }

    @Test
    void spritesCarryTheWrapModeLdlibDeclared() {
        // LDLib2's SpriteTexture defaults to CLAMP — the stretched edges and center this
        // texture draws as Wrap.stretch. Only MCSprites' four SCROLLER_* sprites opt into
        // REPEAT, and they are the only ones that keep tiling.
        assertEquals(
            List.of("mc:SCROLLER_H", "mc:SCROLLER_H_DARK", "mc:SCROLLER_V", "mc:SCROLLER_V_DARK"),
            BuiltInTextures.all().entrySet().stream()
                    .filter(
                        entry -> entry.getValue() instanceof NineSliceTexture nine
                                && nine.wrap() == NineSliceTexture.Wrap.repeat
                    ).map(Map.Entry::getKey).toList()
        );
        assertEquals(NineSliceTexture.Wrap.stretch, sliced("ore:BTN_DEFAULT").wrap());
    }

    @Test
    void lookupIsCaseInsensitiveAndKnowsLdlibNamespaces() {
        assertSame(BuiltInTextures.get("ore:BTN_DEFAULT"), BuiltInTextures.get("ore:btn_default"));
        assertSame(BuiltInTextures.get("mc:RECT"), BuiltInTextures.get("ui-mc:RECT"));
        assertSame(BuiltInTextures.get("gdp:TAB"), BuiltInTextures.get("UI-GDP:tab"));
        assertNull(BuiltInTextures.get(null));
        assertNull(BuiltInTextures.get("ore:NOPE"));
    }

    @Test
    void theTableIsReadOnly() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> BuiltInTextures.all().put("ore:NOPE", VisualTexture.empty)
        );
    }
}
