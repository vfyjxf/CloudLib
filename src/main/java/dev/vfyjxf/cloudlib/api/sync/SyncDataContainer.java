package dev.vfyjxf.cloudlib.api.sync;

public class SyncDataContainer {
    private final SyncDataHolder holder;

    public static SyncDataContainer create(SyncDataHolder holder) {
        return new SyncDataContainer(holder);
    }

    private SyncDataContainer(SyncDataHolder holder) {
        this.holder = holder;
    }
}
