package dev.vfyjxf.cloudlib.network.payload;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entity container channel's wire format and snapshot logic: codec
 * round-trips for both directions plus the reach/clamp/copy rules the server
 * handler applies before answering.
 */
class EntityContainerPayloadsTest {

    @BeforeAll
    static void bootItems() {
        Bootstrap.bootStrap();
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(
            new FriendlyByteBuf(Unpooled.buffer()),
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
    }

    // region codecs

    @Test
    void queryRoundTripsAnEntityId() {
        RegistryFriendlyByteBuf buf = buffer();
        EntityContainerQueryPayload original = new EntityContainerQueryPayload(48151623);
        original.encode(buf);
        EntityContainerQueryPayload decoded = EntityContainerQueryPayload.decode(buf);
        assertEquals(original.entityId(), decoded.entityId());
    }

    @Test
    void contentsRoundTripStacksAndCounts() {
        RegistryFriendlyByteBuf buf = buffer();
        List<ItemStack> stacks = List
                .of(new ItemStack(Items.APPLE, 12), ItemStack.EMPTY, new ItemStack(Items.DIAMOND_SWORD));
        EntityContainerContentsPayload original = new EntityContainerContentsPayload(42, stacks);
        EntityContainerContentsPayload.info.streamCodec().encode(buf, original);
        EntityContainerContentsPayload decoded = EntityContainerContentsPayload.info.streamCodec().decode(buf);

        assertEquals(42, decoded.entityId());
        assertEquals(3, decoded.stacks().size());
        assertEquals(Items.APPLE, decoded.stacks().get(0).getItem());
        assertEquals(12, decoded.stacks().get(0).getCount());
        assertTrue(decoded.stacks().get(1).isEmpty());
        assertEquals(Items.DIAMOND_SWORD, decoded.stacks().get(2).getItem());
        assertEquals(1, decoded.stacks().get(2).getCount());
    }

    // endregion

    // region reach gate

    @Test
    void reachAcceptsSixteenBlocksAndNothingFurther() {
        assertTrue(EntityContainerQueryPayload.inReach(0));
        assertTrue(EntityContainerQueryPayload.inReach(16.0 * 16.0));
        assertFalse(EntityContainerQueryPayload.inReach(16.01 * 16.01));
    }

    // endregion

    // region snapshot logic

    @Test
    void collectingFromAHandlerCopiesSlotOrderAndClamps() {
        SimpleContainer container = new SimpleContainer(20);
        container.setItem(0, new ItemStack(Items.BREAD, 3));
        container.setItem(19, new ItemStack(Items.STONE, 64));
        IItemHandler handler = new InvWrapper(container);

        List<ItemStack> stacks = EntityContainerQueryPayload.collect(handler, 512);
        assertEquals(20, stacks.size());
        assertEquals(Items.BREAD, stacks.get(0).getItem());
        assertEquals(Items.STONE, stacks.get(19).getItem());

        // the max-slots clamp caps oversized handlers
        List<ItemStack> clamped = EntityContainerQueryPayload.collect(handler, 5);
        assertEquals(5, clamped.size());
        assertEquals(Items.BREAD, clamped.get(0).getItem());

        // copies, not live references — later container mutation must not leak in
        container.setItem(0, ItemStack.EMPTY);
        assertEquals(3, stacks.get(0).getCount());
    }

    @Test
    void collectingFromAContainerClampsToo() {
        SimpleContainer container = new SimpleContainer(8);
        container.setItem(7, new ItemStack(Items.APPLE, 2));
        List<ItemStack> stacks = EntityContainerQueryPayload.collect(container, 4);
        assertEquals(4, stacks.size());
        assertTrue(stacks.stream().allMatch(ItemStack::isEmpty));
    }

    // endregion
}
