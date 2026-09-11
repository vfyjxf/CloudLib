package dev.vfyjxf.inworldui.demo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math coverage of the sigil-pad recognizer: resampling, normalization
 * and template matching — no Minecraft classes involved.
 */
class GlyphClassifierTest {

    private static final float TOLERANCE = 0.16f;

    private static final List<GlyphClassifier.Template> TEMPLATES = List.of(
            GlyphClassifier.Template.cyclic(0, "pulse", circle(14)),
            GlyphClassifier.Template.of(1, "surge", new float[][]{
                    {0, 0}, {1, 0}, {0, 1}, {1, 1}}),
            GlyphClassifier.Template.of(2, "mark", new float[][]{
                    {0, 0.55f}, {0.4f, 1}, {1, 0}}),
            GlyphClassifier.Template.of(3, "channel", new float[][]{
                    {0, 0.5f}, {1, 0.5f}})
    );

    private static float[][] circle(int n) {
        float[][] pts = new float[n][2];
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / n;
            pts[i] = new float[]{(float) (0.5 + 0.5 * Math.cos(a)), (float) (0.5 + 0.5 * Math.sin(a))};
        }
        return pts;
    }

    /** Turns a template's point list into a dense polyline stroke (like real ink). */
    private static List<float[]> strokeOf(float[][] corners) {
        List<float[]> stroke = new ArrayList<>();
        for (int i = 0; i < corners.length; i++) {
            float[] a = corners[i];
            float[] b = corners[(i + 1) % corners.length];
            if (i == corners.length - 1) break;         //open polyline — corners only
            for (int s = 0; s < 8; s++) {
                float t = s / 8f;
                stroke.add(new float[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1])});
            }
        }
        stroke.add(corners[corners.length - 1].clone());
        return stroke;
    }

    private static List<float[]> closedStrokeOf(float[][] corners) {
        List<float[]> stroke = new ArrayList<>();
        int n = corners.length;
        for (int i = 0; i < n; i++) {
            float[] a = corners[i];
            float[] b = corners[(i + 1) % n];
            for (int s = 0; s < 4; s++) {
                float t = s / 4f;
                stroke.add(new float[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1])});
            }
        }
        return stroke;
    }

    @Test
    void resampleProducesEquidistantPoints() {
        //a straight diagonal — consecutive samples must be evenly spaced
        List<float[]> stroke = new ArrayList<>();
        for (int i = 0; i <= 20; i++) stroke.add(new float[]{i, i * 0.5f});

        float[][] pts = GlyphClassifier.resample(stroke, 8);
        assertEquals(8, pts.length);
        double total = Math.sqrt(20 * 20 + 10 * 10);
        for (int i = 1; i < pts.length; i++) {
            float dx = pts[i][0] - pts[i - 1][0];
            float dy = pts[i][1] - pts[i - 1][1];
            assertEquals(total / 7, Math.sqrt(dx * dx + dy * dy), 0.05,
                    "points should be equidistant along the stroke");
        }
        //and must cover the whole stroke
        assertEquals(0, pts[0][0], 0.05);
        assertEquals(20, pts[7][0], 0.05);
    }

    @Test
    void recognizesEachTemplate() {
        assertEquals(0, GlyphClassifier.classify(
                List.of(circle(40)), TEMPLATES, TOLERANCE), "circle");
        assertEquals(1, GlyphClassifier.classify(
                strokeOf(new float[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}}), TEMPLATES, TOLERANCE), "zee");
        assertEquals(2, GlyphClassifier.classify(
                strokeOf(new float[][]{{0, 0.55f}, {0.4f, 1}, {1, 0}}), TEMPLATES, TOLERANCE), "vee");
        assertEquals(3, GlyphClassifier.classify(
                strokeOf(new float[][]{{0, 0.5f}, {1, 0.5f}}), TEMPLATES, TOLERANCE), "dash");
    }

    @Test
    void circleMatchesFromAnyStartPoint() {
        //same circle, pen went down at a different point — cyclic matching must still hit
        float[][] shifted = new float[40][2];
        float[][] base = circle(40);
        for (int i = 0; i < 40; i++) shifted[i] = base[(i + 11) % 40];
        assertEquals(0, GlyphClassifier.classify(List.of(shifted), TEMPLATES, TOLERANCE));
    }

    @Test
    void zeeMatchesDrawnBackwards() {
        //a Z traced right-to-left is still a Z (direction-reversal is allowed)
        List<float[]> stroke = strokeOf(new float[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}});
        List<float[]> reversed = new ArrayList<>(stroke);
        java.util.Collections.reverse(reversed);
        assertEquals(1, GlyphClassifier.classify(reversed, TEMPLATES, TOLERANCE));
    }

    @Test
    void junkStrokeIsRejected() {
        //dense scribble that resembles nothing
        List<float[]> noise = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            noise.add(new float[]{(float) Math.sin(i * 1.7), (float) Math.cos(i * 2.3)});
        }
        assertEquals(-1, GlyphClassifier.classify(noise, TEMPLATES, TOLERANCE));
    }

    @Test
    void degenerateStrokeIsRejected() {
        assertEquals(-1, GlyphClassifier.classify(List.of(), TEMPLATES, TOLERANCE));
        assertEquals(-1, GlyphClassifier.classify(List.of(new float[]{1, 1}), TEMPLATES, TOLERANCE));
    }

    @Test
    void squareDoesNotMatchOpenShapes() {
        //a closed square legitimately sits near the circle template for a pure
        //shape-distance matcher — the hard requirement is it must not collapse
        //into the open strokes (vee/channel)
        List<float[]> square = closedStrokeOf(new float[][]{
                {0, 0}, {1, 0}, {1, 1}, {0, 1}});
        int id = GlyphClassifier.classify(square, TEMPLATES, TOLERANCE);
        assertTrue(id == -1 || id == 0,
                "a square should be rejected or read as the closed shape, got " + id);
    }
}
