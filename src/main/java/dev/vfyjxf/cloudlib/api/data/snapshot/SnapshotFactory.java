package dev.vfyjxf.cloudlib.api.data.snapshot;

public interface SnapshotFactory {
    <T> Snapshot<T> create(T value);
}
