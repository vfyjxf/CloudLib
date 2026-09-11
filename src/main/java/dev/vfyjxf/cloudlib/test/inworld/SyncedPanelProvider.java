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
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.cloudlib.ui.widget.ChipWidget;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.ItemStripWidget;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import dev.vfyjxf.cloudlib.ui.widget.ProgressBarWidget;
import dev.vfyjxf.cloudlib.ui.widget.TextWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.JustifyContent;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.alignItemsFlexStart;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.columnGap;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;

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
                    .hints("V:+1", "LMB:press", "R:inspect")
                    .action(ctx -> {
                        SyncedTestBlockEntity be = ctx.blockEntity(SyncedTestBlockEntity.class);
                        if (be != null) be.sendAction(SyncedTestBlockEntity.ACTION_INCREMENT);
                    })
                    .leaderLine(false));

            //side console: the placement is re-offered each pass, tracking the
            //player's horizontal direction so the panel always faces them
            sink.offer(InworldPanelSpec
                    .of("synced/side/" + p,
                            InworldAnchor.of(p),
                            InworldPlacement.face(sideToward(context.player(), p), 0.5, 0.5, 64),
                            SyncedPanelProvider::sideContent)
                    .title(Component.literal("IO"))
                    .hints("LMB:press"));

            sink.offer(InworldPanelSpec
                    .of("synced/tag/" + p,
                            InworldAnchor.of(p, new Vec3(0.5, 1.15, 0.5)),
                            InworldPlacement.dock(),
                            SyncedPanelProvider::tagContent)
                    .title(Component.literal("LINK")));
        }

        //follow-mode demo: an entity-tag pinned above the player
        sink.offer(InworldPanelSpec
                .of("player/tag",
                        InworldAnchor.of(() -> context.player().position().add(0, 2.35, 0)),
                        InworldPlacement.follow(0, 0),
                        c -> new EntityTagWidget(context.player(), "LOCAL"))
                .maxDistance(64));
    }

    //region content

    private static Widget faceContent(InworldPanelContext ctx) {
        SyncedTestBlockEntity be = ctx.blockEntity(SyncedTestBlockEntity.class);
        ColumnWidget column = ColumnWidget.create(3);
        column.useStyle(alignItemsFlexStart());
        if (be == null) {
            column.addWidget(TextWidget.of("NO LINK").setColor(HackerTheme.TEXT_DIM));
            return column;
        }

        //row 1: count + 16-cycle progress meter
        WidgetGroup<Widget> countRow = Widgets.row(JustifyContent.FLEX_START, AlignItems.CENTER);
        countRow.useStyle(columnGap(3));
        var count = TextWidget.of("count " + be.count().get()).setColor(HackerTheme.TEXT);
        be.count().onChange(v -> count.setText("count " + v));
        countRow.addWidget(count);
        var cycle = ProgressBarWidget.create(() -> (be.count().get() & 15) / 16.0);
        cycle.setColors(0xFF081018, HackerTheme.ACCENT);
        cycle.useStyle(sizeOf(40, 5));
        countRow.addWidget(cycle);
        column.addWidget(countRow);

        var label = TextWidget.of(be.label().get()).setColor(HackerTheme.TEXT_DIM);
        be.label().onChange(v -> label.setText(v));
        column.addWidget(label);

        var divider = DividerWidget.horizontal();
        divider.setColor(HackerTheme.TITLE_RULE);
        divider.useStyle(sizeOf(96, 3));
        column.addWidget(divider);

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

    private static Widget sideContent(InworldPanelContext ctx) {
        SyncedTestBlockEntity be = ctx.blockEntity(SyncedTestBlockEntity.class);
        ColumnWidget column = ColumnWidget.create(2);
        if (be == null) {
            column.addWidget(TextWidget.of("NO LINK").setColor(HackerTheme.TEXT_DIM));
            return column;
        }
        var count = TextWidget.of("cnt " + be.count().get()).setColor(HackerTheme.ACCENT);
        be.count().onChange(v -> count.setText("cnt " + v));
        column.addWidget(count);
        WidgetGroup<Widget> actions = Widgets.row(JustifyContent.FLEX_START, AlignItems.FLEX_START);
        actions.useStyle(columnGap(2));
        actions.addWidget(ChipWidget.of("+1", () -> be.sendAction(SyncedTestBlockEntity.ACTION_INCREMENT)));
        actions.addWidget(ChipWidget.of("rst", () -> be.sendAction(SyncedTestBlockEntity.ACTION_RESET)));
        column.addWidget(actions);
        return column;
    }

    /** The block face oriented toward the player's position (fallback: where they look). */
    private static Direction sideToward(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        Vec3 delta = player.position().subtract(Vec3.atCenterOf(pos));
        if (delta.x * delta.x + delta.z * delta.z < 0.05) {
            return Direction.fromYRot(player.getYRot()).getOpposite();
        }
        return Direction.getNearest(delta.x, 0, delta.z);
    }

    private static Widget tagContent(InworldPanelContext ctx) {
        SyncedTestBlockEntity be = ctx.blockEntity(SyncedTestBlockEntity.class);
        ColumnWidget column = ColumnWidget.create(2);
        column.useStyle(alignItemsFlexStart());
        if (be == null) {
            column.addWidget(TextWidget.of("--").setColor(HackerTheme.TEXT_DIM));
            return column;
        }
        var text = TextWidget.of("◈ " + be.count().get()).setColor(HackerTheme.ACCENT);
        be.count().onChange(v -> text.setText("◈ " + v));
        column.addWidget(text);
        var label = TextWidget.of(be.label().get()).setColor(HackerTheme.TEXT_DIM);
        be.label().onChange(v -> label.setText(v));
        column.addWidget(label);
        return column;
    }

    //endregion

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
