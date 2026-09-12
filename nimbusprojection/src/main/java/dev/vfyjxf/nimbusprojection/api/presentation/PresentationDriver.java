package dev.vfyjxf.nimbusprojection.api.presentation;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The solver behind one {@link Presentation} type — the open SPI that
 * makes presentation modes extensible.
 * <p>
 * A driver answers three questions per frame: where the panel's surface
 * sits ({@link #resolve}), how a screen point / view ray maps onto it
 * ({@link #pick}), and what it becomes under inspect flattening
 * ({@link #flatten}). Occlusion avoidance, chrome collision and focus
 * participation are the runtime's uniform pipeline — drivers only report
 * geometry, they never reinvent layout.
 * <p>
 * The built-in presentations (face/floating/follow/dock/expand/inspect)
 * are implemented as ordinary drivers over this same SPI — the extension
 * point is proven by the builtins, not bolted on. Register with
 * {@code NimbusClient.registerPresentation(driver)}.
 */
public interface PresentationDriver<P extends Presentation> {

    /** The {@link Presentation#type()} id this driver solves. */
    ResourceLocation presentationId();

    /**
     * Computes where the panel's surface sits this frame — a world-space
     * quad or a screen-space rect.
     */
    PanelGeometry resolve(SolveContext<P> ctx);

    /**
     * Maps the current pointer (view ray in world mode, screen point in
     * inspect mode) onto the surface, returning surface-local uv in gui
     * pixels, or null when it misses.
     */
    @Nullable
    FloatPos pick(PickContext<P> ctx);

    /**
     * What this presentation becomes under inspect flattening. Default:
     * a natural-sized rect beside the anchor's projected point.
     */
    default FlattenedGeometry flatten(FlattenContext<P> ctx) {
        return FlattenedGeometry.beside(ctx.anchorScreen(), ctx.naturalSize());
    }

    /** How this presentation participates in inspect mode. */
    default InspectPolicy inspectPolicy() {
        return InspectPolicy.flatten;
    }
}
