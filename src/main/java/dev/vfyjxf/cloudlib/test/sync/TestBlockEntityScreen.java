package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.event.WidgetEvent;
import dev.vfyjxf.cloudlib.api.ui.layout.GridResizer;
import dev.vfyjxf.cloudlib.api.ui.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.sync.BasicMenuScreen;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.helper.RenderHelper;
import dev.vfyjxf.cloudlib.ui.widgets.TextWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class TestBlockEntityScreen extends BasicMenuScreen<TestBlockEntity.Menu> {

    public TestBlockEntityScreen(TestBlockEntity.Menu menu, Inventory playerInventory) {
        super(menu, playerInventory);

        mainGroup.withModifier(
                Modifier()
                        .background(0xff282c34)
        );

        //region item display
        var itemDisplayWidget = mainGroup().addWidget(new Widget() {
            private ItemStack displayItem = ItemStack.EMPTY;

            {
                onEvent(WidgetEvent.onRender, (graphics, mouseX, mouseY, partialTicks, context) -> {
                    if (displayItem.isEmpty()) return;
                    graphics.renderItem(displayItem, 0, 0);
                });
            }
        });
        menu.registerEntry.whenReceive(stack -> {
            itemDisplayWidget.displayItem = stack;
        });
        itemDisplayWidget.withModifier(
                Modifier.builder()
                        .size(16, 16)
                        .posRel(0.4, 0.5)
        );
        //endregion

        //region basic display
        var basicDisplay = mainGroup.addWidget(TextWidget.of("nothing"));
        menu.basic.whenReceive(integer -> {
            basicDisplay.setText(Component.literal("Received from server:" + integer));
        });
        basicDisplay.withModifier(
                Modifier.builder()
                        .size(32)
                        .background(Color.darkGray.getRGB())
                        .posRel(0.2, 0.2)
        );
        //endregion

        //region item display
        Function<ItemStack, Widget> itemDisplayFactory = (ItemStack stack) -> new Widget() {
            private final ItemStack displayItem = stack;

            {
                onEvent(WidgetEvent.onRender, ((graphics, mouseX, mouseY, partialTicks, context) -> {
                    if (displayItem.isEmpty()) return;
                    graphics.renderItem(displayItem, 0, 0);
                }));
            }
        };
        var itemDisplayList = mainGroup().addWidget(new WidgetGroup<>());
        itemDisplayList.mark("itemDisplayList");
        Modifier modifier = Modifier.builder()
                .size(16 * 3)
                .posRel(0.5, 0.5)
                .layoutWith(GridResizer::new, l -> l.fixed(3));
        menu.layerExpose.whenReceive(stacks -> {
            itemDisplayList.clear();
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                var itemDisplay = itemDisplayList.addWidget(itemDisplayFactory.apply(stack));
                itemDisplay.mark("itemDisplay" + i);
                itemDisplay.withModifier(
                        Modifier.builder()
                                .size(16, 16)
                );
            }
            itemDisplayList.withModifier(modifier);
            itemDisplayList.init();
            itemDisplayList.layout();
        });
        //endregion


        //region label display
        var labelDisplay = mainGroup().addWidget(TextWidget.of("Nothing"));
        menu.reference.whenReceive(string -> {
            labelDisplay.setText(Component.literal("Received from server:" + string));
        });
        labelDisplay.withModifier(
                Modifier.builder()
                        .posRel(0.5, 0.2)
                        .size(64, 32)
        );
        //endregion

        //region selected
        var selectedDisplay = mainGroup().addWidget(itemDisplayFactory.apply(ItemStack.EMPTY));
        selectedDisplay.mark("selectedDisplay");
        selectedDisplay.withModifier(
                Modifier().
                        size(16, 16)
                        .posRel(0.6, 0.3)
        );
        List<ItemStack> stacks = List.of(
                Items.APPLE.getDefaultInstance(),
                Items.BEDROCK.getDefaultInstance(),
                Items.DIAMOND.getDefaultInstance(),
                Items.DIAMOND_SWORD.getDefaultInstance(),
                Items.DIAMOND_PICKAXE.getDefaultInstance()
        );
        ThreadLocalRandom random = ThreadLocalRandom.current();
        selectedDisplay.onEvent(WidgetEvent.onRender, ((graphics, mouseX, mouseY, partialTicks, context) -> {
            RenderHelper.drawSolidRect(graphics, 0, 0, 16, 16, 0xFF000000);
        }));
        selectedDisplay.onEvent(InputEvent.onMouseClicked, (input, context) -> {
            int index = random.nextInt(stacks.size());
            ItemStack stack = stacks.get(index);
            menu.selected.sendToServer(stack.copyWithCount(random.nextInt(1, 64)));
            return true;
        });
        //endregion
    }
}
