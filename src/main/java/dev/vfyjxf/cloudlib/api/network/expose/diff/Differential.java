package dev.vfyjxf.cloudlib.api.network.expose.diff;

import org.jetbrains.annotations.ApiStatus;

/**
 *
 * @param <D> the type of the difference
 */
@ApiStatus.Internal
public interface Differential<D> {

    void whenDiffReceive(D difference);

}
