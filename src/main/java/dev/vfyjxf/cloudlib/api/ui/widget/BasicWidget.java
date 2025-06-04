package dev.vfyjxf.cloudlib.api.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.state.NoneState;
import dev.vfyjxf.cloudlib.api.ui.state.State;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;

/**
 * Internal interface for basic widget lifecycle and state management.
 */
@ApiStatus.Internal
abstract class BasicWidget {

    protected final State state;
    private final BiFunction<State, BasicWidget, BasicWidget> factory;

    /**
     * Stateless widget constructor.
     */
    BasicWidget() {
        state = NoneState.INSTANCE;
        factory = (state, widget) -> widget;
    }

    /**
     * Stateful widget constructor.
     */
    @SuppressWarnings("unchecked")
    <S extends State, W extends BasicWidget>
    BasicWidget(S state, BiFunction<S, W, W> factory) {
        this.state = state;
        this.factory = (BiFunction<State, BasicWidget, BasicWidget>) factory;
    }

    boolean stateless() {
        return state == NoneState.INSTANCE;
    }

    boolean stateful() {
        return state != NoneState.INSTANCE;
    }

}
