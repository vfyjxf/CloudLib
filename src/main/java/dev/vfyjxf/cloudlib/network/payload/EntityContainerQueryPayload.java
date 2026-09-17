package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.network.CloudlibPayloads;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client→server request for an entity's live inventory — the entity twin of
 * {@link ContainerQueryPayload}. Entity inventories (donkey/mule/llama chests,
 * modded {@link Capabilities.ItemHandler#ENTITY} carriers) are server-only,
 * so widgets ask the server for a snapshot and render the cached reply from
 * {@link EntityContainerContentsPayload}. Reads stay server-authoritative.
 * <p>
 * Resolution order: the {@code ItemHandler.ENTITY} capability first, then the
 * horse-family {@link AbstractChestedHorse#getInventory()} container when a
 * chest is carried. The query only passes for a live entity the player can
 * see — within {@link #reach} blocks.
 */
public record EntityContainerQueryPayload(int entityId) implements ServerboundPayload {

    /** Reach bound for a query. */
    static final double reach = 16.0;
    /** Never ship more slots than this — a malformed entity shouldn't spam the wire. */
    static final int maxSlots = 512;

    public static final ServerPayloadInfo<EntityContainerQueryPayload> info = CloudlibPayloads.createServerInfo(
            StreamCodec.ofMember(EntityContainerQueryPayload::encode, EntityContainerQueryPayload::decode),
            "entity_container_query");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    static EntityContainerQueryPayload decode(RegistryFriendlyByteBuf buf) {
        return new EntityContainerQueryPayload(buf.readVarInt());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        Entity entity = player.level().getEntity(entityId);
        if (entity == null || !entity.isAlive()) return;
        if (!inReach(entity.distanceToSqr(player))) return;
        List<ItemStack> stacks = snapshotOf(entity, maxSlots);
        if (stacks == null) return;
        context.reply(new EntityContainerContentsPayload(entityId, stacks));
    }

    static boolean inReach(double distanceSq) {
        return distanceSq <= reach * reach;
    }

    /**
     * The entity's readable inventory, or null when it has none: the
     * {@code ItemHandler.ENTITY} capability first, then a chested horse's
     * container.
     */
    static @Nullable List<ItemStack> snapshotOf(Entity entity, int maxSlots) {
        IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler != null) return collect(handler, maxSlots);
        if (entity instanceof AbstractChestedHorse horse && horse.hasChest()) {
            return collect(horse.getInventory(), maxSlots);
        }
        return null;
    }

    /** Copies at most {@code maxSlots} slots out of an item handler. */
    static List<ItemStack> collect(IItemHandler handler, int maxSlots) {
        int slots = Math.min(handler.getSlots(), maxSlots);
        List<ItemStack> stacks = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            stacks.add(handler.getStackInSlot(i).copy());
        }
        return stacks;
    }

    /** Copies at most {@code maxSlots} slots out of a vanilla container. */
    static List<ItemStack> collect(Container container, int maxSlots) {
        int slots = Math.min(container.getContainerSize(), maxSlots);
        List<ItemStack> stacks = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            stacks.add(container.getItem(i).copy());
        }
        return stacks;
    }
}
