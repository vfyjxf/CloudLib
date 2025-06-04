package dev.vfyjxf.cloudlib.api.ui.state;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Basic interface for a ui state.
 */
public interface State {

    static <T> MutableState<T> mutableOf(T initialValue) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    static <T extends @Nullable Object> MutableState<T> mutable() {
        return mutableOf(null);
    }

    /**
     * 只会在tick end调用，调用过后应该还原为false.
     */
    @ApiStatus.Internal
    boolean changed();

}
