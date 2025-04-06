package dev.vfyjxf.cloudlib.api.network.expose.common;

import dev.vfyjxf.cloudlib.api.network.expose.BasicExpose;
import dev.vfyjxf.cloudlib.api.network.expose.ValueSupplier;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
class CommonExpose<T> extends BasicExpose<T> {

    protected CommonExpose(String name, short id, Snapshot<T> snapshot, ValueSupplier<T> valueSupplier) {
        super(name, id, snapshot, valueSupplier);
    }
}
