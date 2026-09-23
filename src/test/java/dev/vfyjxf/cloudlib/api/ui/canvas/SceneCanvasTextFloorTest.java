package dev.vfyjxf.cloudlib.api.ui.canvas;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text primitives versus Minecraft's opaque-promotion guard: the font
 * renderer renders a color with alpha byte 0..3 as <em>fully opaque</em>
 * ({@code Font.adjustColor}), so a subtree fade's tail would flash text
 * bright for its last frames. The canvas skips text draws that fall into
 * that band instead — a ≤3/255-opacity glyph is indistinguishable from
 * nothing. This is the regression net for the in-game one-frame bright text
 * flash at the end of panel exit animations.
 * <p>
 * Headless: a dummy {@code Minecraft} (Unsafe-allocated, the
 * {@code OverlayTestMinecrafts} trick) satisfies the canvas's font reads;
 * the recording {@code GuiGraphics} captures the draw without a real font.
 */
class SceneCanvasTextFloorTest {

    private static final class RecordingGraphics extends GuiGraphics {
        final List<Integer> colors = new ArrayList<>();

        RecordingGraphics(Minecraft minecraft) {
            super(minecraft, null);
        }

        @Override
        public int drawString(Font font, String text, int x, int y, int color, boolean dropShadow) {
            colors.add(color);
            return 0;
        }

        @Override
        public int drawString(Font font, Component text, int x, int y, int color, boolean dropShadow) {
            colors.add(color);
            return 0;
        }

        @Override
        public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow) {
            colors.add(color);
            return 0;
        }
    }

    private static @Nullable Minecraft prior;

    @BeforeEach
    void setUp() throws Exception {
        prior = Minecraft.getInstance();
        setMinecraftInstance(dummy());
    }

    @AfterEach
    void tearDown() throws Exception {
        setMinecraftInstance(Objects.requireNonNull(prior));
    }

    private static Minecraft dummy() {
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

    @Test
    void textBelowTheAlphaFloorIsSkippedNotFlashedOpaque() {
        SceneCanvas canvas = SceneCanvas.create(new RecordingGraphics(dummy()));
        RecordingGraphics rec = recording(canvas);
        canvas.text("fading", 0, 0, 0xFFE8F0FF);
        assertEquals(1, rec.colors.size(), "untinted text draws");
        assertEquals(0xFFE8F0FF, (int) rec.colors.get(0));

        rec.colors.clear();
        canvas.color(0x02FFFFFF); // alpha byte 2 — inside the promotion band
        canvas.text("fading", 0, 0, 0xFFE8F0FF);
        assertEquals(0, rec.colors.size(), "a band-alpha text draw is skipped — the font would flash it opaque");

        canvas.color(0x00FFFFFF); // alpha byte 0 — the fade's floor
        canvas.text("fading", 0, 0, 0xFFE8F0FF);
        assertEquals(0, rec.colors.size(), "a fully faded text draw is skipped");

        rec.colors.clear();
        canvas.color(0x04FFFFFF); // alpha byte 4 — the first band-free value
        canvas.text("fading", 0, 0, 0xFFE8F0FF);
        assertEquals(1, rec.colors.size(), "alpha 4 still draws");
        assertTrue(((rec.colors.get(0) >>> 24) & 0xFF) <= 4, "the tinted color carries the faded alpha");
        canvas.color(0xFFFFFFFF);
    }

    @SuppressWarnings("DataFlowIssue")
    private static RecordingGraphics recording(SceneCanvas canvas) {
        try {
            Field field = SceneCanvas.class.getDeclaredField("graphics");
            field.setAccessible(true);
            return (RecordingGraphics) field.get(canvas);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
