package dev.vfyjxf.cloudlib.api.ui.inworld.render;

/**
 * A batching carrier for one renderer's geometry: everything emitted between
 * {@link #begin()} and {@link #flush()} accumulates, and {@code flush()} draws
 * it in a single pass — one shader/bind/draw for the whole frame instead of
 * one per element.
 * <p>
 * The contract fixes only the lifecycle; the accumulated geometry type
 * (vertices, quads, sprites, …) is the implementation's business and reaches
 * emitters through {@link #emitter()}.
 *
 * @param <E> the emitter/geometry-sink type emitters write into — defined by
 *            the implementation (e.g. a vertex buffer)
 */
public interface RenderBatch<E> {

    /**
     * Opens accumulation for a new frame, discarding anything left over from
     * the previous one.
     */
    void begin();

    /**
     * The sink this frame's geometry is emitted into. Valid between
     * {@link #begin()} and {@link #flush()}.
     */
    E emitter();

    /** Whether nothing has been emitted since {@link #begin()} — lets owners skip an empty flush. */
    boolean isEmpty();

    /**
     * Draws everything accumulated since {@link #begin()} in one pass and
     * closes the batch until the next {@code begin()}.
     */
    void flush();
}
