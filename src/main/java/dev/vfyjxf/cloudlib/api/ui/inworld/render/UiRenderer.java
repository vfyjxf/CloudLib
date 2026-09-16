package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.jetbrains.annotations.Nullable;

/**
 * A renderer for one kind of in-world UI content, bound to a {@link RenderTarget}
 * per frame and drawing through a {@link RenderBatch} — the "draw each renderer's
 * output as one batch" abstraction.
 * <p>
 * Frame lifecycle: {@link #begin(RenderTarget)} binds the target and opens the
 * frame (resetting the batch and preparing pipeline state); content is emitted
 * into {@link #batch()}'s emitter; {@link #flush()} draws the accumulated
 * geometry in a single pass and closes the frame.
 *
 * @param <R> the render target variant this renderer draws into
 */
public interface UiRenderer<R extends RenderTarget> {

    /** The target of the current frame — non-null between {@link #begin} and {@link #flush}. */
    @Nullable
    R target();

    /** The batch this renderer accumulates the frame's geometry into. */
    RenderBatch<?> batch();

    /**
     * Binds {@code target} and opens a new frame: the batch is reset and any
     * pipeline state the renderer needs is prepared.
     */
    void begin(R target);

    /** Draws the accumulated batch in one pass and closes the frame. */
    void flush();
}
