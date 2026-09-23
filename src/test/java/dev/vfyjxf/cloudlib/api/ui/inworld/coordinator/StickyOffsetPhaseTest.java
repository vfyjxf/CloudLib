package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sticky phase regression: a sticky element whose candidate lattice
 * rounds to integer pixels must not have its committed offset re-derived
 * from that lattice at every resolve. The offset is the placement contract's
 * own storage — once a slot is granted, its offset is frozen and the render
 * position is the frozen offset plus the live anchor. Re-deriving the offset
 * from the candidate's integer rect re-phased it by up to half a pixel per
 * axis on every epoch (the rounding the lattice applies to a fractionally
 * drifting anchor), which after draw-time rounding showed as a ±1 px square
 * wave at the 5 Hz resolve cadence. The sticky keep now carries the
 * incumbent offset forward exactly; only a slot that genuinely stopped
 * fitting (clamp, nudge) is allowed to establish a new offset.
 */
class StickyOffsetPhaseTest {

    private static final int W = 1280;
    private static final int H = 720;
    private static final double dt = 1.0 / 60.0;

    /**
     * A sticky follow-style element with the zone-lattice shape: candidates
     * dock to the anchor and snap to integer pixels, so a fractional anchor
     * drift perturbs the lattice rounding while the docked clearance stays
     * constant.
     */
    private static final class LatticeElement implements InworldElement {

        private final String id;
        private final Size size = new Size(96, 60);
        private FloatPos anchor;

        LatticeElement(String id, double anchorX, double anchorY) {
            this.id = id;
            this.anchor = new FloatPos(anchorX, anchorY);
        }

        void moveTo(double x, double y) {
            anchor = new FloatPos(x, y);
        }

        private FloatRect docked(double clearanceX, double clearanceY) {
            double centerX = anchor.x() + clearanceX;
            double centerY = anchor.y() + clearanceY;
            int x = (int) Math.round(centerX - size.width() * 0.5);
            int y = (int) Math.round(centerY - size.height() * 0.5);
            return new FloatRect(x, y, size.width(), size.height());
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            List<PlacementCandidate> candidates = new ArrayList<>(3);
            candidates.add(PlacementCandidate.screen(docked(size.width() * 0.5 + 8, 0)));
            candidates.add(PlacementCandidate.screen(docked(-(size.width() * 0.5 + 8), 0)));
            candidates.add(PlacementCandidate.screen(docked(0, 0)));
            return ElementProposal.of(context.variant(), anchor, candidates);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SpaceKind spaceKind() {
            return SpaceKind.world;
        }

        @Override
        public VariantLadder ladder() {
            InworldVariant rung = new InworldVariant(
                0,
                size,
                ContentTier.full,
                SpacePolicy.active,
                false,
                true,
                0.75 * 96 * 60
            );
            return VariantLadder.of(List.of(rung));
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public boolean sticky() {
            return true;
        }

        @Override
        public ElementMode mode() {
            return ElementMode.selfManaged;
        }
    }

    private static CoordinationResult frame(InworldCoordinator coordinator, double now, Rect... exclusions) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(W, H, now, dt, exclusions));
    }

    @Test
    void subPixelAnchorDriftNeverReRoundsTheCommittedOffset() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        // a fractional birth anchor: the granted offset itself is fractional
        // (integer lattice rect minus fractional anchor), the shape most
        // exposed to re-rounding
        LatticeElement element = new LatticeElement("e", 300.25, 200.6);
        coordinator.register(element);

        // the preferred right slot is excluded at birth — the grant lands
        // left, and the left slot stays comfortably inside the screen for
        // the whole walk below (no clamp: a clamped slot legitimately
        // establishes a new offset)
        Rect overRight = new Rect(300, 120, 240, 160);
        CoordinationResult first = frame(coordinator, 0, overRight);
        FloatRect granted = first.placementOf("e").offsetRect();
        assertTrue(granted.x() < 0, "granted the left slot: " + granted);
        assertTrue(granted.x() != Math.floor(granted.x()), "the test needs a fractional granted offset: " + granted);

        // the anchor drifts fractionally — per-frame steps that never cross
        // the .5 rounding boundary by whole pixels, across many epochs (a
        // resolve every 0.2 s) and an anchor-displacement resolve (the walk
        // passes the 12 px threshold)
        double now = dt;
        for (int f = 0; f < 120; f++) {
            element.moveTo(300.25 + f * 0.31, 200.6 + f * 0.17);
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            InworldPlacement placement = result.placementOf("e");
            // the committed offset is bit-identical to the grant: no epoch
            // ever re-derives it from the candidate's integer rect
            assertEquals(granted, placement.offsetRect(), "frame " + f + ": the committed offset re-phased");
            // and the render rect is exactly frozen offset + live anchor —
            // no ±1 px re-rounding anywhere in the chain
            FloatPos anchor = placement.anchor();
            assertEquals(
                anchor.x() + granted.x(),
                placement.screenRect().x(),
                1.0e-9,
                "frame " + f + ": rides the anchor"
            );
            assertEquals(
                anchor.y() + granted.y(),
                placement.screenRect().y(),
                1.0e-9,
                "frame " + f + ": rides the anchor"
            );
        }
        // sanity: the walk really did cross many resolve boundaries
        assertTrue(coordinator.epoch() >= 8, "expected many epochs, saw " + coordinator.epoch());
    }

    @Test
    void aSlotThatStoppedFittingStillEstablishesANewOffset() {
        // the freeze is not a deadlock: when the held slot leaves the screen
        // and the fit clamps it, the clamped rect legitimately becomes the
        // new slot — the offset changes exactly then (the moving-anchor
        // re-pin regression covers the full walk; this pins the boundary)
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        LatticeElement element = new LatticeElement("e", 40, 200);
        coordinator.register(element);

        CoordinationResult first = frame(coordinator, 0);
        FloatRect granted = first.placementOf("e").offsetRect();
        assertEquals(granted, frame(coordinator, dt).placementOf("e").offsetRect(), "a fitting slot never re-phases");

        // walk the anchor off the left edge: the clamped fit must produce a
        // different (edge-following) offset, not the frozen one
        element.moveTo(-20, 200);
        CoordinationResult clamped = frame(coordinator, 2 * dt);
        assertTrue(
            clamped.placementOf("e").offsetRect().x() > granted.x(),
            "the clamped slot establishes its own offset: " + clamped.placementOf("e").offsetRect()
        );
        assertEquals(0, clamped.placementOf("e").screenRect().x(), 0.01, "clamped to the work-area edge");
    }
}
