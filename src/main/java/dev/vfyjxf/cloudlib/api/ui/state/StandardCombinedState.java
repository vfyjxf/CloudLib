package dev.vfyjxf.cloudlib.api.ui.state;

import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.factory.Lists;

import java.util.Collection;


final class ListCombinedState implements CombinedState {
    private final ImmutableCollection<State> states;

    ListCombinedState(State... states) {
        this.states = Lists.immutable.of(states);
    }

    ListCombinedState(Collection<State> states) {
        this.states = Lists.immutable.ofAll(states);
    }

    @Override
    public ImmutableCollection<State> states() {
        return states;
    }

    @Override
    public boolean changed() {
        return states.anySatisfy(State::changed);
    }
}
