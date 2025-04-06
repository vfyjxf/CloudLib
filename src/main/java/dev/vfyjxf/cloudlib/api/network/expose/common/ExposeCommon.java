package dev.vfyjxf.cloudlib.api.network.expose.common;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ExposeCommon {

    /**
     * @return the debug name of this expose
     */
    String name();

    /**
     * The id of this value,it represents the type of this value in the data stream.
     *
     * @return the id of this expose
     */
    short id();

    /**
     * @param <T> the type of the snapshot
     * @return whether the snapshot has changed
     */
    <T> boolean changed();

}
