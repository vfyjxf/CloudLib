package dev.vfyjxf.cloudlib.api.ui.state;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;

public interface SnapshotState<T> extends MutableState<T> {

    Snapshot.MutableRef<T> snapshot();


}
