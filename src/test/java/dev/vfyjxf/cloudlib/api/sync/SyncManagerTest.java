package dev.vfyjxf.cloudlib.api.sync;

import dev.vfyjxf.cloudlib.api.data.ObservableValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(EphemeralTestServerProvider.class)
class SyncManagerTest {
    static class DataHolder implements SyncDataHolder {
        static final TypeSyncManager<DataHolder> TYPE_SYNC_MANAGER = TypeSyncManager.define(DataHolder.class);
        static final SyncValue<DataHolder, Integer> manage = TYPE_SYNC_MANAGER.server(
                holder -> holder.intData,
                (dataHolder, integer) -> dataHolder.intData = integer
        );
        int intData;

        @Override
        public SyncDataContainer syncDataContainer() {
            return null;
        }
    }

    @Test
    public void testCatch(MinecraftServer minecraftServer) {
        var type = DataHolder.TYPE_SYNC_MANAGER;
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TypeSyncManager.define(DataHolder.class).server(
                    holder -> holder.intData,
                    (dataHolder, integer) -> dataHolder.intData = integer
            );
        });
        Object dataReceiver = new Object();
        IItemHandler testObjValue = null;
        List<? extends Iterable<? extends Item>> list = null;
        class MyData {
            IItemHandler testObjValue;
            List<? extends Iterable<? extends Item>> list;
        }
        var data = new MyData();
        SyncManager.manage(
                dataReceiver,
                ObservableValue.of(data),
                (receiver, value) -> {

                }
        );
        DataHolder dataHolder = new DataHolder();
        SyncManager.onReceive(
                dataHolder,
                DataHolder.manage,
                integer -> {
                    System.out.println("Received data: " + integer);
                }
        );
    }

}