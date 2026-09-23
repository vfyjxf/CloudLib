package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import java.util.Arrays;

/**
 * The frame-cost measurement counters behind {@code /nimbus perf} — a
 * measurement-only side channel: production code increments plain longs at
 * the wired call sites, nothing reads them except the perf tooling, and no
 * rendering decision ever consults this class. Overhead is one array
 * increment per event (no synchronization, no allocation on the counter
 * path).
 * <p>
 * <b>Thread contract:</b> single-threaded on the render thread, matching
 * every wired call site ({@code UiSurface.render}, canvas flushes, the world
 * renderer's level stage, the gui pass) — NeoForge fires client ticks on that
 * same thread, so the perf window logic may snapshot without races. Callers
 * on other threads see at worst a torn aggregate, never a broken pipeline.
 * <p>
 * <b>Frame model:</b> {@link #beginFrame()} zeroes the per-frame
 * accumulators, {@link #endFrame()} folds them into window aggregates
 * (totals and per-frame peaks) and counts the frame. A frame that was never
 * ended is folded lazily by the next {@code beginFrame}, so a missing
 * end-of-frame hook degrades resolution by one frame, never drops data.
 * {@link #reset()} starts a fresh measurement window and stamps
 * {@link Snapshot#resetAtNanos} for cumulative fps.
 * <p>
 * {@link Metric#panelCount} is a gauge, not an event counter: the world
 * renderer <em>sets</em> it once per frame (assignment, not increment), so
 * the average over frames is the mean active panel count.
 */
public final class RenderStats {

    /** The wired counters — see the class docs for each one's call site. */
    public enum Metric {
        /** {@code UiSurface.render} repaints of a panel FBO (per frame). */
        surfaceRenders,
        /** FBO texture (re)allocations — new target or resize. */
        surfaceResizes,
        /** {@code glGenerateMipmap} invocations. */
        mipmapGenerations,
        /** Sum of active surface texels this frame (the supersample budget in use). */
        texelBudgetUsed,
        /** World panel quad draws ({@code WorldUiRenderer.drawPanelQuad}). */
        quadDraws,
        /** Line mesh draws — the world pass's per-panel companion lines and the emitters batch. */
        lineDraws,
        /** Item renders ({@code SceneCanvas.renderItem}/{@code renderItemIcon}). */
        itemRenders,
        /** Framebuffer binds inside {@code UiSurface} (enter + restore per repaint). */
        fboBinds,
        /** Canvas batch draw calls flushed by {@code SceneCanvas.flushBatch}. */
        canvasBatches,
        /** Vanilla buffer flushes (text/sprite draws) triggered by a canvas flush. */
        textBatches,
        /** Gauge: panels composited by the world pass this frame. */
        panelCount
    }

    private static final int metricCount = Metric.values().length;

    /**
     * An immutable copy of the aggregates — what {@link #snapshot()} hands to
     * reporting code. Averages are per <em>completed</em> frame; peaks are the
     * maximum single-frame values; {@code resetAtNanos} is the
     * {@link System#nanoTime()} stamp of the last {@link #reset()} (0 before
     * any reset).
     */
    public record Snapshot(long frames, long[] totals, long[] peaks, long resetAtNanos) {

        public Snapshot {
            totals = totals.clone();
            peaks = peaks.clone();
        }

        @Override
        public long[] totals() {
            return totals.clone();
        }

        @Override
        public long[] peaks() {
            return peaks.clone();
        }

        /** The window's total of a metric (or its per-frame value for gauges). */
        public long total(Metric metric) {
            return totals[metric.ordinal()];
        }

        /** The highest single-frame value a metric reached in the window. */
        public long peak(Metric metric) {
            return peaks[metric.ordinal()];
        }

        /** The metric's total divided by completed frames (0 for an empty window). */
        public double avgPerFrame(Metric metric) {
            return frames == 0 ? 0.0 : (double) totals[metric.ordinal()] / frames;
        }
    }

    private static final long[] frame = new long[metricCount];
    private static final long[] totals = new long[metricCount];
    private static final long[] peaks = new long[metricCount];

    private static long frames;
    private static long resetAtNanos;
    private static boolean frameOpen;

    private RenderStats() {}

    // region frame lifecycle

    /** Starts a frame: folds any unfinished previous frame, then zeroes the accumulators. */
    public static void beginFrame() {
        if (frameOpen) foldFrame();
        Arrays.fill(frame, 0L);
        frameOpen = true;
    }

    /**
     * Completes the frame: folds the accumulators into the window aggregates.
     * Folding happens at most once per frame — extra calls are no-ops.
     */
    public static void endFrame() {
        if (!frameOpen) return;
        foldFrame();
        frameOpen = false;
    }

    private static void foldFrame() {
        frames++;
        for (int i = 0; i < metricCount; i++) {
            totals[i] += frame[i];
            if (frame[i] > peaks[i]) peaks[i] = frame[i];
        }
    }

    /** Zeroes every aggregate and starts a fresh measurement window. */
    public static void reset() {
        Arrays.fill(frame, 0L);
        Arrays.fill(totals, 0L);
        Arrays.fill(peaks, 0L);
        frames = 0;
        resetAtNanos = System.nanoTime();
        frameOpen = false;
    }

    /** A copy of the window aggregates; the live counters are untouched. */
    public static Snapshot snapshot() {
        return new Snapshot(frames, totals, peaks, resetAtNanos);
    }

    // endregion

    // region counters — plain long writes on the render thread

    /** A panel FBO was repainted; {@code texels} is its active w×h texture size. */
    public static void surfaceRendered(long texels) {
        frame[Metric.surfaceRenders.ordinal()]++;
        frame[Metric.texelBudgetUsed.ordinal()] += texels;
    }

    /**
     * A panel's surface was kept from the previous paint (the content-version
     * cache skipped the repaint) — its texture stays active, so the texel
     * budget in use still counts it.
     */
    public static void surfaceCached(long texels) {
        frame[Metric.texelBudgetUsed.ordinal()] += texels;
    }

    /** A surface's color/depth storage was allocated or resized. */
    public static void surfaceResized() {
        frame[Metric.surfaceResizes.ordinal()]++;
    }

    /** {@code glGenerateMipmap} ran against a surface texture. */
    public static void mipmapGenerated() {
        frame[Metric.mipmapGenerations.ordinal()]++;
    }

    /** A world panel quad was submitted to the world pass. */
    public static void quadDrawn() {
        frame[Metric.quadDraws.ordinal()]++;
    }

    /** A line mesh was submitted to the world pass. */
    public static void lineDrawn() {
        frame[Metric.lineDraws.ordinal()]++;
    }

    /** An item was rendered through the canvas ({@code renderItem}/{@code renderItemIcon}). */
    public static void itemRendered() {
        frame[Metric.itemRenders.ordinal()]++;
    }

    /** A framebuffer bind issued inside a surface repaint. */
    public static void fboBound() {
        frame[Metric.fboBinds.ordinal()]++;
    }

    /** {@code count} canvas batches (same-type runs) were flushed as draw calls. */
    public static void canvasBatchesFlushed(int count) {
        frame[Metric.canvasBatches.ordinal()] += count;
    }

    /** A vanilla buffer flush (pending text/sprite draws) was triggered by a canvas flush. */
    public static void textBatchFlushed() {
        frame[Metric.textBatches.ordinal()]++;
    }

    /** Gauge: how many panels the world pass composited this frame. */
    public static void panelCount(int count) {
        frame[Metric.panelCount.ordinal()] = count;
    }

    // endregion
}
