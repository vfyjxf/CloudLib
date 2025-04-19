package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {

    S createScreen(M menu, Inventory inventory);

//    default S createScreen(M menu, Inventory inventory) {
//        return createScreen(menu, inventory);
//    }

}
