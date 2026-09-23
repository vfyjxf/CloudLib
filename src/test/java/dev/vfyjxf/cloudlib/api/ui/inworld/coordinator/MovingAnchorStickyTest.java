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
 * The moving-anchor sticky regression: a sticky element whose candidate
 * lattice docks to its anchor (candidates translate with it, rects rounded to
 * integer pixels — the zone-lattice shape) must keep its granted slot across
 * resolves while the anchor drifts — the placement is stored anchor-relative,
 * so the sticky match runs in offset space. The absolute-rect match this
 * replaces de-pinned on any anchor motion past a fraction of a pixel, handing
 * the pick back to the preference order at every resolve — a walking entity
 * re-argued its slot five times a second and visibly hopped.
 */
class MovingAnchorStickyTest {

    private static final int W = 1280;
    private static final int H = 720;
    private static final double dt = 1.0 / 60.0;

    /**
     * A sticky follow-style element: an anchor-docked candidate lattice with
     * integer-pixel rects (right / left / centered, the zone lattice's
     * rounding), so fractional anchor motion perturbs both the absolute rects
     * and — by at most one pixel — the offsets.
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
            // the lattice rounding: candidate rects snap to integer pixels as
            // the (fractional) anchor drifts, exactly like the zone lattice
            double centerX = anchor.x() + clearanceX;
            double centerY = anchor.y() + clearanceY;
            int x = (int) Math.round(centerX - size.width() * 0.5);
            int y = (int) Math.round(centerY - size.height() * 0.5);
            return new FloatRect(x, y, size.width(), size.height());
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            List<PlacementCandidate> candidates = new ArrayList<>(3);
            candidates.add(PlacementCandidate.screen(docked(size.width() * 0.5 + 8, 0))); // right — the preferred
            candidates.add(PlacementCandidate.screen(docked(-(size.width() * 0.5 + 8), 0))); // left
            candidates.add(PlacementCandidate.screen(docked(0, 0))); // centered
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
    void walkingAnchorKeepsItsGrantedSlotAcrossResolves() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        LatticeElement element = new LatticeElement("e", 300, 200);
        coordinator.register(element);

        // the preferred right slot is excluded at birth — the grant lands left
        Rect overRight = new Rect(300, 120, 240, 160);
        CoordinationResult first = frame(coordinator, 0, overRight);
        FloatRect granted = first.placementOf("e").offsetRect();
        assertTrue(granted.x() < 0, "granted the left slot: " + granted);
        double grantedX = granted.x();

        // the anchor walks with fractional per-frame drift (a projected
        // entity: a few px per frame, a resolve every handful of frames) —
        // the exclusion is gone, so the preference would take the right slot
        // again if the sticky match de-pinned
        double now = dt;
        for (int f = 0; f < 90; f++) {
            element.moveTo(300 + f * 3.7, 200 + f * 1.9);
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            FloatRect offset = result.placementOf("e").offsetRect();
            assertEquals(
                grantedX,
                offset.x(),
                2.0,
                "frame " + f + ": the granted slot de-pinned — offset " + offset + " vs granted " + granted
            );
            // and the slot still rides the anchor: the screen rect follows it
            FloatPos anchor = result.placementOf("e").anchor();
            FloatRect screen = result.placementOf("e").screenRect();
            assertEquals(anchor.x() + grantedX, screen.x(), 2.0, "frame " + f + ": rides the anchor");
        }
    }

    @Test
    void clampedSlotRePinsOnceAndStaysStable() {
        // stability is not a freeze: walk the anchor toward the left screen
        // edge — the granted left slot clamps, drifts off its lattice offset,
        // and the element re-pins exactly once to a slot that fits (the right
        // one); from then on the new slot is held. What must never happen is
        // flapping between slots across consecutive resolves.
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        LatticeElement element = new LatticeElement("e", 300, 200);
        coordinator.register(element);

        Rect overRight = new Rect(300, 120, 240, 160);
        CoordinationResult first = frame(coordinator, 0, overRight);
        assertTrue(first.placementOf("e").offsetRect().x() < 0, "granted the left slot");

        double now = dt;
        double lastRectX = first.placementOf("e").screenRect().x();
        double lastOffsetX = first.placementOf("e").offsetRect().x();
        int slotHops = 0;
        int hopFrame = -1;
        // stop while the anchor is still on-screen: past the edge the slot
        // clamps by design (the driving adapter clamps every frame anyway)
        for (int f = 0; f < 66; f++) {
            element.moveTo(300 - f * 4.1, 200);
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            InworldPlacement placement = result.placementOf("e");
            double rectX = placement.screenRect().x();
            // clamping re-applies at resolves; between them the retained
            // offset may drift at most the anchor-displacement threshold
            assertTrue(
                rectX >= -13.0 && rectX <= W,
                "frame " + f + ": rect stays in the work area (resolve clamped): " + rectX
            );
            if (rectX - lastRectX > 13.5) {
                slotHops++;
                hopFrame = f;
            }
            lastRectX = rectX;
            lastOffsetX = placement.offsetRect().x();
        }
        assertTrue(slotHops == 1, "exactly one re-pin, saw " + slotHops + " (hop at " + hopFrame + ")");
        // after the hop the new slot holds: the offset stays on one lattice value
        // (still on-screen — past the edge the clamp owns the offset again)
        double settled = lastOffsetX;
        for (int f = 66; f < 74; f++) {
            element.moveTo(300 - f * 4.1, 200);
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            assertTrue(
                Math.abs(result.placementOf("e").offsetRect().x() - settled) <= 2.0,
                "frame " + f + ": the re-pinned slot holds"
            );
        }
    }
}
