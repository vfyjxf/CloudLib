package dev.vfyjxf.cloudlib.api.snapshot;

public interface SnapshotFactory {
    <T> Snapshot<T> create(T value);
}
