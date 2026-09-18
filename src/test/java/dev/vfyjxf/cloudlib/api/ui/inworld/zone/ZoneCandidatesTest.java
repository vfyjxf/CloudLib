package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneCandidatesTest {

    private static final Rect screen = new Rect(0, 0, 480, 270);
    private static final Size panel = new Size(60, 24);
    private static final FloatPos anchor = new FloatPos(240, 135);
    private static final ZoneCandidates.Config config = ZoneCandidates.Config.defaults();

    private static List<ZoneCandidates.Candidate> generate(FloatPos at) {
        return ZoneCandidates.generate(at, panel, screen, config);
    }

    private static Optional<ZoneCandidates.Candidate> find(
            List<ZoneCandidates.Candidate> candidates, ZoneCandidates.Direction direction, ZoneCandidates.Tier tier) {
        return candidates.stream()
                .filter(c -> c.direction() == direction && c.tier() == tier)
                .findFirst();
    }

    @Test
    void generatesTheFullLatticeInARoomyScreen() {
        List<ZoneCandidates.Candidate> candidates = generate(anchor);

        assertEquals(25, candidates.size());
        Set<Rect> rects = new HashSet<>();
        for (ZoneCandidates.Candidate candidate : candidates) {
            assertTrue(rects.add(candidate.rect()), "duplicate rect: " + candidate.rect());
            assertTrue(screen.contains(candidate.rect()), "outside safeRect: " + candidate.rect());
        }
        // the in-place candidate is first
        assertEquals(ZoneCandidates.Tier.anchor, candidates.get(0).tier());
        assertEquals(new Rect(210, 123, 60, 24), candidates.get(0).rect());
    }

    @Test
    void cardinalDirectionsDockByAlignmentSemantics() {
        List<ZoneCandidates.Candidate> candidates = generate(anchor);

        // top: bottom edge `near` px above the anchor, horizontally centered
        assertEquals(
                new Rect(210, 103, 60, 24),
                find(candidates, ZoneCandidates.Direction.top, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        assertEquals(
                127,
                find(candidates, ZoneCandidates.Direction.top, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect()
                        .bottom());
        // bottom: top edge `near` px below the anchor
        assertEquals(
                new Rect(210, 143, 60, 24),
                find(candidates, ZoneCandidates.Direction.bottom, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        // left: right edge `near` px left of the anchor, vertically centered
        assertEquals(
                new Rect(172, 123, 60, 24),
                find(candidates, ZoneCandidates.Direction.left, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        // right: left edge `near` px right of the anchor
        assertEquals(
                new Rect(248, 123, 60, 24),
                find(candidates, ZoneCandidates.Direction.right, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
    }

    @Test
    void diagonalDirectionsOffsetBothAxes() {
        List<ZoneCandidates.Candidate> candidates = generate(anchor);

        assertEquals(
                new Rect(172, 103, 60, 24),
                find(candidates, ZoneCandidates.Direction.topLeft, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        assertEquals(
                new Rect(248, 103, 60, 24),
                find(candidates, ZoneCandidates.Direction.topRight, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        assertEquals(
                new Rect(172, 143, 60, 24),
                find(candidates, ZoneCandidates.Direction.bottomLeft, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        assertEquals(
                new Rect(300, 195, 60, 24),
                find(candidates, ZoneCandidates.Direction.bottomRight, ZoneCandidates.Tier.far)
                        .orElseThrow()
                        .rect());
    }

    @Test
    void distanceTiersSpreadAlongTheDirection() {
        List<ZoneCandidates.Candidate> candidates = generate(anchor);

        assertEquals(
                new Rect(210, 103, 60, 24),
                find(candidates, ZoneCandidates.Direction.top, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        assertEquals(
                new Rect(210, 83, 60, 24),
                find(candidates, ZoneCandidates.Direction.top, ZoneCandidates.Tier.medium)
                        .orElseThrow()
                        .rect());
        assertEquals(
                new Rect(210, 51, 60, 24),
                find(candidates, ZoneCandidates.Direction.top, ZoneCandidates.Tier.far)
                        .orElseThrow()
                        .rect());
    }

    @Test
    void outOfBoundsCandidatesAreClampedToTheNearestFeasibleSpot() {
        List<ZoneCandidates.Candidate> candidates = generate(new FloatPos(470, 130));

        assertFalse(candidates.isEmpty());
        for (ZoneCandidates.Candidate candidate : candidates) {
            assertTrue(screen.contains(candidate.rect()), "outside safeRect: " + candidate.rect());
        }
        // top-near keeps its vertical alignment and clamps x flush against the right edge
        assertEquals(
                new Rect(420, 98, 60, 24),
                find(candidates, ZoneCandidates.Direction.top, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        // right-near (x=478), the in-place candidate (x=440) and every right tier
        // clamp onto the same spot — dedup keeps exactly one
        long atClampedSpot = candidates.stream()
                .filter(c -> c.rect().equals(new Rect(420, 118, 60, 24)))
                .count();
        assertEquals(1, atClampedSpot);
        // far fewer than 25 survive: many directions collapse onto the right edge
        assertTrue(candidates.size() < 25, "expected dedup near the edge, got " + candidates.size());
    }

    @Test
    void clampingUsesTheSafeRectNotTheScreen() {
        Rect safe = new Rect(40, 30, 400, 210);
        List<ZoneCandidates.Candidate> candidates =
                ZoneCandidates.generate(new FloatPos(120, 135), panel, safe, config);

        for (ZoneCandidates.Candidate candidate : candidates) {
            assertTrue(safe.contains(candidate.rect()), "outside safeRect: " + candidate.rect());
        }
        // left-near fits unclamped...
        assertEquals(
                new Rect(52, 123, 60, 24),
                find(candidates, ZoneCandidates.Direction.left, ZoneCandidates.Tier.near)
                        .orElseThrow()
                        .rect());
        // ...left-medium would sit at x=32 and clamps flush against the safe
        // rect's left edge (x=40), not the screen's (x=0)
        assertEquals(
                new Rect(40, 123, 60, 24),
                find(candidates, ZoneCandidates.Direction.left, ZoneCandidates.Tier.medium)
                        .orElseThrow()
                        .rect());
    }

    @Test
    void cornerAnchorDeduplicatesTheCollapsedCandidates() {
        List<ZoneCandidates.Candidate> candidates = generate(new FloatPos(2, 2));

        assertFalse(candidates.isEmpty());
        Set<Rect> rects = new HashSet<>();
        for (ZoneCandidates.Candidate candidate : candidates) {
            assertTrue(rects.add(candidate.rect()), "duplicate rect: " + candidate.rect());
            assertTrue(screen.contains(candidate.rect()), "outside safeRect: " + candidate.rect());
        }
        assertTrue(candidates.size() < 25, "expected heavy collapse at the corner, got " + candidates.size());
        // the in-place candidate clamps into the very corner, and every
        // corner-docked direction collapses onto that same rect (deduped)
        assertEquals(ZoneCandidates.Tier.anchor, candidates.get(0).tier());
        assertEquals(new Rect(0, 0, 60, 24), candidates.get(0).rect());
    }

    @Test
    void offScreenAnchorsStillYieldClampedCandidates() {
        List<ZoneCandidates.Candidate> candidates = generate(new FloatPos(600, 135));

        assertFalse(candidates.isEmpty());
        for (ZoneCandidates.Candidate candidate : candidates) {
            assertTrue(screen.contains(candidate.rect()), "outside safeRect: " + candidate.rect());
        }
    }

    @Test
    void dropsEveryCandidateWhenThePanelCannotFit() {
        assertTrue(ZoneCandidates.generate(anchor, new Size(500, 24), screen, config)
                .isEmpty());
        assertTrue(ZoneCandidates.generate(anchor, new Size(60, 280), screen, config)
                .isEmpty());
        // a panel that fits nowhere yields nothing, not an exception
        assertTrue(ZoneCandidates.generate(anchor, new Size(481, 271), screen, config)
                .isEmpty());
    }

    @Test
    void canonicalOrderIsAnchorFirstThenDirectionsNearToFar() {
        List<ZoneCandidates.Candidate> candidates = generate(anchor);

        assertEquals(ZoneCandidates.Tier.anchor, candidates.get(0).tier());
        int index = 1;
        for (ZoneCandidates.Direction direction : ZoneCandidates.Direction.values()) {
            for (ZoneCandidates.Tier tier : new ZoneCandidates.Tier[] {
                ZoneCandidates.Tier.near, ZoneCandidates.Tier.medium, ZoneCandidates.Tier.far
            }) {
                ZoneCandidates.Candidate candidate = candidates.get(index++);
                assertEquals(direction, candidate.direction());
                assertEquals(tier, candidate.tier());
            }
        }
        assertEquals(candidates.size(), index);
    }

    @Test
    void defaultConfigMatchesTheSpecifiedClearances() {
        assertEquals(new ZoneCandidates.Config(8.0, 28.0, 60.0), ZoneCandidates.Config.defaults());
        // the default overload generates the same lattice as the explicit one
        assertEquals(ZoneCandidates.generate(anchor, panel, screen), generate(anchor));
    }

    @Test
    void candidateRecordGuardsTheAnchorTierInvariant() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ZoneCandidates.Candidate(null, ZoneCandidates.Tier.near, new Rect(0, 0, 10, 10)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ZoneCandidates.Candidate(
                        ZoneCandidates.Direction.top, ZoneCandidates.Tier.anchor, new Rect(0, 0, 10, 10)));
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> ZoneCandidates.Config.of(28.0, 28.0, 60.0));
        assertThrows(IllegalArgumentException.class, () -> ZoneCandidates.Config.of(8.0, 60.0, 28.0));
        assertThrows(IllegalArgumentException.class, () -> ZoneCandidates.Config.of(0.0, 28.0, 60.0));
        assertThrows(IllegalArgumentException.class, () -> ZoneCandidates.Config.of(8.0, 28.0, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> ZoneCandidates.generate(new FloatPos(Double.NaN, 0), panel, screen, config));
        assertThrows(
                IllegalArgumentException.class, () -> ZoneCandidates.generate(anchor, new Size(0, 24), screen, config));
        assertThrows(
                IllegalArgumentException.class,
                () -> ZoneCandidates.generate(anchor, new Size(60, -1), screen, config));
    }
}
