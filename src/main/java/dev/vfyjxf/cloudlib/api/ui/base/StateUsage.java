package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.ui.state.MutableState;

public sealed interface StateUsage permits StateCollector {

    void use(MutableState<?> state);

    default void use(MutableState<?>... states) {
        for (MutableState<?> state : states) use(state);
    }

}
