package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stillness contract of the full adaptive chain
 * {@code WorldUiRenderer.renderSurfaces} drives per panel:
 * {@code projectQuad → desired → SupersampleController (stable) →
 * Supersampling.allocate (budget) → SupersampleController (grant)} — the
 * second controller is the grant stabilizer that keeps budget-driven step
 * downs (other panels toggling visibility, screen areas reordering) from
 * resizing the texture in segments. Nothing here may touch GL; the chain is
 * the real production math.
 */
class AdaptiveStabilityTest {

    /** One panel's cascade through the frame, mirroring renderSurfaces. */
    private static final class PanelChain {

        final Supersampling.Request seed;
        final SupersampleController stable = new SupersampleController();
        final SupersampleController grant = new SupersampleController();
        int applied = -1;
        int switches;

        PanelChain(int logicalW, int logicalH, int floor, double screenArea) {
            seed = new Supersampling.Request(logicalW, logicalH, 0, floor, screenArea);
        }

        /** @param projectedHeight framebuffer px, or a negative value for "no projection this frame" */
        int frame(double projectedHeight, long budget, List<PanelChain> all) {
            int desired = Supersampling.desired(Math.max(0, projectedHeight), seed.logicalHeight(), seed.floor());
            int stabilized = stable.observe(desired);
            List<Supersampling.Request> requests = new ArrayList<>();
            for (PanelChain chain : all) {
                requests.add(
                    new Supersampling.Request(
                        chain.seed.logicalWidth(),
                        chain.seed.logicalHeight(),
                        chain == this ? stabilized : chain.lastStable,
                        chain.seed.floor(),
                        chain.seed.screenArea()
                    )
                );
            }
            int grantedRaw = Supersampling.allocate(requests, budget)[all.indexOf(this)];
            int granted = grant.observe(grantedRaw);
            if (granted != applied) {
                switches++;
                applied = granted;
            }
            lastStable = stabilized;
            return granted;
        }

        private int lastStable;
    }

    // region stillness (test: static projection + ±1px noise + nulls + segment flips)

    @Test
    void aStillPanelNeverResizesItsSurface() {
        // 100px logical panel, floor 2. The projected height sits exactly on
        // the round() boundary (350 → desired 4, 349 → 3): the worst spot
        // for sensor noise — every ±1px frame flips the raw desired between
        // 3 and 4. Micro-jitter is high-frequency: noise runs stay short
        // (capped at 4 frames below), and the cascade must absorb all of it
        PanelChain panel = new PanelChain(120, 100, 2, 90_000);
        List<PanelChain> all = List.of(panel);
        long seed = 42;
        int run = 0;
        int side = 0;
        boolean high = true;
        for (int frame = 0; frame < 600; frame++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) >>> 17;
            if (run == 0) {
                // alternate the side each repick (−1 and +1 both round away
                // from 350's boundary side; consecutive same-side picks
                // could otherwise chain past the window)
                side = high ? 1 : -1;
                high = !high;
                run = 1 + (int) (seed % 4); // hold it 1..4 frames — jitter, not drift
            } else {
                run--;
            }
            double projected = (frame % 97 == 96) ? -1 : 350 + side; // occasional degenerate frame
            int granted = panel.frame(projected, Supersampling.defaultTexelBudget, all);
            assertTrue(granted >= 2 && granted <= Supersampling.maxSupersample);
        }
        assertEquals(
            1,
            panel.switches,
            "the initial allocation only — short-run noise, degenerate frames and"
                    + " rounding flaps never resize the surface"
        );
        assertTrue(panel.applied == 3 || panel.applied == 4, "held on whichever side the first frame sampled");
    }

    @Test
    void unboundedNoiseAtAKnifeEdgeStillBarelyResizes() {
        // the honest caveat: noise sitting exactly on the rounding boundary
        // can occasionally string together a run longer than the stability
        // window — that is a sustained observation and the controller may
        // follow it, but a 600-frame jitter burst costs at most a couple of
        // resizes, not one per flip
        PanelChain panel = new PanelChain(120, 100, 2, 90_000);
        List<PanelChain> all = List.of(panel);
        long seed = 42;
        for (int frame = 0; frame < 600; frame++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) >>> 17;
            double projected = 350 + ((seed % 3) - 1);
            panel.frame(projected, Supersampling.defaultTexelBudget, all);
        }
        assertTrue(panel.switches <= 3, "expected at most a couple of knife-edge switches, got " + panel.switches);
    }

    @Test
    void adjacentStepSegmentFlipsBelowTheWindowNeverResize() {
        // the boundary-jitter shape at segment granularity: the desired
        // value flips between adjacent factors in runs shorter than the
        // stability window (8 frames < 10), sustained for seconds
        PanelChain panel = new PanelChain(120, 100, 2, 90_000);
        List<PanelChain> all = List.of(panel);
        for (int frame = 0; frame < 400; frame++) {
            boolean highSegment = (frame / 8) % 2 == 0;
            panel.frame(highSegment ? 350 : 330, Supersampling.defaultTexelBudget, all);
        }
        assertEquals(1, panel.switches, "segment flips below the window are absorbed");
    }

    @Test
    void aSustainedGenuineChangeResizesExactlyOnce() {
        PanelChain panel = new PanelChain(120, 100, 2, 90_000);
        List<PanelChain> all = List.of(panel);
        for (int frame = 0; frame < 100; frame++) panel.frame(350, Supersampling.defaultTexelBudget, all);
        assertEquals(4, panel.applied);
        for (int frame = 0; frame < 100; frame++) panel.frame(560, Supersampling.defaultTexelBudget, all);
        assertEquals(6, panel.applied, "the sustained change is followed");
        assertEquals(2, panel.switches, "initial allocation + exactly one resize");
    }

    // endregion

    // region budget jitter through the grant stabilizer

    private static final long survivorBudget = 400L * 300 * 36 + 400L * 300 * 9; // B@6 + A@3

    @Test
    void budgetPressureSegmentFlipsNeverResizeTheSurvivor() {
        // two equal panels sharing a budget that fits B at ss6 (B is served
        // first — bigger projected area) plus A at ss3, or A alone at ss6:
        // B toggling presence in 8-frame segments flaps A's raw grant
        // 6↔3 — the grant stabilizer holds A's texture
        PanelChain a = new PanelChain(400, 300, 1, 1_000_000);
        PanelChain b = new PanelChain(400, 300, 1, 2_000_000);
        List<PanelChain> both = List.of(b, a);
        List<PanelChain> onlyA = List.of(a);
        for (int frame = 0; frame < 480; frame++) {
            boolean bPresent = (frame / 8) % 2 == 1;
            a.frame(1800, survivorBudget, bPresent ? both : onlyA);
            if (bPresent) b.frame(1800, survivorBudget, both);
        }
        assertEquals(1, a.switches, "the raw grant flaps with the budget; the texture must not");
        assertEquals(6, a.applied);
    }

    @Test
    void sustainedOverspendStillStepsDown() {
        // the stabilizer delays, never blocks: pressure that holds longer
        // than the window does step the panel down
        PanelChain a = new PanelChain(400, 300, 1, 1_000_000);
        PanelChain b = new PanelChain(400, 300, 1, 2_000_000);
        List<PanelChain> both = List.of(b, a);
        List<PanelChain> onlyA = List.of(a);
        for (int frame = 0; frame < 60; frame++) a.frame(1800, survivorBudget, onlyA);
        assertEquals(6, a.applied);
        for (int frame = 0; frame < 60; frame++) {
            a.frame(1800, survivorBudget, both);
            b.frame(1800, survivorBudget, both);
        }
        assertTrue(a.applied < 6, "sustained overspend steps the panel down (got " + a.applied + ")");
        assertEquals(2, a.switches);
    }

    // endregion
}
