package dev.vfyjxf.cloudlib.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicBlockEntity extends BlockEntity {

    private static final Logger log = LoggerFactory.getLogger(BasicBlockEntity.class);

    protected static final String UPDATE_TAG = "UpdateTag";

    public BasicBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public abstract AbstractContainerMenu createMenu();

    @Override
    @SuppressWarnings("ConstantConditions")
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag updateTag = new CompoundTag();
        CompoundTag compoundTag = new CompoundTag();
        writeUpdateData(compoundTag, registries);
        updateTag.put(UPDATE_TAG, compoundTag);
        return updateTag;
    }

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(tag, registries);
    }

    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        //region Read Update Tag
        if (tag.contains(UPDATE_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag updateTag = tag.getCompound(UPDATE_TAG);
            if (readUpdateData(updateTag, registries)) {
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
            }
        }
        //endregion
        super.loadAdditional(tag, registries);
        loadData(tag, registries);
    }

    // Optionally: Run some custom logic when the packet is received.
    // The super/default implementation forwards to #loadAdditional.
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (!tag.isEmpty()) {
            readUpdateData(tag, registries);
        }
        super.onDataPacket(connection, packet, registries);
        // Do whatever you need to do here.
    }

    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {

    }

    public void loadData(CompoundTag tag, HolderLookup.Provider registries) {

    }

    /**
     * Call on {@link BlockEntity#getUpdateTag(HolderLookup.Provider)}
     *
     * @param data The data to write to
     */
    protected void writeUpdateData(CompoundTag data, HolderLookup.Provider registries) {

    }

    /**
     * @param data The data to read from
     * @return True if the block entity should be updated
     */
    protected boolean readUpdateData(CompoundTag data, HolderLookup.Provider registries) {
        return false;
    }

}
