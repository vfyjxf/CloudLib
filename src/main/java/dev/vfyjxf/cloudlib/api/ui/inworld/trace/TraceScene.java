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
 *
 * @param <E> the emitter/geometry-sink type the pass's batch accumulates into
 */
public sealed interface TraceScene<E> {

    /** The render target of the pass this scene belongs to. */
    RenderTarget target();

    /** The batch trace geometry accumulates into during this pass. */
    RenderBatch<E> batch();

    /** The world pass: trace geometry in world space, drawn inside the level render. */
    record WorldTraceScene<E>(WorldTarget target, RenderBatch<E> batch) implements TraceScene<E> {}

    /** The screen pass: trace geometry in gui-scaled pixels, drawn with the gui. */
    record ScreenTraceScene<E>(ScreenTarget target, RenderBatch<E> batch) implements TraceScene<E> {}
}
