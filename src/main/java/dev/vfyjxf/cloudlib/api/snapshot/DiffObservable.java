package dev.vfyjxf.cloudlib.api.snapshot;

public interface DiffObservable<D> extends Observable {

    @Override
    boolean changed();

    D difference();
}
