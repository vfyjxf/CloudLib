package dev.vfyjxf.cloudlib.api.network.expose.diff;

import dev.vfyjxf.cloudlib.api.network.expose.reverse.Reversed;
import dev.vfyjxf.cloudlib.api.network.expose.common.LayerExpose;

public interface DiffReverseLayerExpose<E, D, S, R> extends LayerExpose<E>, Differential<D>, Reversed<S, R> {
}
