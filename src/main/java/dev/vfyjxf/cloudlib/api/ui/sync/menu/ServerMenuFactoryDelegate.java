package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface ServerMenuFactoryDelegate<M extends AbstractContainerMenu, A> {
    M create(int id, Inventory playerInv, A accessor);
}
