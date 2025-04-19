package dev.vfyjxf.cloudlib.api.network.expose;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

@ApiStatus.Internal
public sealed interface ExposeCommon permits Expose, LayerExpose {

    /**
     * @return the debug name of this expose
     */
    @ApiStatus.Internal
    String name();

    /**
     * The id of this value,it represents the type of this value in the data stream.
     *
     * @return the id of this expose
     */
    @ApiStatus.Internal
    short id();

    /**
     * @param <T> the type of the snapshot
     * @return whether the snapshot has changed
     */
    @Contract(pure = true)
    @ApiStatus.Internal
    <T> boolean changed();

    void updateSnapshot();

    void forceUpdateSnapshot();

}
