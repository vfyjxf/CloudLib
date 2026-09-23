package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The forwarded channel's fade contract, headless: the tint a segment hands the shader color, the
 * blend the drain takes over, the set/reset pairing around a segment, and the empty segment that
 * must cost no GL state. The drain's own draws need a live context, so what is pinned here is
 * everything a render pass decides <em>before</em> it reaches GL — the arithmetic the fade rides
 * and the state transitions around it.
 */
class ForwardedChannelFadeTest {

    private static Minecraft priorMinecraft;

    /** Records nothing and never touches the buffer source — headless there is no font and no GL. */
    private static final class RecordingGraphics extends GuiGraphics {

        RecordingGraphics(Minecraft minecraft) {
            super(minecraft, null);
        }

        @Override
        public int drawString(Font font, String text, int x, int y, int color, boolean dropShadow) {
            return 0;
        }

        @Override
        public int drawString(Font font, Component text, int x, int y, int color, boolean dropShadow) {
            return 0;
        }

        @Override
        public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow) {
            return 0;
        }
    }

    private static Minecraft dummyMinecraft() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            return (Minecraft) unsafe.allocateInstance(Minecraft.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setMinecraftInstance(Minecraft value) throws Exception {
        for (Field field : Minecraft.class.getDeclaredFields()) {
            if (field.getType() == Minecraft.class && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                field.set(null, value);
                return;
            }
        }
        throw new IllegalStateException("no static Minecraft field found");
    }

    /**
     * How many render calls the render system has parked for the render thread. Off that thread
     * every {@code setShaderColor} is recorded rather than applied, which is what makes the tint's
     * set/reset pairing observable without a context.
     */
    private static int queuedRenderCalls() throws Exception {
        Field field = RenderSystem.class.getDeclaredField("recordingQueue");
        field.setAccessible(true);
        return ((Collection<?>) field.get(null)).size();
    }

    @BeforeEach
    void setUp() throws Exception {
        priorMinecraft = Minecraft.getInstance();
        setMinecraftInstance(dummyMinecraft());
    }

    @Test
    void forwardedTintKeepsItsRgbAndRidesItsAlpha() {
        assertArrayEquals(
            new float[]{1f, 1f, 1f, 0x40 / 255f},
            SceneCanvas.forwardedShaderColor(0x40FFFFFF),
            1e-6f,
            "a translucent white tint is white with that alpha"
        );

        // a coloured tint stays its own colour: the rgb is the tint's, never the tint scaled by its
        // own alpha — that scaling is the difference between a fade and a darkening
        float[] coloured = SceneCanvas.forwardedShaderColor(0x40010203);
        assertArrayEquals(
            new float[]{0x01 / 255f, 0x02 / 255f, 0x03 / 255f, 0x40 / 255f},
            coloured,
            1e-6f,
            "the rgb is not premultiplied by the tint's alpha"
        );
        assertEquals(1f, SceneCanvas.forwardedShaderColor(0xFFFFFFFF)[3], 1e-6f, "opaque is opaque");
        assertEquals(0f, SceneCanvas.forwardedShaderColor(0x00FFFFFF)[3], 1e-6f, "a spent tint carries no alpha");
    }

    @Test
    void onlyATranslucentTintOwnsTheBlendState() {
        assertFalse(ForwardedBufferSource.fadeOwnsBlend(1f), "an opaque segment drains like vanilla");
        assertTrue(ForwardedBufferSource.fadeOwnsBlend(0.999f), "any fade forces the blend");
        assertTrue(ForwardedBufferSource.fadeOwnsBlend(0.5f));
        assertTrue(ForwardedBufferSource.fadeOwnsBlend(0f), "a spent tint still blends — nothing is what it draws");
    }

    @Test
    void openingAndClosingASegmentSetsAndResetsTheTint() throws Exception {
        SceneCanvas canvas = SceneCanvas.create(new RecordingGraphics(Minecraft.getInstance()));
        int before = queuedRenderCalls();

        canvas.color(0x40FFFFFF);
        canvas.graphics();
        assertEquals(before + 1, queuedRenderCalls(), "opening a forwarded segment hands it the subtree tint");

        canvas.resetColor();
        canvas.flushBatch();
        assertEquals(before + 2, queuedRenderCalls(), "closing it resets the shader color — the tint never leaks out");

        // a second close is not a second reset: the pairing is one set, one reset
        canvas.flushBatch();
        assertEquals(before + 2, queuedRenderCalls(), "an inactive segment stays closed");
    }

    @Test
    void anEmptyForwardedSegmentTouchesNoGlState() throws Exception {
        SceneCanvas canvas = SceneCanvas.create(new RecordingGraphics(Minecraft.getInstance()));
        int before = queuedRenderCalls();

        canvas.color(0x40FFFFFF);
        canvas.graphics();
        canvas.pushTransform();
        canvas.popTransform();
        canvas.resetColor();
        canvas.flushBatch();

        // reaching GL off the render thread throws rather than recording, so these calls completing
        // at all is the assertion: a segment that captured nothing drains as a no-op instead of
        // toggling the depth test for no pixels. The tint still opens and closes around it.
        assertEquals(before + 2, queuedRenderCalls(), "an empty segment still owns its tint's set and reset");
    }
}
