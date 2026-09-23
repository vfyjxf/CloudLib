package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Assembler and assembled-element wiring: environments, stages, dispatch. */
class PipelineAssemblerTest {

    @Test
    void beginFrameIsRequiredBeforeTheCoordinatorDrives() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.nameplate, "np"));
        harness.register(element);
        assertThrows(IllegalStateException.class, harness::frameRaw);
        assertEquals("np", element.id());
    }

    @Test
    void specAndStrategiesAreBoundAtAssembly() {
        PipelineAssembler assembler = PipelineAssembler.create();
        ElementSpec spec = ElementSpec.from(InworldProfile.facePanel, "fp");
        AssembledElement element = assembler.assemble(spec);
        assertSame(spec, element.spec());
        assertEquals(SpaceKind.world, element.spaceKind());
        assertEquals(5, element.priority());
        assertTrue(element.sticky());
        assertEquals(ElementMode.arbitrated, element.mode());
        assertEquals(4, element.ladder().size());
    }

    @Test
    void screenAnchorKindsMapToTrackedAndPanel() {
        PipelineAssembler assembler = PipelineAssembler.create();
        assertEquals(SpaceKind.tracked, assembler.assemble(ElementSpec.from(InworldProfile.dock, "d")).spaceKind());
        assertEquals(
            SpaceKind.panel,
            assembler.assemble(ElementSpec.from(InworldProfile.dock, "p").withAnchor(AnchorFacet.none())).spaceKind()
        );
    }

    @Test
    void cameraTrackedAnchorResolvesWithoutAnEnvironmentAnchor() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.dock, "dock"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.unanchored(), element);
        assertEquals(388.0, Objects.requireNonNull(result.placementOf("dock")).anchor().x(), 0.01);
    }

    @Test
    void anchorlessEnvironmentRetractsWorldElements() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.nameplate, "np"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.unanchored(), element);
        assertNull(result.placementOf("np"));
        assertEquals(VisibilityTracker.Phase.hidden, Objects.requireNonNull(result.elementState("np")).phase());
    }
}
