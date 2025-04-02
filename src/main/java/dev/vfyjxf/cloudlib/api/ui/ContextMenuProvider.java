package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;

/**
 * C provider to build context menu
 */
public interface ContextMenuProvider {

    /**
     * @param builder  the builder to add menu items to
     * @param provider the provider of the context menu
     * @param mouseX   the x position of the mouse
     * @param mouseY   the y position of the mouse
     */
    void buildMenu(ContextMenuBuilder builder, Widget provider, double mouseX, double mouseY);

}
