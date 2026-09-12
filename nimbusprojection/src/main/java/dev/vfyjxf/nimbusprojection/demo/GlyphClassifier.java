package dev.vfyjxf.nimbusprojection.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny $1-recognizer-style stroke classifier for the sigil pad: resample the
 * ink to N equidistant points, scale the bounding box to the unit square
 * (non-uniform — direction and proportions stay meaningful), center the
 * centroid, then compare mean point distance against each template.
 * <p>
 * Stroke direction is respected (a mirrored Z is not a Z); templates marked
 * {@code cyclic} additionally match under any cyclic point shift, so a closed
 * shape recognizes regardless of where the pen went down.
 * <p>
 * Pure math, no Minecraft types — unit-testable.
 */
public final class GlyphClassifier {

    public record Template(int id, String name, float[][] points, boolean cyclic) {

        public static Template of(int id, String name, float[][] points) {
            return new Template(id, name, points, false);
        }

        public static Template cyclic(int id, String name, float[][] points) {
            return new Template(id, name, points, true);
        }
    }

    public static final int SAMPLE = 16;

    private GlyphClassifier() {
    }

    /** Best-matching template id, or -1 when nothing beats {@code maxDist}. */
    public static int classify(List<float[]> stroke, List<Template> templates, float maxDist) {
        if (stroke.size() < 2) return -1;
        float[][] sample = normalize(resample(stroke, SAMPLE));
        int best = -1;
        float bestDist = maxDist;
        for (Template t : templates) {
            float d = matchDistance(sample, t);
            if (d < bestDist) {
                bestDist = d;
                best = t.id();
            }
        }
        return best;
    }

    /** Mean point distance after normalization, honoring direction + cyclic shifts. */
    static float matchDistance(float[][] sample, Template template) {
        //templates store corner paths — resample them to SAMPLE points like the
        //ink (cyclic templates close the loop so the seam edge is walked too)
        List<float[]> path = new ArrayList<>(List.of(template.points()));
        if (template.cyclic() && path.size() > 1) path.add(path.get(0).clone());
        float[][] tp = normalize(resample(path, SAMPLE));
        float d = meanDist(sample, tp, 0, false);
        if (template.cyclic()) {
            for (int shift = 1; shift < SAMPLE; shift++) {
                d = Math.min(d, meanDist(sample, tp, shift, false));
                d = Math.min(d, meanDist(sample, tp, shift, true));
            }
        } else {
            d = Math.min(d, meanDist(sample, tp, 0, true));
        }
        return d;
    }

    /** Mean euclidean distance; {@code rev} walks the template backwards, {@code shift} cyclically offsets it. */
    private static float meanDist(float[][] a, float[][] b, int shift, boolean rev) {
        float sum = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int j = (i + shift) % n;
            float[] p = rev ? b[n - 1 - j] : b[j];
            float dx = a[i][0] - p[0], dy = a[i][1] - p[1];
            sum += Math.sqrt(dx * dx + dy * dy);
        }
        return sum / n;
    }

    /** Resample the polyline to {@code n} equidistant points along its arc length. */
    public static float[][] resample(List<float[]> stroke, int n) {
        float total = 0;
        for (int i = 1; i < stroke.size(); i++) {
            total += dist(stroke.get(i - 1), stroke.get(i));
        }
        float[][] out = new float[n][];
        if (total < 1e-4f) {
            float[] p = stroke.get(0);
            for (int i = 0; i < n; i++) out[i] = new float[]{p[0], p[1]};
            return out;
        }
        float stepLen = total / (n - 1);
        float acc = 0;
        List<float[]> pts = new ArrayList<>(stroke);
        out[0] = pts.get(0).clone();
        int idx = 1;
        float[] prev = pts.get(0);
        for (int i = 1; i < pts.size() && idx < n; i++) {
            float[] cur = pts.get(i);
            float d = dist(prev, cur);
            while (acc + d >= stepLen && idx < n) {
                float t = (stepLen - acc) / d;
                prev = new float[]{prev[0] + t * (cur[0] - prev[0]), prev[1] + t * (cur[1] - prev[1])};
                out[idx++] = prev.clone();
                d = dist(prev, cur);
                acc = 0;
            }
            acc += d;
            prev = cur;
        }
        while (idx < n) out[idx++] = pts.get(pts.size() - 1).clone();
        return out;
    }

    /** Scale to the unit box (non-uniform) and re-center on the centroid. */
    static float[][] normalize(float[][] pts) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] p : pts) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        float w = Math.max(maxX - minX, 1e-3f), h = Math.max(maxY - minY, 1e-3f);
        float[][] out = new float[pts.length][2];
        float cx = 0, cy = 0;
        for (int i = 0; i < pts.length; i++) {
            out[i][0] = (pts[i][0] - minX) / w;
            out[i][1] = (pts[i][1] - minY) / h;
            cx += out[i][0];
            cy += out[i][1];
        }
        cx /= pts.length;
        cy /= pts.length;
        for (float[] p : out) {
            p[0] -= cx;
            p[1] -= cy;
        }
        return out;
    }

    private static float dist(float[] a, float[] b) {
        float dx = a[0] - b[0], dy = a[1] - b[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
