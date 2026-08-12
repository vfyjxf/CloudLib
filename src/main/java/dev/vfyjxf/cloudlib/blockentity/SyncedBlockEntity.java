package dev.vfyjxf.cloudlib.blockentity;

/** A BlockEntity that owns a {@link BlockEntitySync} for live Expose sync. */
@FunctionalInterface
public interface SyncedBlockEntity {
    BlockEntitySync sync();
}
