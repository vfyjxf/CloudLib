package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Drives assembled elements through a real {@link InworldCoordinator} the
 * way an adapter would: beginFrame → coordinator frame → observe. Purely
 * headless.
 */
final class LayoutHarness {

    static final int width = 400;
    static final int height = 300;
    static final double dt = 1.0 / 60.0;

    private final InworldCoordinator coordinator = InworldCoordinator.withDefaults();
    private final PipelineAssembler assembler = PipelineAssembler.create();
    private double now = -dt;

    AssembledElement assemble(ElementSpec spec) {
        return assembler.assemble(spec);
    }

    void register(AssembledElement element) {
        coordinator.register(element);
    }

    /** One frame with the given environment and matching exclusions. */
    CoordinationResult frame(LayoutEnvironment environment, AssembledElement... elements) {
        return frame(environment, environment.exclusionRects(), elements);
    }

    /** One frame with the given environment and coordinator-side exclusions. */
    CoordinationResult frame(LayoutEnvironment environment, List<Rect> exclusions, AssembledElement... elements) {
        for (AssembledElement element : elements) {
            element.beginFrame(environment);
        }
        double current = now + dt;
        now = current;
        CoordinationResult result = coordinator
                .frame(InworldCoordinator.FrameInput.of(width, height, current, dt, exclusions));
        for (AssembledElement element : elements) {
            element.observe(result);
        }
        return result;
    }

    /** An environment anchored at the given screen position. */
    static LayoutEnvironment anchored(@Nullable FloatPos anchor) {
        LayoutEnvironment environment = LayoutEnvironment.of(width, height).at(0.0, dt);
        return anchor == null ? environment : environment.withAnchor(AnchorFrame.screen(anchor));
    }

    /** An empty environment (no anchor) at the harness clock. */
    static LayoutEnvironment unanchored() {
        return anchored(null);
    }

    /** One frame without touching any element (no beginFrame — contract testing). */
    CoordinationResult frameRaw() {
        double current = now + dt;
        now = current;
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, current, dt));
    }

    static FloatPos pos(double x, double y) {
        return new FloatPos(x, y);
    }
}
