package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.sync.SyncDataContainer;
import dev.vfyjxf.cloudlib.api.sync.SyncDataHolder;
import dev.vfyjxf.cloudlib.api.sync.SyncValue;
import dev.vfyjxf.cloudlib.api.sync.TypeSyncManager;

public class TestDataHolder implements SyncDataHolder {

    public TestDataHolder(int intData) {
        this.intData = intData;
        System.out.println();
    }

    private static final TypeSyncManager<TestDataHolder> sync = TypeSyncManager.define(TestDataHolder.class);
    private final SyncDataContainer syncContainer = SyncDataContainer.create(this);

    public static final SyncValue<TestDataHolder, Integer> manage = sync.server("intData");
    public static final SyncValue<TestDataHolder, Integer> manage2 = sync.server(h -> h.intData2, (h, v) -> h.intData2 = v);
    private int intData;
    private int intData2;

    public int getData() {
        return intData;
    }

    public void setData(int value) {
        this.intData = value;
        manage.setChange(this);
    }

    @Override
    public SyncDataContainer syncDataContainer() {
        return syncContainer;
    }
}
