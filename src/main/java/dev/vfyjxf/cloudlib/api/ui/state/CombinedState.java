package dev.vfyjxf.cloudlib.api.ui.state;

import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * A Linked List<State>
 */
public sealed interface CombinedState extends State
        permits ListCombinedState {

    static CombinedState of(State... states) {
        return new ListCombinedState(states);
    }


    /**
     * @return all contained states
     */
    ImmutableCollection<State> states();

    /**
     * @return any contained state changed
     */
    @Override
    boolean changed();
}
