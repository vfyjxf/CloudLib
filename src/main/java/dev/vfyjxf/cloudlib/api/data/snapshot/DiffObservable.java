package dev.vfyjxf.cloudlib.api.data.snapshot;

public interface DiffObservable<D> extends Observable {

    /**
     * @return whether this observable has changed,it will be reset after {@link #difference()} is called
     */
    @Override
    boolean changed();

    /**
     * Get the difference of this observable
     *
     * @return return the difference since the last time it was checked
     */
    D difference();
}
