package dev.vfyjxf.cloudlib.api.ui.state;

/**
 * A constant {@link State} that never changes its value.
 *
 * @param <T> the type of the state
 */
public record ConstantState<T>(T value) implements ReadableState<T> {
    @Override
    public boolean changed() {
        return false;
    }
}
