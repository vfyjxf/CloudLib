package dev.vfyjxf.cloudlib.api.data.snapshot;

public interface DiffObservable<D> extends Observable {

    @Override
    boolean changed();

    D difference();
}
