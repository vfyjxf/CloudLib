package dev.vfyjxf.cloudlib.blockentity;

import dev.vfyjxf.cloudlib.api.data.serialize.Serialize;
import dev.vfyjxf.cloudlib.api.data.serialize.SerializerManagement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Composable delegate giving a BlockEntity declarative {@link Serialize} persistence. Register
 * entries on {@link #management()}, then delegate {@code saveData}/{@code loadData} here.
 */
public final class BlockEntitySerializer {

    private final SerializerManagement management = new SerializerManagement();

    public SerializerManagement management() {
        return management;
    }

    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        management.saveAll(tag, registries);
    }

    public void loadData(CompoundTag tag, HolderLookup.Provider registries) {
        management.loadAll(tag, registries);
    }
}
