package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;

/**
 * Animated demo content for the expand panel: a radar-style square scope with
 * a rotating sweep, pulsing blips and a jittering signal bar — everything is
 * driven by gameTime+partialTick so it exercises per-frame animation inside
 * an in-world panel.
 */
public final class RadarWidget extends Widget {

    private static final int W = 150;
    private static final int H = 96;

    private static final int[] BLIP_ANGLE = {18, 96, 165, 238, 301};
    private static final int[] BLIP_RADIUS = {9, 15, 21, 12, 18};

    public RadarWidget() {
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(W, H)));
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        int w = width();
        int h = height();
        float now = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() + partialTicks
                : 0;

        int cx = w / 2;
        int cy = h / 2 - 6;
        int r = Math.min(w, h) / 2 - 10;

        //concentric square rings + crosshair
        for (int i = 1; i <= 3; i++) {
            int rr = r * i / 3;
            canvas.strokeRect(cx - rr, cy - rr, rr * 2, rr * 2, InworldTheme.BORDER);
        }
        canvas.fill(cx - r, cy, r * 2, 1, 0x2200FFE0);
        canvas.fill(cx, cy - r, 1, r * 2, 0x2200FFE0);

        //rotating sweep arm + fading tail
        double sweep = now * 0.11;
        for (int i = 0; i < 3; i++) {
            double a = sweep - i * 0.16;
            float alpha = 1f - i * 0.33f;
            int color = ((int) (alpha * 200) << 24) | (InworldTheme.ACCENT & 0x00FFFFFF);
            canvas.line(cx, cy,
                    (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r),
                    1f, color);
        }

        //pulsing blips on the rings
        for (int i = 0; i < BLIP_ANGLE.length; i++) {
            double a = Math.toRadians(BLIP_ANGLE[i]);
            int bx = (int) (cx + Math.cos(a) * BLIP_RADIUS[i]);
            int by = (int) (cy + Math.sin(a) * BLIP_RADIUS[i]);
            float pulse = (float) (Math.sin(now * 0.22 + i * 1.7) * 0.5 + 0.5);
            int size = pulse > 0.66f ? 3 : 2;
            int color = ((int) (0x55 + pulse * 0xAA) << 24) | (InworldTheme.ACCENT & 0x00FFFFFF);
            canvas.fill(bx - size / 2, by - size / 2, size, size, color);
        }

        //signal bars along the bottom
        int bars = 14;
        int bw = 4;
        int bx0 = cx - bars * (bw + 2) / 2;
        for (int i = 0; i < bars; i++) {
            float amp = (float) (Math.sin(now * 0.3 + i * 0.9) * 0.5 + 0.5);
            int bh = 2 + (int) (amp * 7);
            canvas.fill(bx0 + i * (bw + 2), h - 6 - bh, bw, bh,
                    amp > 0.6f ? InworldTheme.ACCENT : InworldTheme.ACCENT_DIM);
        }
        canvas.text("SIG " + (int) ((Math.sin(now * 0.2) * 0.5 + 0.5) * 99) + "%",
                2, h - 8, InworldTheme.TEXT_DIM);
    }
}
