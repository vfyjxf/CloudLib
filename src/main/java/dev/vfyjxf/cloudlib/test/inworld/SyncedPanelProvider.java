package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.ui.Widgets;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldSink;
import dev.vfyjxf.cloudlib.test.sync.SyncedTestBlockEntity;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.TextWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.JustifyContent;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.alignItemsFlexStart;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.columnGap;

/**
 * Demo provider: offers an in-world panel for every
 * {@link SyncedTestBlockEntity} near the player, plus a small follow-tag on the
 * player so the {@code follow} presentation mode is always visible.
 * <p>
 * For each synced block it offers two panels:
 * <ul>
 *   <li>a face panel flattened onto the block's top face — live count, label,
 *       item strip and action chips routed through the reversed expose channel;</li>
 *   <li>a floating status tag projected to screen space with a leader line.</li>
 * </ul>
 */
public final class SyncedPanelProvider implements InworldProvider {

    private static final int RANGE = 10;

    @Override
    public void provide(InworldContext context, InworldSink sink) {
        var level = context.level();
        BlockPos center = context.player().blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-RANGE, -5, -RANGE),
                center.offset(RANGE, 5, RANGE))) {
            if (!(level.getBlockEntity(pos) instanceof SyncedTestBlockEntity)) continue;
            BlockPos p = pos.immutable();

            sink.offer(InworldPanelSpec
                    .of("synced/face/" + p,
                            InworldAnchor.of(p),
                            InworldPlacement.face(Direction.UP, 0.5, 0.5, 96),
                            SyncedPanelProvider::faceContent)
                    .title(Component.literal("SYNCED://" + shortPos(p)))
                    .hints("LMB:press", "R:inspect")
                    .leaderLine(false));

            sink.offer(InworldPanelSpec
                    .of("synced/tag/" + p,
                            InworldAnchor.of(p, new Vec3(0.5, 1.15, 0.5)),
                            InworldPlacement.floating(),
                            SyncedPanelProvider::tagContent)
                    .title(Component.literal("LINK")));
        }

        //follow-mode demo: a small tag pinned above the player
        sink.offer(InworldPanelSpec
                .of("player/tag",
                        InworldAnchor.of(() -> context.player().position().add(0, 2.35, 0)),
                        InworldPlacement.follow(0, 0),
                        c -> playerTag())
                .maxDistance(64));
    }

    //region content

    private static Widget faceContent(InworldPanelContext ctx) {
        SyncedTestBlockEntity be = ctx.blockEntity(SyncedTestBlockEntity.class);
        ColumnWidget column = ColumnWidget.create(3);
        column.useStyle(alignItemsFlexStart());
        if (be == null) {
            column.addWidget(TextWidget.of("NO LINK").setColor(InworldTheme.TEXT_DIM));
            return column;
        }

        var count = TextWidget.of("count " + be.count().get()).setColor(InworldTheme.TEXT);
        be.count().onChange(v -> count.setText("count " + v));
        column.addWidget(count);

        var label = TextWidget.of(be.label().get()).setColor(InworldTheme.TEXT_DIM);
        be.label().onChange(v -> label.setText(v));
        column.addWidget(label);

        var strip = new ItemStripWidget();
        strip.setItems(be.items().get());
        be.items().onChange(strip::setItems);
        column.addWidget(strip);

        WidgetGroup<Widget> actions = Widgets.row(JustifyContent.FLEX_START, AlignItems.FLEX_START);
        actions.useStyle(columnGap(2));
        actions.addWidget(ChipWidget.of("+1", () -> be.sendAction(SyncedTestBlockEntity.ACTION_INCREMENT)));
        actions.addWidget(ChipWidget.of("-1", () -> be.sendAction(SyncedTestBlockEntity.ACTION_DECREMENT)));
        actions.addWidget(ChipWidget.of("item", () -> be.sendAction(SyncedTestBlockEntity.ACTION_PICK_ITEM)));
        actions.addWidget(ChipWidget.of("rst", () -> be.sendAction(SyncedTestBlockEntity.ACTION_RESET)));
        column.addWidget(actions);

        return column;
    }

    private static Widget tagContent(InworldPanelContext ctx) {
        SyncedTestBlockEntity be = ctx.blockEntity(SyncedTestBlockEntity.class);
        var text = TextWidget.of(be == null ? "--" : "◈ " + be.count().get()).setColor(InworldTheme.ACCENT);
        if (be != null) {
            be.count().onChange(v -> text.setText("◈ " + v));
        }
        return text;
    }

    private static Widget playerTag() {
        return TextWidget.of("◈ LOCAL").setColor(InworldTheme.TEXT_DIM);
    }

    //endregion

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
