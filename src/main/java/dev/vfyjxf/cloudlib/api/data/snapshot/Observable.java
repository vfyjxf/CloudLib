package dev.vfyjxf.cloudlib.api.data.snapshot;

/**
 * An observable object that can be observed for changes.
 */
public interface Observable {

    /**
     * @return whether this observable has changed since the last time it was checked.
     */
    boolean changed();
}
