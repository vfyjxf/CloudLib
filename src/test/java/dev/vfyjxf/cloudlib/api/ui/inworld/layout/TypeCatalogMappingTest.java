package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.GroupLayoutEngine;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.InworldGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.OrbitAroundAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.StackInColumn;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 22-class inworld type catalog (plan §2.5), class by class: every type
 * gets at least one profile-or-declaration combination that constructs
 * (compile-time proof), passes facet validation (construction-time proof)
 * and drives a full coordinator frame (pipeline proof). The full mapping
 * table with notes lives in {@code docs/inworld-type-catalog-mapping.md}.
 */
class TypeCatalogMappingTest {

    /** Asserts the spec drives: presented, granted, strongest rung. */
    private static InworldPlacement drives(ElementSpec spec) {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(spec);
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);
        InworldPlacement placement = result.placementOf(spec.id());
        assertNotNull(placement, spec.id() + " must be presentable");
        assertEquals(0, placement.variant().level(), spec.id() + " grants its strongest rung in an empty scene");
        assertTrue(placement.screenRect().width() > 0);
        return placement;
    }

    // region 源追踪·实体 (entity-anchored)

    @Test
    void t01HealthBarNameplate() {
        drives(ElementSpec.from(InworldProfile.nameplate, "t1").withAnchor(AnchorFacet.entity("cow-1", 0.8)));
    }

    @Test
    void t02DamageNumber() {
        // Transient TTL channel: ghost policy (mutual non-interference) plus
        // a quick-fade ladder; the driver retracts when the TTL lapses.
        drives(ElementSpec.from(InworldProfile.transientUi, "t2").withAnchor(AnchorFacet.entity("cow-1", 1.2)));
    }

    @Test
    void t03StatusIconRow() {
        ElementSpec spec = ElementSpec.from(InworldProfile.orbit, "t3")
                .withAnchor(AnchorFacet.entity("cow-1", 1.0))
                .withGroup(GroupFacet.of(new InworldGroup("nimbus", "status", "cow-1"), OrbitAroundAnchor.of()));
        drives(spec);

        // The group side: four icons take ring slots around the anchor.
        GroupLayoutEngine engine =
                GroupLayoutEngine.of(new InworldGroup("nimbus", "status", "cow-1"), OrbitAroundAnchor.of());
        List<GroupLayoutEngine.GroupMember> icons = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            icons.add(new GroupLayoutEngine.GroupMember("icon-" + i, 200, 150, 24, 24));
        }
        GroupLayoutEngine.GroupResult arranged =
                engine.arrange(new GroupLayoutEngine.GroupFrame(200, 150, icons, List.of()));
        assertEquals(4, arranged.clusters().size());
        assertEquals(0, arranged.aggregatedCount());
    }

    @Test
    void t04EntityFollowPanel() {
        // The occluded→screen-sidebar transfer is bespoke: the escape hatch.
        SidebarTransferLayouter layouter = new SidebarTransferLayouter();
        ElementSpec spec = ElementSpec.from(InworldProfile.follow, "t4")
                .withAnchor(AnchorFacet.entity("player-1", 0.5))
                .custom(layouter);
        drives(spec);
        assertTrue(layouter.proposeCount >= 1, "the custom layouter drove the element side");
    }

    @Test
    void t05VehicleRiders() {
        ElementSpec spec = ElementSpec.from(InworldProfile.orbit, "t5")
                .withAnchor(AnchorFacet.entity("minecart-1", 0.6))
                .withGroup(GroupFacet.of(new InworldGroup("nimbus", "riders", "minecart-1"), OrbitAroundAnchor.of()));
        drives(spec);

        // Multi-rider aggregation: three riders ring the vehicle.
        GroupLayoutEngine engine =
                GroupLayoutEngine.of(new InworldGroup("nimbus", "riders", "minecart-1"), OrbitAroundAnchor.of());
        List<GroupLayoutEngine.GroupMember> riders = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            riders.add(new GroupLayoutEngine.GroupMember("rider-" + i, 200, 150, 28, 28));
        }
        GroupLayoutEngine.GroupResult arranged =
                engine.arrange(new GroupLayoutEngine.GroupFrame(200, 150, riders, List.of()));
        assertEquals(3, arranged.clusters().size());
    }

    @Test
    void t06ChatBubble() {
        ElementSpec spec = ElementSpec.from(InworldProfile.transientUi, "t6")
                .withAnchor(AnchorFacet.entity("villager-1", 2.0))
                .withGroup(GroupFacet.of(new InworldGroup("nimbus", "bubbles", "villager-1"), StackInColumn.of()));
        drives(spec);

        // The stack: bubbles pile up above the anchor with the "+N" overflow.
        GroupLayoutEngine engine =
                GroupLayoutEngine.of(new InworldGroup("nimbus", "bubbles", "villager-1"), StackInColumn.of());
        List<GroupLayoutEngine.GroupMember> bubbles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            bubbles.add(new GroupLayoutEngine.GroupMember("b-" + i, 200, 150, 20, 20));
        }
        GroupLayoutEngine.GroupResult arranged =
                engine.arrange(new GroupLayoutEngine.GroupFrame(200, 150, bubbles, List.of()));
        assertEquals(0.0, arranged.of("b-0").offsetY(), 1.0e-9);
        assertEquals(-24.0, arranged.of("b-1").offsetY(), 1.0e-9);
        assertEquals(-48.0, arranged.of("b-2").offsetY(), 1.0e-9);
    }

    // endregion

    // region 源追踪·方块 (block-anchored)

    @Test
    void t07FacePanel() {
        drives(ElementSpec.from(InworldProfile.facePanel, "t7")
                .withAnchor(new AnchorFacet.BlockFace(10, 64, -5, AnchorFacet.Normal.north, 6.0)));
    }

    @Test
    void t08Hologram() {
        drives(ElementSpec.from(InworldProfile.hologram, "t8").withAnchor(AnchorFacet.position(10.5, 66, -4.5)));
    }

    @Test
    void t09FurnaceProgress() {
        // The mini bar degrades to a corner icon (iconOnly rung).
        drives(ElementSpec.from(InworldProfile.facePanel, "t9")
                .withAnchor(AnchorFacet.blockFace(10, 65, -5, AnchorFacet.Normal.up))
                .withDegrade(DegradeFacet.fixed(new Size(48, 12), new Size(20, 20))));
    }

    @Test
    void t10AreaMarker() {
        // Wireframe + corner labels: fixed posture, no rect avoidance.
        drives(ElementSpec.from(InworldProfile.ground, "t10").withAnchor(AnchorFacet.position(12, 64, -3)));
    }

    @Test
    void t11GroundProjection() {
        drives(ElementSpec.from(InworldProfile.ground, "t11").withAnchor(AnchorFacet.position(12.5, 64, -2.5)));
    }

    // endregion

    // region 摄像头/世界混合

    @Test
    void t12SelectionHighlight() {
        // Pure render layer: ghost, indicator-masked, never arbitrates space.
        drives(ElementSpec.from(InworldProfile.transientUi, "t12")
                .withAnchor(AnchorFacet.entity("cow-1"))
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.indicator), SpacePolicy.ghost, 0)));
    }

    @Test
    void t13LeaderLines() {
        // Adaptive s→po→hyperleader routing is bound by the nameplate profile.
        drives(ElementSpec.from(InworldProfile.nameplate, "t13").withAnchor(AnchorFacet.position(30, 70, -10)));
    }

    @Test
    void t14WaypointInScreen() {
        drives(ElementSpec.from(InworldProfile.waypoint, "t14").withAnchor(AnchorFacet.position(100, 80, 100)));
    }

    @Test
    void t15WaypointOffscreen() {
        InworldPlacement placement =
                drives(ElementSpec.from(InworldProfile.waypoint, "t15").withAnchor(AnchorFacet.position(-40, 70, -60)));
        // The ANGLE-encoder end of the ladder: direction cues live at the edge.
        assertTrue(placement.screenRect().x() >= 0.0);
    }

    @Test
    void t16Ping() {
        ElementSpec spec = ElementSpec.from(InworldProfile.transientUi, "t16")
                .withAnchor(AnchorFacet.position(50, 65, 50))
                .withGroup(GroupFacet.of(new InworldGroup("nimbus", "pings", "channel-a"), OrbitAroundAnchor.of()));
        drives(spec);

        // Multiple pings fan out around the marked spot.
        GroupLayoutEngine engine =
                GroupLayoutEngine.of(new InworldGroup("nimbus", "pings", "channel-a"), OrbitAroundAnchor.of());
        List<GroupLayoutEngine.GroupMember> pings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            pings.add(new GroupLayoutEngine.GroupMember("ping-" + i, 200, 150, 16, 16));
        }
        GroupLayoutEngine.GroupResult arranged =
                engine.arrange(new GroupLayoutEngine.GroupFrame(200, 150, pings, List.of()));
        assertEquals(3, arranged.clusters().size());
    }

    // endregion

    // region 屏幕空间

    @Test
    void t17DamageDirectionIndicator() {
        // Screen-center ring arc: pure overlay, exempt from avoidance.
        drives(ElementSpec.from(InworldProfile.transientUi, "t17")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.indicator), SpacePolicy.ghost, 0))
                .withAnchor(AnchorFacet.cameraTracked(0.5, 0.5)));
    }

    @Test
    void t18CompassStrip() {
        InworldPlacement placement = drives(ElementSpec.from(InworldProfile.dock, "t18")
                .withAnchor(AnchorFacet.cameraTracked(0.5, 0.03))
                .withDegrade(DegradeFacet.of(
                        SpacePolicy.passive, false, true, new Size(200, 18), new Size(120, 16), new Size(60, 14))));
        assertEquals(8.0, placement.screenRect().y(), 0.01, "the top-edge scan starts at the margin");
    }

    @Test
    void t19InteractionHint() {
        drives(ElementSpec.from(InworldProfile.excentric, "t19").withAnchor(AnchorFacet.position(60, 66, -20)));
    }

    @Test
    void t20RadialMenu() {
        // Modal: fixed, high priority, screen-center anchored.
        drives(ElementSpec.from(InworldProfile.transientUi, "t20")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.fixed, 20))
                .withDegrade(DegradeFacet.fixed(new Size(200, 200)))
                .withAnchor(AnchorFacet.cameraTracked(0.5, 0.5)));
    }

    @Test
    void t21InspectLayer() {
        drives(ElementSpec.from(InworldProfile.transientUi, "t21")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel, SpaceMask.hudOverlay), SpacePolicy.fixed, 30))
                .withDegrade(DegradeFacet.fixed(new Size(320, 240)))
                .withAnchor(AnchorFacet.cameraTracked(0.5, 0.5)));
    }

    @Test
    void t22MinimapDock() {
        InworldPlacement placement = drives(ElementSpec.from(InworldProfile.dock, "t22"));
        assertEquals(292.0, placement.screenRect().x(), 0.01, "the corner dock cursor");
        assertEquals(8.0, placement.screenRect().y(), 0.01);
    }

    // endregion

    /** The T4 bespoke bit: anchored normally, sidebar under occlusion. */
    static final class SidebarTransferLayouter implements InworldLayouter {

        int proposeCount;
        boolean occluded;

        @Override
        public SpaceReservation reserve(InworldLayoutContext ctx) {
            return new SpaceReservation(SpaceKind.world, 8, true, ElementMode.arbitrated);
        }

        @Override
        public ElementProposal propose(InworldLayoutContext ctx) {
            proposeCount++;
            Size variantSize = ctx.variant().requestedSize();
            FloatPos anchor = occluded
                    ? new FloatPos(16, 150)
                    : ctx.environment().anchor().screen();
            return ElementProposal.of(
                    ctx.variant(),
                    anchor,
                    PlacementCandidate.screen(FloatRect.around(anchor, variantSize.width(), variantSize.height())));
        }

        @Override
        public void arbitrated(@Nullable InworldPlacement placement, Feedback feedback) {}
    }
}
