package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RenderStats} window semantics: frame reset, aggregation, peaks,
 * gauges and reset — headless, against the static counter registry.
 */
class RenderStatsTest {

    @BeforeEach
    @AfterEach
    void resetRegistry() {
        RenderStats.reset();
    }

    @Test
    void countersAccumulateWithinAFrame() {
        RenderStats.beginFrame();
        RenderStats.surfaceRendered(100);
        RenderStats.surfaceRendered(50);
        RenderStats.surfaceResized();
        RenderStats.mipmapGenerated();
        RenderStats.quadDrawn();
        RenderStats.lineDrawn();
        RenderStats.itemRendered();
        RenderStats.itemRendered();
        RenderStats.itemRendered();
        RenderStats.fboBound();
        RenderStats.canvasBatchesFlushed(4);
        RenderStats.textBatchFlushed();
        RenderStats.endFrame();

        RenderStats.Snapshot snap = RenderStats.snapshot();
        assertEquals(1, snap.frames());
        assertEquals(2, snap.total(RenderStats.Metric.surfaceRenders));
        assertEquals(1, snap.total(RenderStats.Metric.surfaceResizes));
        assertEquals(1, snap.total(RenderStats.Metric.mipmapGenerations));
        assertEquals(150, snap.total(RenderStats.Metric.texelBudgetUsed));
        assertEquals(1, snap.total(RenderStats.Metric.quadDraws));
        assertEquals(1, snap.total(RenderStats.Metric.lineDraws));
        assertEquals(3, snap.total(RenderStats.Metric.itemRenders));
        assertEquals(1, snap.total(RenderStats.Metric.fboBinds));
        assertEquals(4, snap.total(RenderStats.Metric.canvasBatches));
        assertEquals(1, snap.total(RenderStats.Metric.textBatches));
    }

    @Test
    void beginFrameResetsCountersWithoutTouchingAggregates() {
        RenderStats.beginFrame();
        RenderStats.quadDrawn();
        RenderStats.endFrame();
        RenderStats.beginFrame();
        RenderStats.endFrame(); // empty frame — folds zeros, counts the frame

        RenderStats.Snapshot snap = RenderStats.snapshot();
        assertEquals(2, snap.frames());
        assertEquals(1, snap.total(RenderStats.Metric.quadDraws));
        assertEquals(0.5, snap.avgPerFrame(RenderStats.Metric.quadDraws), 1e-9);
    }

    @Test
    void averagesAndPeaksArePerFrame() {
        RenderStats.beginFrame();
        RenderStats.surfaceRendered(100);
        RenderStats.panelCount(2);
        RenderStats.endFrame();
        RenderStats.beginFrame();
        RenderStats.surfaceRendered(700);
        RenderStats.panelCount(6);
        RenderStats.endFrame();
        RenderStats.beginFrame();
        RenderStats.surfaceRendered(300);
        RenderStats.surfaceRendered(50);
        RenderStats.panelCount(1);
        RenderStats.endFrame();

        RenderStats.Snapshot snap = RenderStats.snapshot();
        assertEquals(3, snap.frames());
        assertEquals(1150, snap.total(RenderStats.Metric.texelBudgetUsed));
        assertEquals(700, snap.peak(RenderStats.Metric.texelBudgetUsed));
        assertEquals(4, snap.total(RenderStats.Metric.surfaceRenders));
        assertEquals(2, snap.peak(RenderStats.Metric.surfaceRenders));
        // gauge: averaged like the event counters, peak is the busiest frame
        assertEquals(9, snap.total(RenderStats.Metric.panelCount));
        assertEquals(3.0, snap.avgPerFrame(RenderStats.Metric.panelCount), 1e-9);
        assertEquals(6, snap.peak(RenderStats.Metric.panelCount));
    }

    @Test
    void unfinishedFrameFoldsLazilyAtNextBegin() {
        RenderStats.beginFrame();
        RenderStats.quadDrawn();
        // no endFrame — beginFrame must fold the stale frame, not drop it
        RenderStats.beginFrame();
        RenderStats.quadDrawn();
        RenderStats.quadDrawn();
        RenderStats.endFrame();

        RenderStats.Snapshot snap = RenderStats.snapshot();
        assertEquals(2, snap.frames());
        assertEquals(3, snap.total(RenderStats.Metric.quadDraws));
    }

    @Test
    void extraEndFrameIsANoOp() {
        RenderStats.beginFrame();
        RenderStats.quadDrawn();
        RenderStats.endFrame();
        RenderStats.endFrame();
        RenderStats.endFrame();

        RenderStats.Snapshot snap = RenderStats.snapshot();
        assertEquals(1, snap.frames());
        assertEquals(1, snap.total(RenderStats.Metric.quadDraws));
    }

    @Test
    void endFrameWithoutBeginIsANoOp() {
        RenderStats.endFrame();
        assertEquals(0, RenderStats.snapshot().frames());
    }

    @Test
    void resetStartsACleanWindow() {
        RenderStats.reset();
        RenderStats.beginFrame();
        RenderStats.quadDrawn();
        RenderStats.endFrame();
        RenderStats.reset();

        RenderStats.Snapshot snap = RenderStats.snapshot();
        assertEquals(0, snap.frames());
        assertEquals(0, snap.total(RenderStats.Metric.quadDraws));
        assertEquals(0, snap.peak(RenderStats.Metric.quadDraws));
        assertTrue(snap.resetAtNanos() > 0);
    }

    @Test
    void snapshotIsDefensivelyCopied() {
        RenderStats.beginFrame();
        RenderStats.quadDrawn();
        RenderStats.endFrame();

        RenderStats.Snapshot snap = RenderStats.snapshot();
        // mutating the exposed arrays must not leak into the registry...
        snap.totals()[RenderStats.Metric.quadDraws.ordinal()] = 999;
        assertEquals(1, RenderStats.snapshot().total(RenderStats.Metric.quadDraws));
        // ...and the record's constructor clones what it is handed
        long[] totals = snap.totals().clone();
        RenderStats.Snapshot copy = new RenderStats.Snapshot(snap.frames(), totals, snap.peaks(), 0);
        totals[RenderStats.Metric.quadDraws.ordinal()] = 999;
        assertEquals(1, copy.total(RenderStats.Metric.quadDraws));
        // a flat 1-per-frame history peaks at 1
        assertEquals(1, snap.peak(RenderStats.Metric.quadDraws));
    }
}
