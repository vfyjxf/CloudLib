package dev.vfyjxf.cloudlib.test.sync;

import dev.vfyjxf.cloudlib.api.ui.Widgets;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.sync.BasicMenuScreen;
import dev.vfyjxf.cloudlib.test.inworld.ChipWidget;
import dev.vfyjxf.cloudlib.test.inworld.ItemStripWidget;
import dev.vfyjxf.cloudlib.ui.inworld.HackerPanel;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.cloudlib.ui.widget.TextWidget;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.JustifyContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.alignItemsCenter;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.alignItemsFlexStart;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.columnGap;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.flexColumn;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.justifyCenter;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.justifyFlexStart;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.widthOf;

/**
 * The synced-block demo screen, rebuilt around the hacker-mode chrome:
 * a single dark panel listing the menu's live exposes, the 9-slot transform
 * inventory as an item strip, and a row of chips that push items through the
 * {@code selected} reversed channel.
 */
public class TestBlockEntityScreen extends BasicMenuScreen<TestBlockEntity.Menu> {

    public TestBlockEntityScreen(TestBlockEntity.Menu menu, Inventory playerInventory) {
        super(menu, playerInventory);

        mainGroup.useStyle(UIStyle.of(flexColumn(), justifyCenter(), alignItemsCenter()));

        HackerPanel panel = new HackerPanel(Component.literal("TEST.BE//live-sync"));
        panel.useStyle(widthOf(190));
        mainGroup.addWidget(panel);

        //region live scalars
        var basic = TextWidget.of("basic --").setColor(InworldTheme.TEXT);
        menu.basic.whenReceive(v -> basic.setText("basic " + v));
        panel.addChild(basic);

        var reference = TextWidget.of("ref --").setColor(InworldTheme.TEXT_DIM);
        menu.reference.whenReceive(v -> reference.setText("ref " + v));
        panel.addChild(reference);
        //endregion

        //region register entry (single live item)
        panel.addChild(TextWidget.of("entry").setColor(InworldTheme.TEXT_DIM));
        var entry = new ItemStripWidget();
        panel.addChild(entry);
        menu.registerEntry.whenReceive(stack -> entry.setItems(List.of(stack)));
        //endregion

        //region transform inventory (9 slots, live layer expose)
        panel.addChild(TextWidget.of("transform").setColor(InworldTheme.TEXT_DIM));
        var transform = new ItemStripWidget();
        panel.addChild(transform);
        menu.layerExpose.whenReceive(transform::setItems);
        //endregion

        //region selected — client → server reversed channel
        panel.addChild(TextWidget.of("select → server").setColor(InworldTheme.TEXT_DIM));
        WidgetGroup<Widget> chips = Widgets.row(JustifyContent.FLEX_START, AlignItems.FLEX_START);
        chips.useStyle(columnGap(2));
        List<ItemStack> options = List.of(
                Items.APPLE.getDefaultInstance(),
                Items.DIAMOND.getDefaultInstance(),
                Items.EMERALD.getDefaultInstance(),
                Items.ENDER_PEARL.getDefaultInstance(),
                Items.NETHERITE_SCRAP.getDefaultInstance()
        );
        ThreadLocalRandom random = ThreadLocalRandom.current();
        var lastSent = TextWidget.of("--").setColor(InworldTheme.ACCENT);
        for (int i = 0; i < options.size(); i++) {
            ItemStack stack = options.get(i);
            chips.addWidget(ChipWidget.of(String.valueOf(i + 1), () -> {
                ItemStack sent = stack.copyWithCount(random.nextInt(1, 64));
                menu.selected.sendToServer(sent);
                lastSent.setText("sent " + sent.getCount() + "x " + sent.getHoverName().getString());
            }));
        }
        panel.addChild(chips);
        panel.addChild(lastSent);
        //endregion

        //region difference logging (kept — this is what the expose demonstrates)
        menu.diffable.whenReceive(x -> System.out.println("all data received" + x));
        menu.diffable.whenDiffReceive(x -> System.out.println("diff data received" + x));
        //endregion
    }
}
