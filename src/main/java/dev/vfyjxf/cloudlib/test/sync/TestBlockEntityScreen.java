package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.ui.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.sync.BasicMenuScreen;
import dev.vfyjxf.cloudlib.ui.widgets.TextWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.awt.Color;

public class TestBlockEntityScreen extends BasicMenuScreen<TestBlockEntity.Menu> {

    public TestBlockEntityScreen(TestBlockEntity.Menu menu, Inventory playerInventory) {
        super(menu, playerInventory);
        mainGroup.withModifier(
                Modifier()
                        .background(Color.GRAY.getRGB())
        );

//        //region basic display
//        var basicDisplay = mainGroup.addWidget(TextWidget.of("nothing"));
//        menu.basic.whenReceive(integer -> {
//            basicDisplay.setText(Component.literal("Received from server:" + integer));
//        });
//        basicDisplay.withModifier(
//                Modifier.builder()
//                        .size(32)
//                        .background(Color.darkGray.getRGB())
//                        .posRel(0.2, 0.2)
//        );
//        //endregion

//        //region item display
//        Function<ItemStack, Widget> itemDisplayFactory = (ItemStack stack) -> new Widget() {
//            private final ItemStack displayItem = ItemStack.EMPTY;
//
//            {
//                onEvent(WidgetEvent.onRender, ((graphics, mouseX, mouseY, partialTicks, context) -> {
//                    if (displayItem.isEmpty()) return;
//                    graphics.renderItem(displayItem, 0, 0);
//                }));
//            }
//        };
//        var itemDisplayList = mainGroup().addWidget(new WidgetGroup<>());
//        itemDisplayList.withModifier(
//                Modifier.builder()
//                        .posRel(0.5, 0.5)
//                        .layoutWith(GridResizer::new, l -> l.fixed(3))
//        );
//        menu.layerExpose.whenReceive(stacks -> {
//            itemDisplayList.clear();
//            for (ItemStack stack : stacks) {
//                var itemDisplay = itemDisplayList.addWidget(itemDisplayFactory.apply(stack));
//                itemDisplay.withModifier(
//                        Modifier.builder()
//                                .size(16, 16)
//                );
//            }
//        });
//        //endregion


//        //region label display
//        var labelDisplay = mainGroup().addWidget(TextWidget.of("Nothing"));
//        menu.reference.whenReceive(string -> {
//            labelDisplay.setText(Component.literal("Received from server:" + string));
//        });
//        labelDisplay.withModifier(
//                Modifier.builder()
//                        .posRel(0.5, 0.2)
//                        .size(64, 32)
//        );
//        //endregion

        //region selected
        var selectedDisplay = mainGroup().addWidget(TextWidget.of("Nothing"));

        //endregion
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }
}
