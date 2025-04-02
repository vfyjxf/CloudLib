package dev.vfyjxf.cloudlib.test.sync;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TestBlockEntityScreen extends AbstractContainerScreen<TestBlockEntity.Menu> {

    public TestBlockEntityScreen(TestBlockEntity.Menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        menu.basic.onUpdate((old,next )->{
            System.out.println("old: "+old+" next: "+next);
        });
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }
}
