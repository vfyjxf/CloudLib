package dev.vfyjxf.cloudlib.api.ui.state;

/**
 * Readable state definition.
 * <p>
 * <b>Only {@link NoneState} can't be read</b>
 *
 * @param <T>
 */
public non-sealed interface ReadableState<T> extends State {

    default T get() {
        return value();
    }

    T value();
}
