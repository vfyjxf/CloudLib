package dev.vfyjxf.cloudlib.api.ui.inworld.stress;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ContentTier;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldElement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldVariant;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ProposeContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.VariantLadder;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.stress.StressElement.Motion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The large-scale property-style stress suite for the coordinator pipeline:
 * seeded generator populations (mixed kinds, policies, ladders and motion
 * modes; exclusion, membership, retraction and resize churn) driven for
 * hundreds to a thousand frames with every frame checked against the
 * {@link FrameInvariants} oracle. Failure messages carry the seed, the first
 * violating frame and the full spec — the minimal reproduction.
 * <p>
 * Suite budget is deliberately bounded: scenario sizes are tuned (the big
 * scenarios scale element sizes up so the same over-constrained 240-element
 * populations stay inside the time budget) for a projected green run well
 * under a minute, while the fail-fast oracle makes the currently-red suite
 * much faster still. The floor scenario — 240 mixed elements × 1000 frames ×
 * three seeds — is kept intact.
 */
class CoordinatorStressTest {

    private static final long[] seeds = {0xC0FFEE, 0x5EED1, 0x98765432L};

    @Test
    void largeMixedPopulationHoldsInvariantsAcrossSeeds() {
        for (long seed : seeds) {
            StressScenario.RunSummary summary =
                    StressScenario.run(StressScenario.Spec.of(seed, 240, 1000).withSizeScale(2.2));
            // the run must actually exercise the machinery, not pass by idling
            assertTrue(
                    summary.peakPresented() > 20,
                    "expected a busy scene, peak presented was " + summary.peakPresented());
            assertTrue(summary.resolvedFrames() > 50, "expected regular resolves: " + summary.causeCounts());
            assertTrue(summary.canonicalFrames().size() == 1000);
            // the static tail must have been strict for a while
            assertTrue(summary.causeCount(CoordinationResult.RenegotiationCause.epochElapsed) > 10);
        }
    }

    @Test
    void sameSeedReproducesByteIdenticalRuns() {
        StressScenario.Spec spec =
                StressScenario.Spec.of(0xABCD12, 90, 450).withChurn(14, 8).withResizes(2);
        // invariants off for this witness only: the continuity violation is
        // reported by the other tests (and the minimal repro below); the
        // determinism property deserves its own unmasked verdict
        StressScenario.RunSummary first = StressScenario.run(spec, false);
        StressScenario.RunSummary second = StressScenario.run(spec, false);
        assertEquals(first.canonicalFrames(), second.canonicalFrames(), "same seed must replay identically");
        assertEquals(first.causeCounts(), second.causeCounts());
        assertEquals(first.presentedFlips(), second.presentedFlips());
    }

    @Test
    void staticSceneSettlesAndNeverReshuffles() {
        StressScenario.RunSummary summary = StressScenario.run(StressScenario.Spec.of(0x57A71C, 70, 420)
                .withMotion(Motion.staticAnchor)
                .withStaticTail(0.5));
        // the strict window (last 120 frames minus settle) enforced zero
        // flips/offset/level changes frame by frame; here we sanity-check the
        // scene was actually populated and that inside that window it resolves
        // only on the epoch tick. The membership, exclusion and retraction
        // churn all belongs to the dynamic phase — the spec schedules it by
        // construction, so a whole-run cause count can never be zero — hence
        // the cause census is read off the tail itself.
        assertTrue(summary.peakPresented() > 30);
        assertTrue(summary.causeCount(CoordinationResult.RenegotiationCause.anchorDisplacement) == 0);
        List<String> tailCauses = tailCauses(summary, 120);
        assertTrue(
                tailCauses.stream()
                        .allMatch(cause -> cause.equals(CoordinationResult.RenegotiationCause.none.name())
                                || cause.equals(CoordinationResult.RenegotiationCause.epochElapsed.name())),
                "the settled tail must re-resolve only on the epoch tick: " + tailCauses);
        assertTrue(
                tailCauses.contains(CoordinationResult.RenegotiationCause.epochElapsed.name()),
                "the settled tail must still re-resolve on the epoch: " + tailCauses);
    }

    /** The renegotiation cause of each of the run's last {@code frames} frames. */
    private static List<String> tailCauses(StressScenario.RunSummary summary, int frames) {
        List<String> canonical = summary.canonicalFrames();
        return canonical.subList(canonical.size() - frames, canonical.size()).stream()
                .map(frame -> frame.split("\\|")[3])
                .toList();
    }

    @Test
    void subThresholdJitterNeverClaimsAnchorDisplacement() {
        StressScenario.RunSummary summary =
                StressScenario.run(StressScenario.Spec.of(0x3117E, 60, 400).withMotion(Motion.jitter));
        assertEquals(
                0,
                summary.causeCount(CoordinationResult.RenegotiationCause.anchorDisplacement),
                "±0.5px jitter cannot cross the 12px threshold within an epoch");
        assertTrue(summary.peakPresented() > 20);
    }

    @Test
    void allElementsShareOneAnchor() {
        StressScenario.RunSummary summary =
                StressScenario.run(StressScenario.Spec.of(0xA11CE, 200, 220).withSingleAnchor());
        // a single contested anchor: the contested spot holds a handful, the
        // rest must degrade or hide — never overlap, never crash
        assertTrue(summary.peakPresented() > 2, "at least the arbitration winners should place");
        assertTrue(summary.presentedFlips() > 10, "contention should churn membership of the presented set");
    }

    @Test
    void giantExclusionZeroesTheWorkArea() {
        StressScenario.RunSummary summary = StressScenario.run(StressScenario.Spec.of(0x2E10A5, 120, 280)
                .withGiantExclusion(60)
                .withStaticTail(0.2));
        assertTrue(summary.peakPresented() > 40, "the pre-squeeze scene should be busy");
        // after the full-screen exclusion lands, only policy-exempt elements
        // can stay presented; the rest hide (with linger), and every frame
        // still commits — the checker verified validity throughout
        assertTrue(summary.lastPresented() < 30, "expected mass hiding, last presented was " + summary.lastPresented());
    }

    @Test
    void everyoneDemandsTheStrongestRung() {
        StressScenario.RunSummary summary =
                StressScenario.run(StressScenario.Spec.of(0x57008, 160, 350).withRungs(1));
        // no degradation ladder to walk: rejected elements go straight to
        // hidden+linger; nothing may overlap and every frame commits
        assertTrue(summary.peakPresented() > 20);
    }

    @Test
    void massTeleportStorm() {
        StressScenario.RunSummary summary =
                StressScenario.run(StressScenario.Spec.of(0x7E1E09, 100, 300).withMotion(Motion.teleport));
        assertTrue(summary.causeCount(CoordinationResult.RenegotiationCause.anchorDisplacement) > 50);
        assertTrue(summary.peakPresented() > 20);
    }

    @Test
    void exclusionStormResolvesEveryFrame() {
        StressScenario.RunSummary summary = StressScenario.run(
                StressScenario.Spec.of(0x5702A, 90, 250).withStorm().withExclusions(4, 0));
        // every frame swaps an exclusion: every frame must re-resolve and
        // still produce a valid, continuous, committed layout
        assertEquals(250, summary.resolvedFrames(), "the storm must force a resolve every frame");
        assertTrue(summary.peakPresented() > 15);
    }

    @Test
    void resizeStormKeepsContinuity() {
        StressScenario.RunSummary summary = StressScenario.run(
                StressScenario.Spec.of(0x2E512E, 90, 500).withResizes(5).withStaticTail(0.25));
        assertTrue(summary.causeCount(CoordinationResult.RenegotiationCause.epochElapsed) > 5);
        assertTrue(summary.peakPresented() > 15);
    }

    @Test
    void membershipChurnUnderLoad() {
        StressScenario.RunSummary summary =
                StressScenario.run(StressScenario.Spec.of(0x0A2E5, 100, 350).withChurn(24, 10));
        assertTrue(summary.causeCount(CoordinationResult.RenegotiationCause.membershipChanged) > 20);
        assertTrue(summary.peakPresented() > 20);
    }

    @Test
    void mixedWorldOnlyPopulationHoldsInvariants() {
        for (long seed : new long[] {0x30B51A, 0x77D0}) {
            StressScenario.RunSummary summary = StressScenario.run(
                    StressScenario.Spec.of(seed, 120, 320).withWorldOnly(0.3).withChurn(12, 8));
            // both populations must actually exercise their flow: the
            // projecting one keeps arbitrating, the world-only one keeps
            // presenting unconditionally, and every frame passed the oracle
            assertTrue(summary.peakPresented() > 20, "expected a busy arbitrated scene");
            assertTrue(summary.peakWorldOnlyPresented() > 10, "expected the world-only flow to present");
            assertTrue(summary.canonicalFrames().size() == 320);
        }
    }

    /**
     * Minimal deterministic reproduction of the {@code visual-continuity}
     * violation the randomized scenarios catch: while a discrete FLIP is
     * animating, the per-frame displacement clamp is skipped, and Relax's
     * residual-overlap separation then shoves the visual by the full
     * overlap depth in a single frame — up to the element's own size, far
     * past the FLIP envelope. Here a 100×80 element glides through a fixed
     * 120×200 panel; as the crossing deepens, the minimal separation axis
     * switches from x to y and the landing spot jumps by the full depth.
     * This test stays red until main clamps separation displacement on
     * animating frames (or folds it into the animation state).
     */
    @Test
    void relaxSeparationMustBeClampedWhileFlipsAnimate() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        ProbeElement glider =
                new ProbeElement("glider", SpaceKind.world, 10, true, SpacePolicy.active, 100, 80, 80, 150);
        ProbeElement fixed =
                new ProbeElement("fixed", SpaceKind.panel, 0, false, SpacePolicy.fixed, 120, 200, 200, 150);
        coordinator.register(glider);
        coordinator.register(fixed);

        double dt = 1.0 / 60.0;
        double now = 0;
        FloatRect lastVisual = null;
        double maxMovement = 0;
        int worstFrame = -1;
        FloatRect worstFrom = null;
        FloatRect worstTo = null;
        for (int frame = 0; frame < 40; frame++) {
            now += dt;
            CoordinationResult result = coordinator.frame(InworldCoordinator.FrameInput.of(400, 300, now, dt));
            CoordinationResult.ElementState state = result.elementState("glider");
            FloatRect visual = state.visualRect();
            if (lastVisual != null) {
                double movement =
                        Math.hypot(visual.centerX() - lastVisual.centerX(), visual.centerY() - lastVisual.centerY());
                if (movement > maxMovement) {
                    maxMovement = movement;
                    worstFrame = frame + 1;
                    worstFrom = lastVisual;
                    worstTo = visual;
                }
            }
            lastVisual = visual;
            if (frame == 15) {
                // a 240px anchor jump: the sticky element keeps its slot and
                // FLIP glides it straight through the fixed panel
                glider.anchor = new FloatPos(320, 150);
            }
        }
        // the oracle's envelope: spring frames bounded by the relax clamp,
        // FLIP frames by the screen-diagonal amplitude over the flip
        // duration clamp
        double envelope = Math.max(24.0, 2 * Math.hypot(400, 300) * dt / 0.25) + 2.0;
        assertTrue(
                maxMovement <= envelope,
                "glider moved " + maxMovement + "px in frame " + worstFrame + " (envelope " + envelope + "): "
                        + worstFrom + " -> " + worstTo
                        + " — separation displacement is not clamped while a FLIP animates");
    }

    /** A precisely controllable single-candidate, single-rung test element. */
    private static final class ProbeElement implements InworldElement {

        private final String id;
        private final SpaceKind kind;
        private final int priority;
        private final boolean sticky;
        private final VariantLadder ladder;
        private final Size size;
        FloatPos anchor;

        ProbeElement(
                String id,
                SpaceKind kind,
                int priority,
                boolean sticky,
                SpacePolicy policy,
                int width,
                int height,
                double anchorX,
                double anchorY) {
            this.id = id;
            this.kind = kind;
            this.priority = priority;
            this.sticky = sticky;
            this.size = new Size(width, height);
            this.anchor = new FloatPos(anchorX, anchorY);
            this.ladder =
                    VariantLadder.of(List.of(new InworldVariant(0, size, ContentTier.full, policy, false, true, 1)));
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SpaceKind spaceKind() {
            return kind;
        }

        @Override
        public VariantLadder ladder() {
            return ladder;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean sticky() {
            return sticky;
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            FloatRect rect = FloatRect.around(anchor, size.width(), size.height());
            return ElementProposal.of(context.variant(), anchor, PlacementCandidate.screen(rect));
        }
    }
}
