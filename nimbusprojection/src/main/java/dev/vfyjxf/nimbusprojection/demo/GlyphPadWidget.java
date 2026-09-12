package dev.vfyjxf.nimbusprojection.demo;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.taffy.geometry.FloatSize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The differentiated trace application — a freehand sigil pad instead of a
 * grid maze. Hold the interact key and sketch a rune anywhere on the pad; on
 * release the stroke is resampled and matched against the known glyphs and
 * the recognized rune id is handed to {@code onGlyph} (the provider wires it
 * to a reversed channel — same ink, different server action).
 * <p>
 * Unlike the Witness puzzle this has no nodes or legal-path rules: the trace
 * is raw freehand ink, which makes it a writing/gesture input rather than a
 * maze solve.
 * <p>
 * Glyphs: {@code ◯ pulse} (0), {@code Z surge} (1), {@code ✓ mark} (2),
 * {@code ─ channel} (3).
 */
public final class GlyphPadWidget extends Widget implements InworldTraceable {

    public static final int GLYPH_PULSE = 0;
    public static final int GLYPH_SURGE = 1;
    public static final int GLYPH_MARK = 2;
    public static final int GLYPH_CHANNEL = 3;

    private static final int W = 92, H = 58;
    private static final int PAD_H = 44;
    private static final float MAX_DIST = 0.16f;

    private static final List<GlyphClassifier.Template> TEMPLATES = List.of(
            GlyphClassifier.Template.cyclic(GLYPH_PULSE, "pulse", circle(14)),
            GlyphClassifier.Template.of(GLYPH_SURGE, "surge", new float[][]{
                    {0, 0}, {1, 0}, {0, 1}, {1, 1}}),
            GlyphClassifier.Template.of(GLYPH_MARK, "mark", new float[][]{
                    {0, 0.55f}, {0.4f, 1}, {1, 0}}),
            GlyphClassifier.Template.of(GLYPH_CHANNEL, "channel", new float[][]{
                    {0, 0.5f}, {1, 0.5f}})
    );

    private static float[][] circle(int n) {
        float[][] pts = new float[n][2];
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            pts[i] = new float[]{0.5f + 0.5f * (float) Math.cos(a), 0.5f + 0.5f * (float) Math.sin(a)};
        }
        return pts;
    }

    private final IntConsumer onGlyph;

    private final List<float[]> stroke = new ArrayList<>();
    private float cursorX, cursorY;
    private boolean tracing;
    private int inkFade;          //committed ink lingers a moment, then fades
    private int resultFlash;
    private String last = "--";
    private boolean lastOk;

    public GlyphPadWidget(InworldPanelContext ctx, IntConsumer onGlyph) {
        this.onGlyph = onGlyph;
        setTickable(true);
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(W, H)));
    }

    //region InworldTraceable

    @Override
    public FloatPos traceCursorStart() {
        //freehand pad still gets a canonical start: the pad center — every
        //stroke begins from the middle of the drawing area, deterministic
        //and centered, instead of wherever the activation aim happened to land
        return new FloatPos(W * 0.5, PAD_H * 0.5);
    }

    @Override
    public boolean traceBegin(InworldPanelContext context, float x, float y) {
        tracing = true;
        stroke.clear();
        inkFade = 0;
        stroke.add(new float[]{x, y});
        cursorX = x;
        cursorY = y;
        return true;
    }

    @Override
    public void traceMove(float x, float y) {
        cursorX = x;
        cursorY = y;
        float[] last = stroke.get(stroke.size() - 1);
        float dx = x - last[0], dy = y - last[1];
        if (dx * dx + dy * dy > 1.4f) {
            stroke.add(new float[]{x, y});
        }
    }

    @Override
    public void traceCommit(InworldPanelContext context) {
        tracing = false;
        inkFade = 24;
        int id = GlyphClassifier.classify(stroke, TEMPLATES, MAX_DIST);
        if (id >= 0) {
            last = TEMPLATES.get(id).name();
            lastOk = true;
            onGlyph.accept(id);
        } else {
            last = "?";
            lastOk = false;
        }
        resultFlash = 30;
    }

    @Override
    public void traceCancel() {
        tracing = false;
        stroke.clear();
    }

    //endregion

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        //the pad: recessed ink surface
        canvas.fill(0, 0, W, PAD_H, 0xA0050A10);
        canvas.strokeRect(0, 0, W, PAD_H, HackerTheme.LINE_DARK);
        //center guides — faint crosshair so freehand strokes have a reference
        canvas.line(W / 2f, 4, W / 2f, PAD_H - 4, 1f, 0x2216D8CF);
        canvas.line(4, PAD_H / 2f, W - 4, PAD_H / 2f, 1f, 0x2216D8CF);

        //ink trail
        if (!stroke.isEmpty()) {
            float alpha = inkFade > 0 && !tracing ? 0.35f + 0.65f * (inkFade / 24f) : 1f;
            int ink = withAlpha(lastOk || tracing ? HackerTheme.ACCENT : 0xFFE06666, alpha);
            for (int i = 1; i < stroke.size(); i++) {
                float[] a = stroke.get(i - 1), b = stroke.get(i);
                canvas.line(a[0], a[1], b[0], b[1], 1.6f, ink);
            }
            if (tracing) {
                canvas.circle(cursorX, cursorY, 3.5f, 0x5516D8CF);
                canvas.circle(cursorX, cursorY, 1.6f, HackerTheme.ACCENT);
            }
        }

        //status row: available glyphs + the last recognized rune
        canvas.text("◯ ⩍ ✓ ─", 0, PAD_H + 4, HackerTheme.TEXT_DIM);
        int color = resultFlash > 0
                ? (lastOk ? HackerTheme.ACCENT : 0xFFE06666)
                : HackerTheme.TEXT_DIM;
        String label = "≫ " + last;
        var font = context().font();
        canvas.text(label, W - font.width(label), PAD_H + 4, color);
    }

    private static int withAlpha(int color, float frac) {
        int a = (int) (((color >>> 24) & 0xFF) * frac);
        return (a << 24) | (color & 0xFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();
        if (inkFade > 0) {
            inkFade--;
            if (inkFade == 0 && !tracing) stroke.clear();
        }
        if (resultFlash > 0) resultFlash--;
    }
}
