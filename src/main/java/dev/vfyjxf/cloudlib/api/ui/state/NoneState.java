package dev.vfyjxf.cloudlib.api.ui.state;

/**
 * A special {@link State} that indicates that no state is present.
 */
public enum NoneState implements State {

    INSTANCE;

    @Override
    public boolean changed() {
        return false;
    }
}
