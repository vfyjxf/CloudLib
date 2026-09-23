package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCandidates;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The A/B equivalence acceptance test (Z2): adding zone-declaring elements
 * to a population must not change any zone-less element's outcome — per
 * frame, field by field — and an all-zone-less population must be exactly
 * the pre-zone behavior (the existing deterministic replay suite pins that;
 * this test re-drives one through the Z2 code path).
 * <p>
 * The mixed population is constructed so the zone panels arbitrate after
 * the zone-less world elements (tracked kind after world kind, registered
 * last) and in screen regions the world elements never visit — a zone
 * panel's grant then cannot feed back into the world elements' arbitration,
 * which is precisely the interference this test stands guard against: any
 * leak (a stray renegotiation cause, an occupancy mark read too early, a
 * budget flap) shows up as a field-by-field diff.
 */
class ZoneAbEquivalenceTest {

    private static final int width = 480;
    private static final int height = 300;
    private static final double dt = 1.0 / 60.0;
    private static final int frames = 16; // spans the first epoch re-resolve

    private static final String[] plainIds = {"np1", "np2", "np3"};
    private static final FloatPos[] plainAnchors = {new FloatPos(120, 80), new FloatPos(300, 90),
            new FloatPos(210, 170)};
    private static final String[] zoneIds = {"z1", "z2"};

    /** One coordinator plus its assembled population, driven frame by frame. */
    private static final class Run {

        final InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        final PipelineAssembler assembler = PipelineAssembler.create();
        final AssembledElement[] plain = new AssembledElement[plainIds.length];
        final AssembledElement[] zone = new AssembledElement[zoneIds.length];
        double now = 0.0;

        Run(boolean withZone) {
            for (int i = 0; i < plainIds.length; i++) {
                plain[i] = assembler.assemble(plainSpec(plainIds[i]));
                coordinator.register(plain[i]);
            }
            if (withZone) {
                for (int i = 0; i < zoneIds.length; i++) {
                    zone[i] = assembler.assemble(zoneSpec(zoneIds[i]));
                    coordinator.register(zone[i]);
                }
            }
        }

        CoordinationResult frame() {
            now += dt;
            // every element gets its own environment: the plain ones exactly
            // as before (no previous layout), the zone ones with the
            // coordinator's previous-frame snapshot attached
            for (int i = 0; i < plain.length; i++) {
                plain[i].beginFrame(
                    LayoutEnvironment.of(width, height).at(now, dt).withAnchor(AnchorFrame.screen(plainAnchors[i]))
                );
            }
            for (int i = 0; i < zone.length && zone[i] != null; i++) {
                zone[i].beginFrame(
                    LayoutEnvironment.of(width, height).at(now, dt).withAnchor(AnchorFrame.screen(zoneAnchor(i)))
                            .withPreviousLayout(coordinator.previousZoneLayout().orElse(null))
                );
            }
            CoordinationResult result = coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
            for (int i = 0; i < plain.length; i++) {
                plain[i].observe(result);
            }
            for (int i = 0; i < zone.length && zone[i] != null; i++) {
                zone[i].observe(result);
            }
            return result;
        }
    }

    @Test
    void zoneElementsDoNotExistForTheZonelessPopulation() {
        Run baseline = new Run(false);
        Run mixed = new Run(true);

        for (int frame = 1; frame <= frames; frame++) {
            CoordinationResult a = baseline.frame();
            CoordinationResult b = mixed.frame();

            assertEquals(a.frame(), b.frame(), "frame " + frame);
            assertEquals(a.epoch(), b.epoch(), "epoch, frame " + frame);
            assertEquals(a.resolved(), b.resolved(), "resolved, frame " + frame);
            assertEquals(a.cause(), b.cause(), "renegotiation cause, frame " + frame);

            for (int i = 0; i < plainIds.length; i++) {
                String id = plainIds[i];
                assertEquals(a.placementOf(id), b.placementOf(id), "placement of " + id + ", frame " + frame);
                assertEquals(a.elementState(id), b.elementState(id), "element state of " + id + ", frame " + frame);
                assertNotNull(a.elementState(id), "the population is presented at all");
            }
        }
    }

    @Test
    void theZonePopulationIsReallyDrivenAndArbitrated() {
        Run mixed = new Run(true);
        CoordinationResult first = mixed.frame();

        // the zone panels took the zone path: their grants are lattice
        // members, they hold the screen, and the population is larger
        assertEquals(plainIds.length + zoneIds.length, first.placements().size());
        for (int i = 0; i < zoneIds.length; i++) {
            InworldPlacement grant = Objects.requireNonNull(first.placementOf(zoneIds[i]));
            Rect granted = Objects.requireNonNull(grant.screenRect()).toRect();
            List<Rect> lattice = latticeRects(zoneAnchor(i), granted.width(), granted.height());
            assertTrue(lattice.contains(granted), zoneIds[i] + "'s grant is a lattice candidate: " + granted);
        }

        // the coordinator maintains the previous-frame snapshot for this
        // population (and computes nothing for a zone-less one)
        assertTrue(mixed.coordinator.previousZoneLayout().isPresent());
        for (String id : plainIds) {
            assertTrue(
                mixed.coordinator.previousZoneLayout().orElseThrow().placements().containsKey(id),
                "the snapshot covers the whole committed layout, zone-less elements included: " + id
            );
        }
        for (String id : zoneIds) {
            assertTrue(mixed.coordinator.previousZoneLayout().orElseThrow().placements().containsKey(id));
        }

        Run baseline = new Run(false);
        baseline.frame();
        assertTrue(
            baseline.coordinator.previousZoneLayout().isEmpty(),
            "a population without zone consumers computes no snapshot"
        );
    }

    // region population

    /** A zone-less world nameplate at a static screen position. */
    private static ElementSpec plainSpec(String id) {
        return ElementSpec.from(InworldProfile.nameplate, id).withAnchor(AnchorFacet.position(0, 1, 0));
    }

    /** A tracked zone panel docked at one bottom corner. */
    private static ElementSpec zoneSpec(String id) {
        // spaces swap first (the cross-family re-anchoring convention), then
        // the anchor — the orientation coheres from blockFace to screen
        return ElementSpec.from(InworldProfile.facePanel, id)
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.passive, 1))
                .withDegrade(DegradeFacet.of(SpacePolicy.passive, false, true, new Size(60, 20), new Size(40, 16)))
                .withAnchor(AnchorFacet.cameraTracked(zoneIds[0].equals(id) ? 0.08 : 0.92, 0.85))
                .withZone(ZoneFacet.of());
    }

    private static FloatPos zoneAnchor(int index) {
        return new FloatPos(index == 0 ? 0.08 * width : 0.92 * width, 0.85 * height);
    }

    /** The lattice rects for the safe rect this test drives (screen-sized). */
    private static List<Rect> latticeRects(FloatPos anchor, int w, int h) {
        return ZoneCandidates.generate(anchor, new Size(w, h), new Rect(0, 0, width, height)).stream()
                .map(ZoneCandidates.Candidate::rect).toList();
    }

    // endregion
}
