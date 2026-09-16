package dev.vfyjxf.cloudlib.api.ui.inworld.trace;

import dev.vfyjxf.cloudlib.api.ui.inworld.render.RenderBatch;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.RenderTarget;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.RenderTarget.ScreenTarget;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.RenderTarget.WorldTarget;

/**
 * The drawing context of one trace render pass — a scene is organized by
 * <em>where</em> the pass draws (the world pass inside the level render, or
 * the screen pass with the gui), never by where a trace comes from.
 * <p>
 * A scene pairs the pass's {@link RenderTarget} (camera/projection/viewport
 * for the world pass, {@code GuiGraphics} for the screen pass) with the
 * {@link RenderBatch} trace geometry accumulates into; the pass owner flushes
 * the batch once so the whole pass draws in a single go.
 */
public sealed interface TraceScene {

    /** The render target of the pass this scene belongs to. */
    RenderTarget target();

    /** The batch trace geometry accumulates into during this pass. */
    RenderBatch<?> batch();

    /** The world pass: trace geometry in world space, drawn inside the level render. */
    record WorldTraceScene(WorldTarget target, RenderBatch<?> batch) implements TraceScene {}

    /** The screen pass: trace geometry in gui-scaled pixels, drawn with the gui. */
    record ScreenTraceScene(ScreenTarget target, RenderBatch<?> batch) implements TraceScene {}
}
