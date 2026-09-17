package dev.vfyjxf.cloudlib.ui.sync;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EntityContainerContents' cache semantics — receive/revision, slot reads,
 * invalidation by liveness sweep and full clears. The watch() path itself
 * needs a live Minecraft client and is exercised in the dev runtime.
 */
class EntityContainerContentsTest {

    @BeforeAll
    static void bootItems() {
        Bootstrap.bootStrap();
    }

    @AfterEach
    void cleanUp() {
        EntityContainerContents.clear();
    }

    @Test
    void receiveStoresSlotsAndBumpsTheRevision() {
        assertEquals(0, EntityContainerContents.revision(7));
        assertEquals(0, EntityContainerContents.slotsOf(7, 0));

        EntityContainerContents.receive(7, List.of(new ItemStack(Items.APPLE, 3), ItemStack.EMPTY));
        assertEquals(1, EntityContainerContents.revision(7));
        assertEquals(2, EntityContainerContents.slotsOf(7, 5));
        assertEquals(Items.APPLE, EntityContainerContents.stackAt(7, 0).getItem());
        assertEquals(3, EntityContainerContents.stackAt(7, 0).getCount());
        assertTrue(EntityContainerContents.stackAt(7, 1).isEmpty());

        EntityContainerContents.receive(7, List.of(new ItemStack(Items.BREAD)));
        assertEquals(2, EntityContainerContents.revision(7), "every snapshot bumps the change signal");
        assertEquals(1, EntityContainerContents.slotsOf(7, 5));
    }

    @Test
    void unknownEntitiesReadEmpty() {
        assertEquals(9, EntityContainerContents.slotsOf(1234, 9));
        assertTrue(EntityContainerContents.stackAt(1234, 0).isEmpty());
        // out-of-range slots read empty even on a cached entity
        EntityContainerContents.receive(1234, List.of(new ItemStack(Items.APPLE)));
        assertTrue(EntityContainerContents.stackAt(1234, 1).isEmpty());
        assertTrue(EntityContainerContents.stackAt(1234, -1).isEmpty());
    }

    @Test
    void livenessSweepDropsGoneEntitiesAndKeepsLiveOnes() {
        EntityContainerContents.receive(1, List.of(new ItemStack(Items.APPLE)));
        EntityContainerContents.receive(2, List.of(new ItemStack(Items.BREAD)));
        EntityContainerContents.receive(3, List.of(new ItemStack(Items.STONE)));

        EntityContainerContents.retainIf(id -> id != 2);

        assertEquals(1, EntityContainerContents.slotsOf(1, 0));
        assertEquals(0, EntityContainerContents.slotsOf(2, 0), "the gone entity's snapshot is invalidated");
        assertEquals(1, EntityContainerContents.slotsOf(3, 0));
        assertEquals(0, EntityContainerContents.revision(2), "the revision resets with the cache");
    }

    @Test
    void forgetDropsOneEntity() {
        EntityContainerContents.receive(5, List.of(new ItemStack(Items.APPLE)));
        EntityContainerContents.forget(5);
        assertEquals(0, EntityContainerContents.slotsOf(5, 0));
        assertEquals(0, EntityContainerContents.revision(5));
    }

    @Test
    void clearDropsEverything() {
        EntityContainerContents.receive(1, List.of(new ItemStack(Items.APPLE)));
        EntityContainerContents.receive(2, List.of(new ItemStack(Items.BREAD)));
        EntityContainerContents.clear();
        assertEquals(0, EntityContainerContents.slotsOf(1, 0));
        assertEquals(0, EntityContainerContents.slotsOf(2, 0));
    }
}
