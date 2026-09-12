package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Slot-to-slot transfer between any two item sources — the commit half of
 * panel-to-panel drops and quick-slot moves. A {@code null} side means "the
 * player's own inventory" (vanilla combined index: 0-35 main+hotbar,
 * 36-39 armor, 40 offhand); a non-null side is the {@code IItemHandler}
 * block at that position.
 * <p>
 * The server is authoritative for everything: both ends re-resolve their
 * capability, reach is re-checked, the source's own {@code extractItem}
 * (simulated first) decides what may leave and the destination's
 * {@code insertItem} what may enter. {@code count} -1 = the whole slot.
 * {@code destSlot} -1 = first fitting slot in the destination.
 */
public record TransferPayload(
        @Nullable BlockPos source, int sourceSlot, @Nullable BlockPos dest, int destSlot, int count)
        implements ServerboundPayload {

    private static final double reach = 12.0;

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferPayload> streamCodec =
            StreamCodec.ofMember(TransferPayload::encode, TransferPayload::decode);

    public static final Type<TransferPayload> type =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "transfer"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(source != null);
        if (source != null) buf.writeBlockPos(source);
        buf.writeVarInt(sourceSlot);
        buf.writeBoolean(dest != null);
        if (dest != null) buf.writeBlockPos(dest);
        buf.writeVarInt(destSlot);
        buf.writeVarInt(count);
    }

    private static TransferPayload decode(RegistryFriendlyByteBuf buf) {
        BlockPos source = buf.readBoolean() ? buf.readBlockPos() : null;
        int sourceSlot = buf.readVarInt();
        BlockPos dest = buf.readBoolean() ? buf.readBlockPos() : null;
        int destSlot = buf.readVarInt();
        return new TransferPayload(source, sourceSlot, dest, destSlot, buf.readVarInt());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        IItemHandler src = side(source, sourceSlot, level, eye, player);
        IItemHandler dst = side(dest, destSlot, level, eye, player);
        if (src == null || dst == null) return;
        if (sourceSlot < 0 || sourceSlot >= src.getSlots()) return;
        if (source != null && source.equals(dest) && sourceSlot == destSlot) return;

        ItemStack stack = src.getStackInSlot(sourceSlot);
        if (stack.isEmpty()) return;
        int want = count < 0 ? stack.getCount() : Math.min(count, stack.getCount());
        // equipment slots only take equippables — the slot index is client
        // input, so the legality check lives here
        if (dest == null && destSlot >= 36 && !fitsEquipment(destSlot, stack)) return;

        // the source's own rules decide what may leave — simulate first
        ItemStack pulled = src.extractItem(sourceSlot, want, true);
        if (pulled.isEmpty()) return;

        // the destination decides what may enter — simulate, then commit both
        ItemStack remainder = destSlot < 0 ? insertAnywhere(dst, pulled, dest) : dst.insertItem(destSlot, pulled, true);
        int moved = pulled.getCount() - remainder.getCount();
        if (moved <= 0) return;
        ItemStack real = src.extractItem(sourceSlot, moved, false);
        ItemStack left = destSlot < 0 ? insertAnywhere(dst, real, dest) : dst.insertItem(destSlot, real, false);
        // the sim promised more than reality accepted — push the difference back
        if (!left.isEmpty()) {
            ItemStack back = src.insertItem(sourceSlot, left, false);
            if (!back.isEmpty() && source == null) {
                player.getInventory().placeItemBackInInventory(back);
            }
        }
    }

    /** Player-inventory auto-insert stays in main+hotbar (0-35) — never fills equipment slots. */
    private static ItemStack insertAnywhere(IItemHandler dst, ItemStack stack, @Nullable BlockPos dest) {
        int limit = dest == null ? Math.min(dst.getSlots(), 36) : dst.getSlots();
        ItemStack remaining = stack;
        for (int s = 0; s < limit && !remaining.isEmpty(); s++) {
            remaining = dst.insertItem(s, remaining, true);
        }
        return remaining;
    }

    /** 36-39 = armor (feet→head), 40 = offhand — the stack must declare that slot. */
    private static boolean fitsEquipment(int slot, ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        if (equipable == null) return false;
        return switch (slot) {
            case 36 -> equipable.getEquipmentSlot() == EquipmentSlot.FEET;
            case 37 -> equipable.getEquipmentSlot() == EquipmentSlot.LEGS;
            case 38 -> equipable.getEquipmentSlot() == EquipmentSlot.CHEST;
            case 39 -> equipable.getEquipmentSlot() == EquipmentSlot.HEAD;
            case 40 -> true; // offhand takes anything vanilla does
            default -> true;
        };
    }

    private @Nullable IItemHandler side(@Nullable BlockPos pos, int slot, Level level, Vec3 eye, ServerPlayer player) {
        if (pos == null) {
            // player inventory as an IItemHandler — slot bounds validated by caller
            return slot >= 0 && slot < player.getInventory().getContainerSize()
                    ? new InvWrapper(player.getInventory())
                    : null;
        }
        if (!pos.closerToCenterThan(eye, reach)) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }
}
