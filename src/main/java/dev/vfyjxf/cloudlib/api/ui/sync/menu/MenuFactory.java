package dev.vfyjxf.cloudlib.api.ui.sync.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public interface MenuFactory<M extends BasicMenu<?>, A> {
    /**
     * @param menuType  the menu type for client to create the menu
     * @param id        the container id
     * @param playerInv the player inventory
     * @param accessor  the accessor for the menu
     * @return the menu
     */
    M create(MenuType<M> menuType, int id, Inventory playerInv, A accessor);

    default M createWithInit(MenuType<M> menuType, int id, Inventory playerInv, A accessor) {
        M menu = create(menuType, id, playerInv, accessor);
        menu.init();
        return menu;
    }
}
