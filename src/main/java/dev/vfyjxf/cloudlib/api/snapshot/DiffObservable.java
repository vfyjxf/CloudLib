package dev.vfyjxf.cloudlib.api.snapshot;

public interface DiffObservable<T> extends Observable {

    @Override
    boolean changed();

    T changes();
}
