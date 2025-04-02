package dev.vfyjxf.cloudlib.api.network.sync;

import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;

import java.util.function.Supplier;

public class DirectReferenceExposeValue<T> implements ExposeValue<T> {

    private final Supplier<T> supplier;

    public DirectReferenceExposeValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public short id() {
        return 0;
    }

    @Override
    public Snapshot<T> snapshot() {
        return null;
    }

    @Override
    public T current() {
        return supplier.get();
    }

}
