package dev.vfyjxf.cloudlib.api.ui.inworld.trace;

/**
 * Draws the trace chrome of one source into a {@link TraceScene}.
 * <p>
 * The contract hands over only the source and whatever context the
 * implementation asked for — what a trace looks like (lines, marks, icons,
 * nothing at all) is entirely the implementation's business. Implementations
 * emit their geometry into {@link TraceScene#batch()}; the pass owner flushes
 * the batch once per pass, so all tracers of a scene draw in a single go.
 *
 * @param <S> the scene variant this tracer draws into
 */
public interface Tracer<S extends TraceScene> {

    /**
     * Emits this tracer's geometry for {@code source} into {@code scene}'s
     * batch. Implementations read the extra information they need (level,
     * partial tick, projection, …) from {@code context} — see
     * {@link TraceContext}'s conventional keys.
     */
    void trace(S scene, TraceSource source, TraceContext context);
}
