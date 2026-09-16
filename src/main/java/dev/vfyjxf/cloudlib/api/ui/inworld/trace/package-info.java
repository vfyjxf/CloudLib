/**
 * Trace contracts — the annotation chrome that links a UI panel back to its
 * world source.
 * <p>
 * Traces are organized by <b>scene</b>
 * ({@link dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceScene}: world pass /
 * screen pass), never by source. A
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.trace.Tracer} receives only a
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceSource} plus a
 * {@link dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceContext} and decides
 * itself what to draw, emitting into the scene's {@code RenderBatch} so the
 * pass flushes in one go. {@link dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceStyle}
 * and {@link dev.vfyjxf.cloudlib.api.ui.inworld.trace.TraceKeys} expose the
 * theme machinery implementations style themselves with; {@code cursor} holds
 * an optional SPI for resolving geometric attachment points off a source.
 */
package dev.vfyjxf.cloudlib.api.ui.inworld.trace;
